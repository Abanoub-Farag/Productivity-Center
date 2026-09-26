package app.virtual_workspace.rooms.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.services.UserReferenceProvider;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.models.RoomMembers;
import app.virtual_workspace.rooms.models.enums.Status;
import app.virtual_workspace.rooms.repositories.RoomMembersRepository;
import app.virtual_workspace.rooms.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RoomMembersService {

    private final RoomMembersRepository roomMembersRepository;
    private final RoomRepository roomRepository;
    private final UserReferenceProvider userReferenceProvider;

    @Transactional
    @CacheEvict(value = "room_members", key = "#roomId")
    public void joinRoom(Long userId, Long roomId) {

        if (roomMembersRepository.existsByUserIdAndRoomId(userId, roomId)) {
            return;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found"));

        RoomMembers roomMembers = RoomMembers.builder()
                .user(userReferenceProvider.getReference(userId))
                .room(room)
                .status(Status.ONLINE)
                .build();

        roomMembersRepository.save(roomMembers);

    }

    @Transactional
    public void heartBeat(Long userId, Long roomId, boolean timerActive) {
        roomMembersRepository.updateHeartbeat(userId, roomId, LocalDateTime.now(), timerActive);
    }

    @Transactional(readOnly = true)
    public RoomMembers findMember(Long userId, Long roomId) {
        return roomMembersRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room membership not found"));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "room_members", key = "#roomId")
    public List<RoomMemberDto> getRoomMembers(Long roomId) {
        return roomMembersRepository.findByRoomIdWithProfileAndUser(roomId);
    }

}

