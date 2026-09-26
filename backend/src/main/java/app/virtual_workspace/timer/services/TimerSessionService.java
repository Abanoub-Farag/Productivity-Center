package app.virtual_workspace.timer.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.services.UserReferenceProvider;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.repositories.RoomMembersRepository;
import app.virtual_workspace.rooms.services.RoomMembersService;
import app.virtual_workspace.rooms.services.RoomReferenceProvider;
import app.virtual_workspace.timer.dtos.TimerSessionResponseDto;
import app.virtual_workspace.timer.mappers.TimerSessionMapper;
import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.models.enums.TimerStatus;
import app.virtual_workspace.timer.repositories.TimerSessionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TimerSessionService {

    private final TimerSessionRepository timerSessionRepository;
    private final RoomMembersRepository roomMembersRepository;
    private final RoomMembersService roomMembersService;
    private final RoomReferenceProvider roomReferenceProvider;
    private final UserReferenceProvider userReferenceProvider;
    private final TimerSessionMapper timerSessionMapper;

    // ── Start ──────────────────────────────────────────────────────────────────

    @Transactional
    public TimerSessionResponseDto startTimer(Long userId, Long roomId) {

        // Verify membership (throws 404 if not a member)
        roomMembersService.findMember(userId, roomId);

        // Guard: no concurrent RUNNING session (DB partial unique index is the
        // ultimate safeguard, but we fail fast with a clear error here)
        timerSessionRepository.findByUserIdAndStatus(userId, TimerStatus.RUNNING)
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("A timer session is already running");
                });

        Room room = roomReferenceProvider.getReference(roomId);

        if (room == null)
            throw new ResourceNotFoundException("Room not found");

        TimerSession session = TimerSession.builder()
                .user(userReferenceProvider.getReference(userId))
                .room(room)
                .startedAt(LocalDateTime.now())
                .status(TimerStatus.RUNNING)
                .build();

        TimerSession saved = timerSessionRepository.save(session);

        roomMembersRepository.clearTimerActive(userId, roomId); // reset first
        // timerActive is set to true via the next heartbeat carrying { timerActive:
        // true }
        // or we set it directly here for immediate consistency:
        roomMembersRepository.updateHeartbeat(userId, roomId, LocalDateTime.now(), true);

        return timerSessionMapper.toDto(saved);
    }

    // ── Complete ───────────────────────────────────────────────────────────────

    @Transactional
    public TimerSessionResponseDto completeTimer(Long sessionId, Long requestingUserId) {

        TimerSession session = timerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timer session not found"));

        if (!session.getUserId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Timer session not found");
        }

        if (session.getStatus() != TimerStatus.RUNNING) {
            throw new ResourceAlreadyExistsException("Timer session is already completed");
        }

        LocalDateTime endedAt = LocalDateTime.now();
        long duration = ChronoUnit.SECONDS.between(session.getStartedAt(), endedAt);

        session.setEndedAt(endedAt);
        session.setDuration(duration);
        session.setStatus(TimerStatus.DONE);

        TimerSession saved = timerSessionRepository.save(session);
        roomMembersRepository.clearTimerActive(session.getUserId(), session.getRoomId());

        return timerSessionMapper.toDto(saved);
    }

    // ── Get one ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimerSessionResponseDto getSession(Long sessionId, Long requestingUserId) {
        TimerSession session = timerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timer session not found"));

        if (!session.getUserId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Timer session not found");
        }

        return timerSessionMapper.toDto(session);
    }

    // ── Active timer for user in room ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimerSessionResponseDto getActiveTimer(Long userId, Long roomId) {
        return timerSessionRepository.findByUserIdAndRoomIdAndStatus(userId, roomId, TimerStatus.RUNNING)
                .map(timerSessionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No active timer session found"));
    }

    // ── Historical sessions ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TimerSessionResponseDto> getUserSessions(Long userId, LocalDateTime from, LocalDateTime to) {
        return timerSessionRepository
                .findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(userId, from, to)
                .stream()
                .map(timerSessionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimerSessionResponseDto> getUserSessionsInRoom(Long userId, Long roomId,
            LocalDateTime from, LocalDateTime to) {
        return timerSessionRepository
                .findByUserIdAndRoomIdAndStartedAtBetweenOrderByStartedAtDesc(userId, roomId, from, to)
                .stream()
                .map(timerSessionMapper::toDto)
                .toList();
    }

    // ── Abandon (called by scheduler) ──────────────────────────────────────────

    @Transactional
    public void finalizeAbandoned(TimerSession session) {
        // Use lastActiveAt of the RoomMembers record as endedAt
        roomMembersRepository.findByUserIdAndRoomId(session.getUserId(), session.getRoomId())
                .ifPresent(member -> {
                    LocalDateTime endedAt = member.getLastActiveAt();
                    long duration = Math.max(0,
                            ChronoUnit.SECONDS.between(session.getStartedAt(), endedAt));

                    session.setEndedAt(endedAt);
                    session.setDuration(duration);
                    session.setStatus(TimerStatus.DONE);
                    timerSessionRepository.save(session);

                    member.setTimerActive(false);
                    roomMembersRepository.save(member);
                });
    }

}
