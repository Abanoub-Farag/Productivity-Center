package app.virtual_workspace.rooms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.favoriteroom.FavoriteRoomResponseDto;
import app.virtual_workspace.rooms.mappers.FavoriteRoomMapper;
import app.virtual_workspace.rooms.models.FavoriteRoom;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.repositories.FavoriteRoomRepository;
import app.virtual_workspace.rooms.repositories.RoomRepository;

@ExtendWith(MockitoExtension.class)
public class FavoriteRoomServiceTest {

    @Mock
    private FavoriteRoomRepository favoriteRoomRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private FavoriteRoomMapper favoriteRoomMapper;

    @InjectMocks
    private FavoriteRoomService favoriteRoomService;

    private User sampleUser;
    private Room sampleRoom;
    private FavoriteRoom sampleFavoriteRoom;
    private FavoriteRoomResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        sampleRoom = Room.builder()
                .id(10L)
                .title("Conference Room")
                .description("Main room")
                .build();

        sampleFavoriteRoom = new FavoriteRoom(100L, sampleUser, 1L, sampleRoom, 10L, LocalDateTime.now());

        sampleResponseDto = new FavoriteRoomResponseDto(
                10L,
                "Conference Room",
                "Main room",
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("getFavoriteRooms() tests")
    class GetFavoriteRoomsTests {

        @Test
        @DisplayName("Should return mapped slice of favorite rooms when favorites exist")
        void getFavoriteRooms_shouldReturnMappedSlice_whenFavoritesExist() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FavoriteRoom> favoriteRoomSlice = new SliceImpl<>(List.of(sampleFavoriteRoom), pageable, false);

            when(favoriteRoomRepository.findFavoriteRoomsByUserId(1L, pageable)).thenReturn(favoriteRoomSlice);
            when(favoriteRoomMapper.modelToFavoriteRoomResponseDto(sampleFavoriteRoom)).thenReturn(sampleResponseDto);

            Slice<FavoriteRoomResponseDto> result = favoriteRoomService.getFavoriteRooms(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getRoomId()).isEqualTo(10L);
            assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Conference Room");

            verify(favoriteRoomRepository, times(1)).findFavoriteRoomsByUserId(1L, pageable);
            verify(favoriteRoomMapper, times(1)).modelToFavoriteRoomResponseDto(sampleFavoriteRoom);
        }

        @Test
        @DisplayName("Should return empty slice when user has no favorite rooms")
        void getFavoriteRooms_shouldReturnEmptySlice_whenNoFavorites() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FavoriteRoom> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

            when(favoriteRoomRepository.findFavoriteRoomsByUserId(1L, pageable)).thenReturn(emptySlice);

            Slice<FavoriteRoomResponseDto> result = favoriteRoomService.getFavoriteRooms(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(favoriteRoomMapper, never()).modelToFavoriteRoomResponseDto(any(FavoriteRoom.class));
        }

        @Test
        @DisplayName("Should propagate exception when repository throws")
        void getFavoriteRooms_shouldPropagateException_whenRepositoryThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(favoriteRoomRepository.findFavoriteRoomsByUserId(1L, pageable))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> favoriteRoomService.getFavoriteRooms(1L, pageable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("addRoomToFavorite() tests")
    class AddRoomToFavoriteTests {

        @Test
        @DisplayName("Should do nothing when room is already in favorites (early return)")
        void addRoomToFavorite_shouldDoNothing_whenRoomAlreadyInFavorites() {
            when(favoriteRoomRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(true);

            favoriteRoomService.addRoomToFavorite(1L, 10L);

            verify(favoriteRoomRepository, times(1)).existsByUserIdAndRoomId(1L, 10L);
            verify(roomRepository, never()).findById(any());
            verify(favoriteRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save favorite room when room is not in favorites and room exists")
        void addRoomToFavorite_shouldSaveFavoriteRoom_whenNotAlreadyInFavorites() {
            when(favoriteRoomRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));

            favoriteRoomService.addRoomToFavorite(1L, 10L);

            verify(roomRepository, times(1)).findById(10L);
            ArgumentCaptor<FavoriteRoom> captor = ArgumentCaptor.forClass(FavoriteRoom.class);
            verify(favoriteRoomRepository, times(1)).save(captor.capture());

            FavoriteRoom saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getRoomId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room is not found")
        void addRoomToFavorite_shouldThrowResourceNotFoundException_whenRoomNotFound() {
            when(favoriteRoomRepository.existsByUserIdAndRoomId(1L, 999L)).thenReturn(false);
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteRoomService.addRoomToFavorite(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No room found with id: 999");

            verify(favoriteRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void addRoomToFavorite_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                Room r = Room.builder().id(id).build();
                when(favoriteRoomRepository.existsByUserIdAndRoomId(1L, id)).thenReturn(false);
                when(roomRepository.findById(id)).thenReturn(Optional.of(r));

                favoriteRoomService.addRoomToFavorite(1L, id);

                verify(roomRepository, times(1)).findById(id);
            }
            verify(favoriteRoomRepository, times(boundaryIds.length)).save(any(FavoriteRoom.class));
        }

        @Test
        @DisplayName("Should propagate exception when favoriteRoomRepository.save throws downstream")
        void addRoomToFavorite_shouldPropagateException_whenSaveThrows() {
            when(favoriteRoomRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(favoriteRoomRepository.save(any(FavoriteRoom.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> favoriteRoomService.addRoomToFavorite(1L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("removeRoomFromFavorite() tests")
    class RemoveRoomFromFavoriteTests {

        @Test
        @DisplayName("Should delete favorite room when it is in the favorite list")
        void removeRoomFromFavorite_shouldDeleteFavoriteRoom_whenExists() {
            when(favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(1L, 10L))
                    .thenReturn(Optional.of(sampleFavoriteRoom));

            favoriteRoomService.removeRoomFromFavorite(1L, 10L);

            verify(favoriteRoomRepository, times(1)).findFavoriteRoomByUserIdAndRoomId(1L, 10L);
            verify(favoriteRoomRepository, times(1)).delete(sampleFavoriteRoom);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room is not in favorite list")
        void removeRoomFromFavorite_shouldThrowException_whenNotInFavorites() {
            when(favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteRoomService.removeRoomFromFavorite(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("This room is not in your favorite list");

            verify(favoriteRoomRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void removeRoomFromFavorite_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                FavoriteRoom fr = new FavoriteRoom(id, sampleUser, 1L, sampleRoom, id, LocalDateTime.now());
                when(favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(1L, id))
                        .thenReturn(Optional.of(fr));

                favoriteRoomService.removeRoomFromFavorite(1L, id);

                verify(favoriteRoomRepository, times(1)).delete(fr);
            }
        }

        @Test
        @DisplayName("Should propagate exception when delete throws downstream")
        void removeRoomFromFavorite_shouldPropagateException_whenDeleteThrows() {
            when(favoriteRoomRepository.findFavoriteRoomByUserIdAndRoomId(1L, 10L))
                    .thenReturn(Optional.of(sampleFavoriteRoom));
            org.mockito.Mockito.doThrow(new RuntimeException("Delete failed"))
                    .when(favoriteRoomRepository).delete(sampleFavoriteRoom);

            assertThatThrownBy(() -> favoriteRoomService.removeRoomFromFavorite(1L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete failed");
        }
    }
}
