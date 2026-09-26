package app.virtual_workspace.rooms.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.favoriteroom.FavoriteRoomResponseDto;
import app.virtual_workspace.rooms.services.FavoriteRoomService;
import app.virtual_workspace.shared.dtos.ApiResponse;

@ExtendWith(MockitoExtension.class)
public class FavoriteRoomControllerTest {

    @Mock
    private FavoriteRoomService favoriteRoomService;

    @InjectMocks
    private FavoriteRoomController favoriteRoomController;

    private UserPrincipal userPrincipal;
    private FavoriteRoomResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("user@example.com")
                .password("password")
                .active(true)
                .role(Role.ROLE_USER)
                .build();

        sampleResponseDto = new FavoriteRoomResponseDto(
                10L,
                "Lounge Room",
                "Relax and chat",
                LocalDateTime.now());
    }

    @Nested
    @DisplayName("getFavoriteRooms() tests")
    class GetFavoriteRoomsTests {

        @Test
        @DisplayName("Should return 200 OK with slice of favorite rooms")
        void getFavoriteRooms_shouldReturnOkWithSlice() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FavoriteRoomResponseDto> slice = new SliceImpl<>(List.of(sampleResponseDto), pageable, false);

            when(favoriteRoomService.getFavoriteRooms(1L, pageable)).thenReturn(slice);

            ResponseEntity<ApiResponse<Slice<FavoriteRoomResponseDto>>> response = favoriteRoomController
                    .getFavoriteRooms(pageable, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Returned Favorite Rooms Successfully");
            assertThat(response.getBody().getData()).isEqualTo(slice);
            assertThat(response.getBody().getData().getContent()).hasSize(1);

            verify(favoriteRoomService, times(1)).getFavoriteRooms(1L, pageable);
        }

        @Test
        @DisplayName("Should propagate exception when favorite room retrieval fails")
        void getFavoriteRooms_shouldPropagateException_whenServiceThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(favoriteRoomService.getFavoriteRooms(1L, pageable))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> favoriteRoomController.getFavoriteRooms(pageable, userPrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("addRoomToFavorite() tests")
    class AddRoomToFavoriteTests {

        @Test
        @DisplayName("Should return 200 OK when room is added to favorites successfully")
        void addRoomToFavorite_shouldReturnOk_whenAddSucceeds() {
            when(favoriteRoomService.addRoomToFavorite(1L, 10L)).thenReturn(sampleResponseDto);

            ResponseEntity<ApiResponse<FavoriteRoomResponseDto>> response = favoriteRoomController
                    .addRoomToFavorite(10L, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Added room to favorites successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleResponseDto);

            verify(favoriteRoomService, times(1)).addRoomToFavorite(1L, 10L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void addRoomToFavorite_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = { 0L, -1L, Long.MAX_VALUE };

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<FavoriteRoomResponseDto>> response = favoriteRoomController
                        .addRoomToFavorite(id, userPrincipal);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(favoriteRoomService, times(1)).addRoomToFavorite(1L, id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when room does not exist")
        void addRoomToFavorite_shouldPropagateException_whenRoomNotFound() {
            doThrow(new ResourceNotFoundException("No room found with id: 999"))
                    .when(favoriteRoomService).addRoomToFavorite(1L, 999L);

            assertThatThrownBy(() -> favoriteRoomController.addRoomToFavorite(999L, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No room found with id: 999");
        }
    }

    @Nested
    @DisplayName("removeRoomFromFavorite() tests")
    class RemoveRoomFromFavoriteTests {

        @Test
        @DisplayName("Should return 200 OK when room is removed from favorites successfully")
        void removeRoomFromFavorite_shouldReturnOk_whenRemoveSucceeds() {
            doNothing().when(favoriteRoomService).removeRoomFromFavorite(1L, 10L);

            ResponseEntity<ApiResponse<Void>> response = favoriteRoomController.removeRoomFromFavorite(10L,
                    userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Removed room from favorites successfully");
            assertThat(response.getBody().getData()).isNull();

            verify(favoriteRoomService, times(1)).removeRoomFromFavorite(1L, 10L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void removeRoomFromFavorite_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = { 0L, -1L, Long.MAX_VALUE };

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<Void>> response = favoriteRoomController.removeRoomFromFavorite(id,
                        userPrincipal);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(favoriteRoomService, times(1)).removeRoomFromFavorite(1L, id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when room is not in favorites")
        void removeRoomFromFavorite_shouldPropagateException_whenNotInFavorites() {
            doThrow(new ResourceNotFoundException("This room is not in your favorite list"))
                    .when(favoriteRoomService).removeRoomFromFavorite(1L, 999L);

            assertThatThrownBy(() -> favoriteRoomController.removeRoomFromFavorite(999L, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("This room is not in your favorite list");
        }
    }
}
