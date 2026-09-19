package app.virtual_workspace.rooms.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.favoriteroom.FavoriteRoomResponseDto;
import app.virtual_workspace.rooms.mappers.FavoriteRoomMapper;
import app.virtual_workspace.rooms.models.FavoriteRoom;
import app.virtual_workspace.rooms.repositories.FavoriteRoomRepository;
import app.virtual_workspace.rooms.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteRoomService {

    private final FavoriteRoomRepository favoriteRoomRepository;
    private final RoomRepository roomRepository;
    private final FavoriteRoomMapper favoriteRoomMapper;
    // private final UserAuthService userAuthService;

    public Slice<FavoriteRoomResponseDto> getFavoriteRooms(Long userId, Pageable pageable) {
        Slice<FavoriteRoom> favoriteRooms = favoriteRoomRepository.findFavoriteRoomsByUserId(userId, pageable);

        return favoriteRooms.map(favoriteRoomMapper::modelToFavoriteRoomResponseDto);
    }

    public void addRoomToFavorite(Long userId, Long roomId) {
        boolean favoriteRoomExist = favoriteRoomRepository.existsByUserIdAndRoomId(userId, roomId);

        if (favoriteRoomExist)
            return;

        roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("No room found with id: " + roomId));

        FavoriteRoom favoriteRoom = new FavoriteRoom();
        favoriteRoom.setRoomId(roomId);
        favoriteRoom.setUserId(userId);

        favoriteRoomRepository.save(favoriteRoom);
    }

    public void removeRoomFromFavorite(Long userId, Long roomId) {
        FavoriteRoom favoriteRoom = favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("This room is not in your favorite list"));

        favoriteRoomRepository.delete(favoriteRoom);
    }

}
