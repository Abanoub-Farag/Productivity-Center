package app.virtual_workspace.rooms.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import app.virtual_workspace.accounts.models.enums.Gender;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto;
import app.virtual_workspace.rooms.models.enums.Status;
import app.virtual_workspace.rooms.services.RoomMembersService;
import app.virtual_workspace.shared.dtos.ApiResponse;

@ExtendWith(MockitoExtension.class)
public class RoomMembersControllerTest {

    @Mock
    private RoomMembersService roomMembersService;

    @InjectMocks
    private RoomMembersController roomMembersController;

    private RoomMemberDto sampleMemberDto;

    @BeforeEach
    void setUp() {
        sampleMemberDto = RoomMemberDto.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .bio("Developer")
                .status(Status.ONLINE)
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(1996, 3, 15))
                .build();
    }

    @Nested
    @DisplayName("joinRoom() tests")
    class JoinRoomTests {

        @Test
        @DisplayName("Should return 200 OK when user joins room successfully")
        void joinRoom_shouldReturnOk_whenJoinSucceeds() {
            doNothing().when(roomMembersService).joinRoom(10L);

            ResponseEntity<ApiResponse<Void>> response = roomMembersController.joinRoom(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Joined the room successfully");
            assertThat(response.getBody().getData()).isNull();

            verify(roomMembersService, times(1)).joinRoom(10L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void joinRoom_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<Void>> response = roomMembersController.joinRoom(id);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(roomMembersService, times(1)).joinRoom(id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when room does not exist")
        void joinRoom_shouldPropagateException_whenRoomNotFound() {
            doThrow(new ResourceNotFoundException("Room Not Found"))
                    .when(roomMembersService).joinRoom(999L);

            assertThatThrownBy(() -> roomMembersController.joinRoom(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room Not Found");
        }
    }

    @Nested
    @DisplayName("heartBeat() tests")
    class HeartBeatTests {

        @Test
        @DisplayName("Should return 200 OK when heartbeat succeeds")
        void heartBeat_shouldReturnOk_whenHeartBeatSucceeds() {
            doNothing().when(roomMembersService).heartBeat(10L);

            ResponseEntity<ApiResponse<Void>> response = roomMembersController.heartBeat(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Heartbeat done");
            assertThat(response.getBody().getData()).isNull();

            verify(roomMembersService, times(1)).heartBeat(10L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void heartBeat_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<Void>> response = roomMembersController.heartBeat(id);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(roomMembersService, times(1)).heartBeat(id);
            }
        }

        @Test
        @DisplayName("Should propagate exception when heartbeat fails downstream")
        void heartBeat_shouldPropagateException_whenServiceThrows() {
            doThrow(new RuntimeException("Heartbeat failure"))
                    .when(roomMembersService).heartBeat(10L);

            assertThatThrownBy(() -> roomMembersController.heartBeat(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Heartbeat failure");
        }
    }

    @Nested
    @DisplayName("getRoomMembers() tests")
    class GetRoomMembersTests {

        @Test
        @DisplayName("Should return 200 OK with list of room members")
        void getRoomMembers_shouldReturnOkWithMembers() {
            when(roomMembersService.getRoomMembers(10L)).thenReturn(List.of(sampleMemberDto));

            ResponseEntity<ApiResponse<List<RoomMemberDto>>> response = roomMembersController.getRoomMembers(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Room members retrieved successfully");
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().getFirst().getFirstName()).isEqualTo("Alice");

            verify(roomMembersService, times(1)).getRoomMembers(10L);
        }

        @Test
        @DisplayName("Should return 200 OK with empty list when room has no members")
        void getRoomMembers_shouldReturnOkWithEmptyList() {
            when(roomMembersService.getRoomMembers(10L)).thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse<List<RoomMemberDto>>> response = roomMembersController.getRoomMembers(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).isEmpty();

            verify(roomMembersService, times(1)).getRoomMembers(10L);
        }

        @Test
        @DisplayName("Should propagate exception when member retrieval fails downstream")
        void getRoomMembers_shouldPropagateException_whenServiceThrows() {
            when(roomMembersService.getRoomMembers(10L))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> roomMembersController.getRoomMembers(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }
}
