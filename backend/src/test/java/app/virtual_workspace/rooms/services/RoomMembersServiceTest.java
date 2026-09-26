package app.virtual_workspace.rooms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import app.virtual_workspace.accounts.services.UserReferenceProvider;
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
    private UserReferenceProvider userReferenceProvider;

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

        org.mockito.Mockito.lenient().when(userReferenceProvider.getReference(any())).thenReturn(sampleUser);
    }

    @Nested
    @DisplayName("joinRoom() tests")
    class JoinRoomTests {

        @Test
        @DisplayName("Should return early and not save when user is already a room member")
        void joinRoom_shouldDoNothing_whenUserAlreadyMember() {
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(true);

            roomMembersService.joinRoom(1L, 10L);

            verify(roomMembersRepository, times(1)).existsByUserIdAndRoomId(1L, 10L);
            verify(roomRepository, never()).findById(any());
            verify(roomMembersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save RoomMembers with status ONLINE when user is not a member and room exists")
        void joinRoom_shouldSaveRoomMembers_whenNotMemberAndRoomExists() {
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));

            roomMembersService.joinRoom(1L, 10L);

            verify(roomRepository, times(1)).findById(10L);
            ArgumentCaptor<RoomMembers> captor = ArgumentCaptor.forClass(RoomMembers.class);
            verify(roomMembersRepository, times(1)).save(captor.capture());

            RoomMembers saved = captor.getValue();
            assertThat(saved.getUser().getId()).isEqualTo(1L);
            assertThat(saved.getRoom().getId()).isEqualTo(10L);
            assertThat(saved.getStatus()).isEqualTo(Status.ONLINE);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room is not found")
        void joinRoom_shouldThrowResourceNotFoundException_whenRoomNotFound() {
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 999L)).thenReturn(false);
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomMembersService.joinRoom(1L, 999L))
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
                when(roomMembersRepository.existsByUserIdAndRoomId(1L, id)).thenReturn(false);
                when(roomRepository.findById(id)).thenReturn(Optional.of(r));

                roomMembersService.joinRoom(1L, id);

                verify(roomRepository, times(1)).findById(id);
            }
            verify(roomMembersRepository, times(boundaryIds.length)).save(any(RoomMembers.class));
        }

        @Test
        @DisplayName("Should propagate exception when roomMembersRepository.save throws downstream")
        void joinRoom_shouldPropagateException_whenSaveThrows() {
            when(roomMembersRepository.existsByUserIdAndRoomId(1L, 10L)).thenReturn(false);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(roomMembersRepository.save(any(RoomMembers.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> roomMembersService.joinRoom(1L, 10L))
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
            roomMembersService.heartBeat(1L, 10L, true);

            verify(roomMembersRepository, times(1))
                    .updateHeartbeat(eq(1L), eq(10L), any(LocalDateTime.class), eq(true));
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void heartBeat_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                roomMembersService.heartBeat(1L, id, false);

                verify(roomMembersRepository, times(1))
                        .updateHeartbeat(eq(1L), eq(id), any(LocalDateTime.class), eq(false));
            }
        }

        @Test
        @DisplayName("Should propagate exception when repository throws downstream")
        void heartBeat_shouldPropagateException_whenRepositoryThrows() {
            doThrow(new RuntimeException("Database error"))
                    .when(roomMembersRepository).updateHeartbeat(eq(1L), eq(10L), any(LocalDateTime.class), eq(false));

            assertThatThrownBy(() -> roomMembersService.heartBeat(1L, 10L, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
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
