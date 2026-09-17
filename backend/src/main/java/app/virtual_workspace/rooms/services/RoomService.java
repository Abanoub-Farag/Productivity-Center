package app.virtual_workspace.rooms.services;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.room.AllRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomRequestDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.RoomDataResponseDto;
import app.virtual_workspace.rooms.dtos.room.UpdateRoomRequestDto;
import app.virtual_workspace.rooms.mappers.RoomMapper;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.models.enums.Visibility;
import app.virtual_workspace.rooms.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserAuthService userAuthService;
    private final RoomMapper roomMapper;

    public Slice<AllRoomResponseDto> getAllRooms(Pageable pageable) {
        Slice<Room> rooms = roomRepository.findByVisibilityNot(Visibility.PRIVATE, pageable);

        return rooms.map(roomMapper::toAllRoomResponseDto);
    }

    @Transactional
    public CreateRoomResponseDto createRoom(
            CreateRoomRequestDto createRoomRequestDto) {
        User user = userAuthService.getAuthenticatedUser();

        if (roomRepository.existsByUserId(user.getId())) {
            throw new ResourceAlreadyExistsException("User already has room");
        }

        Room room = roomMapper.createRoomRequestDtoToModel(createRoomRequestDto);

        room.setUser(user);
        roomRepository.save(room);

        return roomMapper.createRoomRequestToResponse(room);
    }

    public RoomDataResponseDto getRoomData(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

        User user = userAuthService.getAuthenticatedUser();

        if (room.getVisibility() != Visibility.PUBLIC && !room.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied for this room");
        }

        return roomMapper.roomModelToDto(room);
    }

    @Transactional
    public RoomDataResponseDto updateRoom(
            Long roomId,
            UpdateRoomRequestDto updateRoomRequestDto) {
        Room room = roomRepository.getRoomById(roomId);

        if (updateRoomRequestDto.getTitle() != null) {
            room.setTitle(updateRoomRequestDto.getTitle());
        }

        if (updateRoomRequestDto.getDescription() != null) {
            room.setDescription(updateRoomRequestDto.getDescription());
        }

        if (updateRoomRequestDto.getVisibility() != null) {
            room.setVisibility(updateRoomRequestDto.getVisibility());
        }

        roomRepository.save(room);
        return roomMapper.roomModelToDto(room);
    }

    @Transactional
    @CacheEvict(value = "room_members", key = "#roomId")
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found"));

        roomRepository.delete(room);
    }

}
