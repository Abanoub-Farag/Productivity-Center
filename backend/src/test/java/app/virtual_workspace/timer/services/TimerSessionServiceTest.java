package app.virtual_workspace.timer.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserReferenceProvider;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.models.RoomMembers;
import app.virtual_workspace.rooms.repositories.RoomMembersRepository;
import app.virtual_workspace.rooms.services.RoomMembersService;
import app.virtual_workspace.rooms.services.RoomReferenceProvider;
import app.virtual_workspace.timer.dtos.TimerSessionResponseDto;
import app.virtual_workspace.timer.mappers.TimerSessionMapper;
import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.models.enums.TimerStatus;
import app.virtual_workspace.timer.repositories.TimerSessionRepository;

@ExtendWith(MockitoExtension.class)
public class TimerSessionServiceTest {

    @Mock private TimerSessionRepository timerSessionRepository;
    @Mock private RoomMembersRepository roomMembersRepository;
    @Mock private RoomMembersService roomMembersService;
    @Mock private RoomReferenceProvider roomReferenceProvider;
    @Mock private UserReferenceProvider userReferenceProvider;
    @Mock private TimerSessionMapper timerSessionMapper;

    @InjectMocks
    private TimerSessionService timerSessionService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final Long SESSION_ID = 100L;

    private User sampleUser;
    private Room sampleRoom;
    private TimerSession runningSession;
    private TimerSessionResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(USER_ID).build();
        sampleRoom = Room.builder().id(ROOM_ID).build();

        runningSession = TimerSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .roomId(ROOM_ID)
                .startedAt(Instant.now().minusSeconds(300))
                .status(TimerStatus.RUNNING)
                .build();

