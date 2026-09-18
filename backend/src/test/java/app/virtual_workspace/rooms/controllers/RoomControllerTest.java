package app.virtual_workspace.rooms.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.rooms.dtos.room.AllRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomRequestDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.RoomDataResponseDto;
import app.virtual_workspace.rooms.dtos.room.UpdateRoomRequestDto;
import app.virtual_workspace.rooms.models.enums.Visibility;
import app.virtual_workspace.rooms.services.RoomService;
import app.virtual_workspace.shared.dtos.ApiResponse;

@ExtendWith(MockitoExtension.class)
public class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    private AllRoomResponseDto sampleAllRoomDto;
    private CreateRoomResponseDto sampleCreatedRoomDto;
    private RoomDataResponseDto sampleRoomDataDto;

    @BeforeEach
    void setUp() {
        sampleAllRoomDto = new AllRoomResponseDto(1L, "Design Room", "UI/UX", 10L, Visibility.PUBLIC);
        sampleCreatedRoomDto = new CreateRoomResponseDto(1L, "Design Room", "UI/UX", 10L, Visibility.PUBLIC);
        sampleRoomDataDto = new RoomDataResponseDto(1L, "Design Room", "UI/UX", 10L, Visibility.PUBLIC);
    }

    @Nested
    @DisplayName("getAllRooms() tests")
    class GetAllRoomsTests {

        @Test
        @DisplayName("Should return 200 OK with slice of all public rooms")
        void getAllRooms_shouldReturnOkWithSlice() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<AllRoomResponseDto> slice = new SliceImpl<>(List.of(sampleAllRoomDto), pageable, false);

            when(roomService.getAllRooms(pageable)).thenReturn(slice);

            ResponseEntity<ApiResponse<Slice<AllRoomResponseDto>>> response = roomController.getAllRooms(pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Rooms retrieved successfully");
            assertThat(response.getBody().getData()).isEqualTo(slice);

            verify(roomService, times(1)).getAllRooms(pageable);
        }

        @Test
        @DisplayName("Should propagate exception when room retrieval fails")
        void getAllRooms_shouldPropagateException_whenServiceThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(roomService.getAllRooms(pageable))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> roomController.getAllRooms(pageable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("createRoom() tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should return 201 CREATED with created room response when valid")
        void createRoom_shouldReturnCreated_whenRequestIsValid() {
            CreateRoomRequestDto request = new CreateRoomRequestDto("Design Room", "UI/UX", Visibility.PUBLIC);

            when(roomService.createRoom(request)).thenReturn(sampleCreatedRoomDto);

            ResponseEntity<ApiResponse<CreateRoomResponseDto>> response = roomController.createRoom(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Room created successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleCreatedRoomDto);

            verify(roomService, times(1)).createRoom(request);
        }

        @Test
        @DisplayName("Should propagate ResourceAlreadyExistsException when user already has a room")
        void createRoom_shouldPropagateException_whenUserAlreadyHasRoom() {
            CreateRoomRequestDto request = new CreateRoomRequestDto("Room", "Desc", Visibility.PUBLIC);

            when(roomService.createRoom(request))
                    .thenThrow(new ResourceAlreadyExistsException("User already has room"));

            assertThatThrownBy(() -> roomController.createRoom(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("User already has room");
        }
    }

    @Nested
    @DisplayName("room() / getRoomData tests")
    class RoomDataTests {

        @Test
        @DisplayName("Should return 200 OK with room data when room exists and accessible")
        void room_shouldReturnOkWithRoomData() {
            when(roomService.getRoomData(1L)).thenReturn(sampleRoomDataDto);

            ResponseEntity<ApiResponse<RoomDataResponseDto>> response = roomController.room(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Room data retrieved successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleRoomDataDto);

            verify(roomService, times(1)).getRoomData(1L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void room_boundaryIds_shouldQueryServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                RoomDataResponseDto dto = new RoomDataResponseDto(id, "T", "D", 1L, Visibility.PUBLIC);
                when(roomService.getRoomData(id)).thenReturn(dto);

                ResponseEntity<ApiResponse<RoomDataResponseDto>> response = roomController.room(id);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getData().getId()).isEqualTo(id);
                verify(roomService, times(1)).getRoomData(id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when room not found")
        void room_shouldPropagateException_whenRoomNotFound() {
            when(roomService.getRoomData(999L))
                    .thenThrow(new ResourceNotFoundException("Room not found with id: 999"));

            assertThatThrownBy(() -> roomController.room(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room not found with id: 999");
        }
    }

    @Nested
    @DisplayName("updateRoom() tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should return 200 OK with updated room data")
        void updateRoom_shouldReturnOkWithUpdatedRoom() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto("New Title", "New Desc", Visibility.PRIVATE);
            RoomDataResponseDto updatedDto = new RoomDataResponseDto(1L, "New Title", "New Desc", 10L, Visibility.PRIVATE);

            when(roomService.updateRoom(1L, updateDto)).thenReturn(updatedDto);

            ResponseEntity<ApiResponse<RoomDataResponseDto>> response = roomController.updateRoom(1L, updateDto);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Room updated successfully");
            assertThat(response.getBody().getData()).isEqualTo(updatedDto);

            verify(roomService, times(1)).updateRoom(1L, updateDto);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void updateRoom_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto("T", "D", Visibility.PUBLIC);

            for (long id : boundaryIds) {
                when(roomService.updateRoom(id, updateDto)).thenReturn(sampleRoomDataDto);

                ResponseEntity<ApiResponse<RoomDataResponseDto>> response = roomController.updateRoom(id, updateDto);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(roomService, times(1)).updateRoom(id, updateDto);
            }
        }

        @Test
        @DisplayName("Should propagate exception when room update fails downstream")
        void updateRoom_shouldPropagateException_whenUpdateFails() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto();
            when(roomService.updateRoom(1L, updateDto))
                    .thenThrow(new RuntimeException("Update failed"));

            assertThatThrownBy(() -> roomController.updateRoom(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Update failed");
        }
    }

    @Nested
    @DisplayName("deleteRoom() tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should return 200 OK when room is deleted successfully")
        void deleteRoom_shouldReturnOk_whenDeleteSucceeds() {
            doNothing().when(roomService).deleteRoom(1L);

            ResponseEntity<ApiResponse<Void>> response = roomController.deleteRoom(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Room deleted successfully");
            assertThat(response.getBody().getData()).isNull();

            verify(roomService, times(1)).deleteRoom(1L);
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void deleteRoom_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<Void>> response = roomController.deleteRoom(id);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(roomService, times(1)).deleteRoom(id);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when room does not exist")
        void deleteRoom_shouldPropagateException_whenRoomNotFound() {
            doThrow(new ResourceNotFoundException("Room Not Found"))
                    .when(roomService).deleteRoom(999L);

            assertThatThrownBy(() -> roomController.deleteRoom(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room Not Found");
        }
    }
}
