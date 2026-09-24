package app.virtual_workspace.rooms.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.services.UserReferenceProvider;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.favoriteroom.FavoriteRoomResponseDto;
import app.virtual_workspace.rooms.mappers.FavoriteRoomMapper;
import app.virtual_workspace.rooms.models.FavoriteRoom;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.repositories.FavoriteRoomRepository;
import app.virtual_workspace.rooms.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteRoomService {

    private final FavoriteRoomRepository favoriteRoomRepository;
    private final RoomRepository roomRepository;
    private final FavoriteRoomMapper favoriteRoomMapper;
    private final UserReferenceProvider userReferenceProvider;

    @Transactional(readOnly = true)
    public Slice<FavoriteRoomResponseDto> getFavoriteRooms(Long userId, Pageable pageable) {
        Slice<FavoriteRoom> favoriteRooms = favoriteRoomRepository.findFavoriteRoomsByUserId(userId, pageable);

        return favoriteRooms.map(favoriteRoomMapper::modelToFavoriteRoomResponseDto);
    }

    @Transactional
    public FavoriteRoomResponseDto addRoomToFavorite(Long userId, Long roomId) {
        boolean favoriteRoomExist = favoriteRoomRepository.existsByUserIdAndRoomId(userId, roomId);

        if (favoriteRoomExist)
            throw new ResourceAlreadyExistsException("Room with id: " + roomId + " is already in favorited");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("No room found with id: " + roomId));

        FavoriteRoom favoriteRoom = new FavoriteRoom();
        favoriteRoom.setRoom(room);
        favoriteRoom.setUser(userReferenceProvider.getReference(userId));

        favoriteRoomRepository.save(favoriteRoom);

        return favoriteRoomMapper.modelToFavoriteRoomResponseDto(favoriteRoom);
    }

    @Transactional()
    public void removeRoomFromFavorite(Long userId, Long roomId) {
        FavoriteRoom favoriteRoom = favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("This room is not in your favorite list"));

        favoriteRoomRepository.delete(favoriteRoom);
    }

}
