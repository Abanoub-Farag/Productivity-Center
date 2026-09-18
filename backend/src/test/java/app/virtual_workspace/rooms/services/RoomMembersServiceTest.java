package app.virtual_workspace.rooms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto;
import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.models.RoomMembers;
import app.virtual_workspace.rooms.models.enums.Status;
import app.virtual_workspace.rooms.repositories.RoomMembersRepository;
import app.virtual_workspace.rooms.repositories.RoomRepository;

@ExtendWith(MockitoExtension.class)
public class RoomMembersServiceTest {

    @Mock
    private RoomMembersRepository roomMembersRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserAuthService userAuthService;

    @InjectMocks
    private RoomMembersService roomMembersService;

    private User sampleUser;
    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        sampleRoom = Room.builder()
                .id(10L)
                .title("Tech Room")
                .build();
    }

    @Nested
    @DisplayName("joinRoom() tests")
    class JoinRoomTests {

        @Test
        @DisplayName("Should return early and not save when user is already a room member")
        void joinRoom_shouldDoNothing_whenUserAlreadyMember() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(true);

            roomMembersService.joinRoom(10L);

            verify(roomMembersRepository, times(1)).existsByUserIdAndRoomId(1L, 10L);
            verify(roomRepository, never()).findById(any());
            verify(roomMembersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save RoomMembers with status ONLINE when user is not a member and room exists")
        void joinRoom_shouldSaveRoomMembers_whenNotMemberAndRoomExists() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));

            roomMembersService.joinRoom(10L);

            verify(roomRepository, times(1)).findById(10L);
            ArgumentCaptor<RoomMembers> captor = ArgumentCaptor.forClass(RoomMembers.class);
            verify(roomMembersRepository, times(1)).save(captor.capture());

            RoomMembers saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(sampleUser);
            assertThat(saved.getRoom()).isEqualTo(sampleRoom);
            assertThat(saved.getStatus()).isEqualTo(Status.ONLINE);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room is not found")
        void joinRoom_shouldThrowResourceNotFoundException_whenRoomNotFound() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 999L)).thenReturn(false);
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomMembersService.joinRoom(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room Not Found");

            verify(roomMembersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void joinRoom_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                Room r = Room.builder().id(id).build();
                when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
                when(roomMembersRepository.existsByUserIdAndRoomId(1L, id)).thenReturn(false);
                when(roomRepository.findById(id)).thenReturn(Optional.of(r));

                roomMembersService.joinRoom(id);

                verify(roomRepository, times(1)).findById(id);
            }
            verify(roomMembersRepository, times(boundaryIds.length)).save(any(RoomMembers.class));
        }

        @Test
        @DisplayName("Should propagate exception when roomMembersRepository.save throws downstream")
        void joinRoom_shouldPropagateException_whenSaveThrows() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(roomMembersRepository.save(any(RoomMembers.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> roomMembersService.joinRoom(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }

    @Nested
    @DisplayName("heartBeat() tests")
    class HeartBeatTests {

        @Test
        @DisplayName("Should update last active timestamp for user and room")
        void heartBeat_shouldUpdateLastActiveTimestamp() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);

            roomMembersService.heartBeat(10L);

            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(roomMembersRepository, times(1))
                    .updateLastActiveAt(eq(1L), eq(10L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should propagate exception when userAuthService fails during heartbeat")
        void heartBeat_shouldPropagateException_whenUserAuthServiceThrows() {
            when(userAuthService.getAuthenticatedUser())
                    .thenThrow(new ResourceNotFoundException("Unauthenticated"));

            assertThatThrownBy(() -> roomMembersService.heartBeat(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Unauthenticated");

            verify(roomMembersRepository, never()).updateLastActiveAt(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getRoomMembers() tests")
    class GetRoomMembersTests {

        @Test
        @DisplayName("Should return list of room members")
        void getRoomMembers_shouldReturnMemberList() {
            RoomMemberDto memberDto = RoomMemberDto.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .status(Status.ONLINE)
                    .build();

            when(roomMembersRepository.findByRoomIdWithProfileAndUser(10L))
                    .thenReturn(List.of(memberDto));

            List<RoomMemberDto> result = roomMembersService.getRoomMembers(10L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getFirstName()).isEqualTo("John");
            verify(roomMembersRepository, times(1)).findByRoomIdWithProfileAndUser(10L);
        }

        @Test
        @DisplayName("Should return empty list when room has no members")
        void getRoomMembers_shouldReturnEmptyList_whenNoMembers() {
            when(roomMembersRepository.findByRoomIdWithProfileAndUser(10L))
                    .thenReturn(Collections.emptyList());

            List<RoomMemberDto> result = roomMembersService.getRoomMembers(10L);

            assertThat(result).isEmpty();
            verify(roomMembersRepository, times(1)).findByRoomIdWithProfileAndUser(10L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void getRoomMembers_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                when(roomMembersRepository.findByRoomIdWithProfileAndUser(id))
                        .thenReturn(Collections.emptyList());

                List<RoomMemberDto> result = roomMembersService.getRoomMembers(id);

                assertThat(result).isEmpty();
                verify(roomMembersRepository, times(1)).findByRoomIdWithProfileAndUser(id);
            }
        }

        @Test
        @DisplayName("Should propagate exception when repository throws downstream")
        void getRoomMembers_shouldPropagateException_whenRepositoryThrows() {
            when(roomMembersRepository.findByRoomIdWithProfileAndUser(10L))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> roomMembersService.getRoomMembers(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }
}
