package app.virtual_workspace.rooms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.repositories.RoomRepository;

@ExtendWith(MockitoExtension.class)
public class RoomAuthServiceTest {

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomAuthService roomAuthService;

    private User authUser;
    private User otherUser;
    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        authUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        sampleRoom = Room.builder()
                .id(100L)
                .title("Workspace Room")
                .user(authUser)
                .build();
    }

    @Nested
    @DisplayName("isOwner() tests")
    class IsOwnerTests {

        @Test
        @DisplayName("Should return true when authenticated user is the owner of the room")
        void isOwner_shouldReturnTrue_whenUserIsOwner() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(roomRepository.getRoomById(100L)).thenReturn(sampleRoom);

            boolean result = roomAuthService.isOwner(100L);

            assertThat(result).isTrue();
            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(roomRepository, times(1)).getRoomById(100L);
        }

        @Test
        @DisplayName("Should return false when authenticated user is not the owner of the room")
        void isOwner_shouldReturnFalse_whenUserIsNotOwner() {
            sampleRoom.setUser(otherUser);
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(roomRepository.getRoomById(100L)).thenReturn(sampleRoom);

            boolean result = roomAuthService.isOwner(100L);

            assertThat(result).isFalse();
            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(roomRepository, times(1)).getRoomById(100L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void isOwner_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                Room room = Room.builder()
                        .id(id)
                        .title("Room " + id)
                        .user(authUser)
                        .build();

                when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
                when(roomRepository.getRoomById(id)).thenReturn(room);

                boolean result = roomAuthService.isOwner(id);

                assertThat(result).isTrue();
                verify(roomRepository, times(1)).getRoomById(id);
            }
        }

        @Test
        @DisplayName("Should propagate exception when userAuthService.getAuthenticatedUser throws")
        void isOwner_shouldPropagateException_whenUserAuthServiceThrows() {
            when(userAuthService.getAuthenticatedUser())
                    .thenThrow(new ResourceNotFoundException("No authenticated user"));

            assertThatThrownBy(() -> roomAuthService.isOwner(100L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No authenticated user");

            verifyNoInteractions(roomRepository);
        }

        @Test
        @DisplayName("Should propagate exception when roomRepository.getRoomById throws downstream")
        void isOwner_shouldPropagateException_whenRoomRepositoryThrows() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(roomRepository.getRoomById(100L))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> roomAuthService.isOwner(100L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(roomRepository, times(1)).getRoomById(100L);
        }
    }
}