        sampleDto = TimerSessionResponseDto.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .roomId(ROOM_ID)
                .status(TimerStatus.RUNNING)
                .startedAt(runningSession.getStartedAt())
                .build();
    }

    // ── startTimer ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startTimer() tests")
    class StartTimerTests {

        @Test
        @DisplayName("Should start a new timer session and set timerActive on the room member")
        void startTimer_shouldCreateSessionAndSetTimerActive() {
            when(roomReferenceProvider.getReference(ROOM_ID)).thenReturn(sampleRoom);
            when(userReferenceProvider.getReference(USER_ID)).thenReturn(sampleUser);
            when(timerSessionRepository.findByUserIdAndStatus(USER_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.empty());
            when(timerSessionRepository.save(any(TimerSession.class))).thenReturn(runningSession);
            when(timerSessionMapper.toDto(runningSession)).thenReturn(sampleDto);

            TimerSessionResponseDto result = timerSessionService.startTimer(USER_ID, ROOM_ID);

            assertThat(result).isEqualTo(sampleDto);

            ArgumentCaptor<TimerSession> captor = ArgumentCaptor.forClass(TimerSession.class);
            verify(timerSessionRepository).save(captor.capture());

            TimerSession saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(TimerStatus.RUNNING);
            assertThat(saved.getStartedAt()).isNotNull();
            assertThat(saved.getUser()).isEqualTo(sampleUser);
            assertThat(saved.getRoom()).isEqualTo(sampleRoom);

            verify(roomMembersRepository).clearTimerActive(USER_ID, ROOM_ID);
            verify(roomMembersRepository).updateHeartbeat(eq(USER_ID), eq(ROOM_ID), any(Instant.class), eq(true));
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException when a RUNNING session already exists")
        void startTimer_shouldThrow_whenSessionAlreadyRunning() {
            when(timerSessionRepository.findByUserIdAndStatus(USER_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.of(runningSession));

            assertThatThrownBy(() -> timerSessionService.startTimer(USER_ID, ROOM_ID))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("A timer session is already running");

            verify(timerSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room reference is null")
        void startTimer_shouldThrow_whenRoomIsNull() {
            when(timerSessionRepository.findByUserIdAndStatus(USER_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.empty());
            when(roomReferenceProvider.getReference(ROOM_ID)).thenReturn(null);

            assertThatThrownBy(() -> timerSessionService.startTimer(USER_ID, ROOM_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room not found");

            verify(timerSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should verify room membership before creating a session")
        void startTimer_shouldVerifyMembership() {
            when(timerSessionRepository.findByUserIdAndStatus(USER_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.empty());
            when(roomReferenceProvider.getReference(ROOM_ID)).thenReturn(sampleRoom);
            when(userReferenceProvider.getReference(USER_ID)).thenReturn(sampleUser);
            when(timerSessionRepository.save(any())).thenReturn(runningSession);
            when(timerSessionMapper.toDto(runningSession)).thenReturn(sampleDto);

            timerSessionService.startTimer(USER_ID, ROOM_ID);

            verify(roomMembersService).findMember(USER_ID, ROOM_ID);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when user is not a room member")
        void startTimer_shouldPropagateException_whenNotMember() {
            when(roomMembersService.findMember(USER_ID, ROOM_ID))
                    .thenThrow(new ResourceNotFoundException("Room membership not found"));

            assertThatThrownBy(() -> timerSessionService.startTimer(USER_ID, ROOM_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room membership not found");

            verify(timerSessionRepository, never()).save(any());
        }
    }

    // ── completeTimer ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeTimer() tests")
    class CompleteTimerTests {

        @Test
        @DisplayName("Should complete a RUNNING session, set duration, and clear timerActive")
        void completeTimer_shouldFinalizeSession() {
            Instant startedAt = Instant.now().minusSeconds(120);
            runningSession.setStartedAt(startedAt);

            TimerSession doneSession = TimerSession.builder()
                    .id(SESSION_ID).userId(USER_ID).roomId(ROOM_ID)
                    .startedAt(startedAt).status(TimerStatus.DONE).duration(120L).build();

            TimerSessionResponseDto doneDto = TimerSessionResponseDto.builder()
                    .id(SESSION_ID).status(TimerStatus.DONE).duration(120L).build();

            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));
            when(timerSessionRepository.save(runningSession)).thenReturn(doneSession);
            when(timerSessionMapper.toDto(doneSession)).thenReturn(doneDto);

            TimerSessionResponseDto result = timerSessionService.completeTimer(SESSION_ID, USER_ID);

            assertThat(result.getStatus()).isEqualTo(TimerStatus.DONE);
            assertThat(result.getDuration()).isEqualTo(120L);

            assertThat(runningSession.getStatus()).isEqualTo(TimerStatus.DONE);
            assertThat(runningSession.getEndedAt()).isNotNull();
            assertThat(runningSession.getDuration()).isGreaterThanOrEqualTo(0L);

            verify(roomMembersRepository).clearTimerActive(USER_ID, ROOM_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session is not found")
        void completeTimer_shouldThrow_whenSessionNotFound() {
            when(timerSessionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timerSessionService.completeTimer(999L, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session belongs to a different user")
        void completeTimer_shouldThrow_whenSessionBelongsToDifferentUser() {
            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));

            assertThatThrownBy(() -> timerSessionService.completeTimer(SESSION_ID, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");

            verify(timerSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException when session is already DONE")
        void completeTimer_shouldThrow_whenSessionAlreadyDone() {
            runningSession.setStatus(TimerStatus.DONE);
            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));

            assertThatThrownBy(() -> timerSessionService.completeTimer(SESSION_ID, USER_ID))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("Timer session is already completed");

            verify(timerSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should compute duration as non-negative even for same-second start/end")
        void completeTimer_shouldProduceNonNegativeDuration() {
            runningSession.setStartedAt(Instant.now()); // started just now
            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));
            when(timerSessionRepository.save(runningSession)).thenReturn(runningSession);
            when(timerSessionMapper.toDto(runningSession)).thenReturn(sampleDto);

            timerSessionService.completeTimer(SESSION_ID, USER_ID);

            assertThat(runningSession.getDuration()).isGreaterThanOrEqualTo(0L);
        }
    }

    // ── getSession ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSession() tests")
    class GetSessionTests {

        @Test
        @DisplayName("Should return DTO when session exists and belongs to the requesting user")
        void getSession_shouldReturnDto_whenSessionFound() {
            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));
            when(timerSessionMapper.toDto(runningSession)).thenReturn(sampleDto);

            TimerSessionResponseDto result = timerSessionService.getSession(SESSION_ID, USER_ID);

            assertThat(result).isEqualTo(sampleDto);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session is not found")
        void getSession_shouldThrow_whenNotFound() {
            when(timerSessionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timerSessionService.getSession(999L, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session belongs to a different user")
        void getSession_shouldThrow_whenSessionBelongsToDifferentUser() {
            when(timerSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(runningSession));

            assertThatThrownBy(() -> timerSessionService.getSession(SESSION_ID, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");
        }
    }

    // ── getActiveTimer ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveTimer() tests")
    class GetActiveTimerTests {

        @Test
        @DisplayName("Should return DTO for the active RUNNING session in the given room")
        void getActiveTimer_shouldReturnDto_whenRunningSessionExists() {
            when(timerSessionRepository.findByUserIdAndRoomIdAndStatus(USER_ID, ROOM_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.of(runningSession));
            when(timerSessionMapper.toDto(runningSession)).thenReturn(sampleDto);

            TimerSessionResponseDto result = timerSessionService.getActiveTimer(USER_ID, ROOM_ID);

            assertThat(result).isEqualTo(sampleDto);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no active session exists")
        void getActiveTimer_shouldThrow_whenNoActiveSession() {
            when(timerSessionRepository.findByUserIdAndRoomIdAndStatus(USER_ID, ROOM_ID, TimerStatus.RUNNING))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> timerSessionService.getActiveTimer(USER_ID, ROOM_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No active timer session found");
        }
    }

    // ── getUserSessions ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserSessions() tests")
    class GetUserSessionsTests {

        @Test
        @DisplayName("Should return mapped list of session DTOs within date range")
        void getUserSessions_shouldReturnMappedList() {
            Instant from = Instant.now().minusSeconds(86400);
            Instant to = Instant.now();

            TimerSession doneSession = TimerSession.builder()
                    .id(200L).userId(USER_ID).status(TimerStatus.DONE).build();
            TimerSessionResponseDto doneDto = TimerSessionResponseDto.builder()
                    .id(200L).status(TimerStatus.DONE).build();

            when(timerSessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(USER_ID, from, to))
                    .thenReturn(List.of(doneSession));
            when(timerSessionMapper.toDto(doneSession)).thenReturn(doneDto);

            List<TimerSessionResponseDto> result = timerSessionService.getUserSessions(USER_ID, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Should return empty list when no sessions in range")
        void getUserSessions_shouldReturnEmptyList_whenNoSessions() {
            Instant from = Instant.now().minusSeconds(86400);
            Instant to = Instant.now();

            when(timerSessionRepository.findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(USER_ID, from, to))
                    .thenReturn(List.of());

            List<TimerSessionResponseDto> result = timerSessionService.getUserSessions(USER_ID, from, to);

            assertThat(result).isEmpty();
            verify(timerSessionMapper, never()).toDto(any());
        }
    }

    // ── getUserSessionsInRoom ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserSessionsInRoom() tests")
    class GetUserSessionsInRoomTests {

        @Test
        @DisplayName("Should return filtered sessions for the specified room")
        void getUserSessionsInRoom_shouldReturnFilteredSessions() {
            Instant from = Instant.now().minusSeconds(86400);
            Instant to = Instant.now();

            TimerSession session = TimerSession.builder()
                    .id(300L).userId(USER_ID).roomId(ROOM_ID).status(TimerStatus.DONE).build();
            TimerSessionResponseDto dto = TimerSessionResponseDto.builder()
                    .id(300L).userId(USER_ID).roomId(ROOM_ID).status(TimerStatus.DONE).build();

            when(timerSessionRepository.findByUserIdAndRoomIdAndStartedAtBetweenOrderByStartedAtDesc(
                    USER_ID, ROOM_ID, from, to))
                    .thenReturn(List.of(session));
            when(timerSessionMapper.toDto(session)).thenReturn(dto);

            List<TimerSessionResponseDto> result = timerSessionService.getUserSessionsInRoom(USER_ID, ROOM_ID, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getRoomId()).isEqualTo(ROOM_ID);
        }

        @Test
        @DisplayName("Should return empty list when no sessions exist in the room for the given range")
        void getUserSessionsInRoom_shouldReturnEmpty_whenNoSessions() {
            Instant from = Instant.now().minusSeconds(86400);
            Instant to = Instant.now();

            when(timerSessionRepository.findByUserIdAndRoomIdAndStartedAtBetweenOrderByStartedAtDesc(
                    USER_ID, ROOM_ID, from, to))
                    .thenReturn(List.of());

            List<TimerSessionResponseDto> result = timerSessionService.getUserSessionsInRoom(USER_ID, ROOM_ID, from, to);

            assertThat(result).isEmpty();
        }
    }

    // ── finalizeAbandoned ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("finalizeAbandoned() tests")
    class FinalizeAbandonedTests {

        @Test
        @DisplayName("Should finalize session using member lastActiveAt as endedAt")
        void finalizeAbandoned_shouldFinalizeWithMemberLastActiveAt() {
            Instant startedAt = Instant.now().minusSeconds(600);
            Instant lastActive = Instant.now().minusSeconds(60);

            runningSession.setStartedAt(startedAt);

            RoomMembers member = RoomMembers.builder()
                    .userId(USER_ID).roomId(ROOM_ID)
                    .lastActiveAt(lastActive).timerActive(true).build();

            when(roomMembersRepository.findByUserIdAndRoomId(USER_ID, ROOM_ID))
                    .thenReturn(Optional.of(member));

            timerSessionService.finalizeAbandoned(runningSession);

            assertThat(runningSession.getStatus()).isEqualTo(TimerStatus.DONE);
            assertThat(runningSession.getEndedAt()).isEqualTo(lastActive);
            assertThat(runningSession.getDuration()).isEqualTo(540L); // 600 - 60
            assertThat(member.isTimerActive()).isFalse();

            verify(timerSessionRepository).save(runningSession);
            verify(roomMembersRepository).save(member);
        }

        @Test
        @DisplayName("Should clamp duration to 0 when lastActiveAt is before startedAt")
        void finalizeAbandoned_shouldClampDurationToZero_whenLastActiveBeforeStart() {
            Instant startedAt = Instant.now().minusSeconds(10);
            Instant lastActive = Instant.now().minusSeconds(60); // before startedAt

            runningSession.setStartedAt(startedAt);

            RoomMembers member = RoomMembers.builder()
                    .userId(USER_ID).roomId(ROOM_ID)
                    .lastActiveAt(lastActive).timerActive(true).build();

            when(roomMembersRepository.findByUserIdAndRoomId(USER_ID, ROOM_ID))
                    .thenReturn(Optional.of(member));

            timerSessionService.finalizeAbandoned(runningSession);

            assertThat(runningSession.getDuration()).isEqualTo(0L);
            assertThat(runningSession.getStatus()).isEqualTo(TimerStatus.DONE);
        }

        @Test
        @DisplayName("Should do nothing when no room member record is found")
        void finalizeAbandoned_shouldDoNothing_whenMemberNotFound() {
            when(roomMembersRepository.findByUserIdAndRoomId(USER_ID, ROOM_ID))
                    .thenReturn(Optional.empty());

            timerSessionService.finalizeAbandoned(runningSession);

            verify(timerSessionRepository, never()).save(any());
            verify(roomMembersRepository, never()).save(any());
            assertThat(runningSession.getStatus()).isEqualTo(TimerStatus.RUNNING); // unchanged
        }
    }
}
