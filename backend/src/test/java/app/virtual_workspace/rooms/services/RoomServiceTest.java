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
import org.springframework.security.access.AccessDeniedException;

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

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private User ownerUser;
    private User otherUser;
    private Room sampleRoom;
    private AllRoomResponseDto sampleAllRoomDto;
    private RoomDataResponseDto sampleRoomDataDto;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        sampleRoom = Room.builder()
                .id(10L)
                .title("Conference A")
                .description("Discussion Room")
                .visibility(Visibility.PUBLIC)
                .user(ownerUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleAllRoomDto = new AllRoomResponseDto(10L, "Conference A", "Discussion Room", 1L, Visibility.PUBLIC);
        sampleRoomDataDto = new RoomDataResponseDto(10L, "Conference A", "Discussion Room", 1L, Visibility.PUBLIC);
    }

    @Nested
    @DisplayName("getAllRooms() tests")
    class GetAllRoomsTests {

        @Test
        @DisplayName("Should return mapped slice of non-private rooms")
        void getAllRooms_shouldReturnMappedSlice() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<Room> roomSlice = new SliceImpl<>(List.of(sampleRoom), pageable, false);

            when(roomRepository.findByVisibilityNot(Visibility.PRIVATE, pageable)).thenReturn(roomSlice);
            when(roomMapper.toAllRoomResponseDto(sampleRoom)).thenReturn(sampleAllRoomDto);

            Slice<AllRoomResponseDto> result = roomService.getAllRooms(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Conference A");

            verify(roomRepository, times(1)).findByVisibilityNot(Visibility.PRIVATE, pageable);
            verify(roomMapper, times(1)).toAllRoomResponseDto(sampleRoom);
        }

        @Test
        @DisplayName("Should return empty slice when no public rooms exist")
        void getAllRooms_shouldReturnEmptySlice_whenNoRooms() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<Room> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

            when(roomRepository.findByVisibilityNot(Visibility.PRIVATE, pageable)).thenReturn(emptySlice);

            Slice<AllRoomResponseDto> result = roomService.getAllRooms(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            verify(roomMapper, never()).toAllRoomResponseDto(any());
        }

        @Test
        @DisplayName("Should propagate exception when roomRepository throws downstream")
        void getAllRooms_shouldPropagateException_whenRepositoryThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(roomRepository.findByVisibilityNot(Visibility.PRIVATE, pageable))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> roomService.getAllRooms(pageable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("createRoom() tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should successfully create room when user does not have an existing room")
        void createRoom_shouldCreateRoom_whenUserHasNoRoom() {
            CreateRoomRequestDto requestDto = new CreateRoomRequestDto("New Room", "Room desc", Visibility.PUBLIC);
            Room mappedRoom = Room.builder()
                    .title("New Room")
                    .description("Room desc")
                    .visibility(Visibility.PUBLIC)
                    .build();

            CreateRoomResponseDto responseDto = new CreateRoomResponseDto(10L, "New Room", "Room desc", 1L, Visibility.PUBLIC);

            when(userAuthService.getAuthenticatedUser()).thenReturn(ownerUser);
            when(roomRepository.existsByUserId(1L)).thenReturn(false);
            when(roomMapper.createRoomRequestDtoToModel(requestDto)).thenReturn(mappedRoom);
            when(roomMapper.createRoomRequestToResponse(mappedRoom)).thenReturn(responseDto);

            CreateRoomResponseDto result = roomService.createRoom(requestDto);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Room");

            ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
            verify(roomRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(ownerUser);
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException when user already has a room")
        void createRoom_shouldThrowException_whenUserAlreadyHasRoom() {
            CreateRoomRequestDto requestDto = new CreateRoomRequestDto("Another Room", "Desc", Visibility.PUBLIC);

            when(userAuthService.getAuthenticatedUser()).thenReturn(ownerUser);
            when(roomRepository.existsByUserId(1L)).thenReturn(true);

            assertThatThrownBy(() -> roomService.createRoom(requestDto))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("User already has room");

            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate exception when roomRepository.save throws downstream")
        void createRoom_shouldPropagateException_whenSaveThrows() {
            CreateRoomRequestDto requestDto = new CreateRoomRequestDto("Room", "Desc", Visibility.PUBLIC);
            Room mappedRoom = new Room();

            when(userAuthService.getAuthenticatedUser()).thenReturn(ownerUser);
            when(roomRepository.existsByUserId(1L)).thenReturn(false);
            when(roomMapper.createRoomRequestDtoToModel(requestDto)).thenReturn(mappedRoom);
            when(roomRepository.save(mappedRoom)).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> roomService.createRoom(requestDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }

    @Nested
    @DisplayName("getRoomData() tests")
    class GetRoomDataTests {

        @Test
        @DisplayName("Should return room data when room is PUBLIC, regardless of whether user is owner")
        void getRoomData_shouldReturnData_whenRoomIsPublic() {
            sampleRoom.setVisibility(Visibility.PUBLIC);

            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(userAuthService.getAuthenticatedUser()).thenReturn(otherUser); // non-owner
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            RoomDataResponseDto result = roomService.getRoomData(10L);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Conference A");
            verify(roomMapper, times(1)).roomModelToDto(sampleRoom);
        }

        @Test
        @DisplayName("Should return room data when room is PRIVATE and user is the owner")
        void getRoomData_shouldReturnData_whenRoomIsPrivateAndUserIsOwner() {
            sampleRoom.setVisibility(Visibility.PRIVATE);

            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(userAuthService.getAuthenticatedUser()).thenReturn(ownerUser); // owner
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            RoomDataResponseDto result = roomService.getRoomData(10L);

            assertThat(result).isNotNull();
            verify(roomMapper, times(1)).roomModelToDto(sampleRoom);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when room is PRIVATE and user is not owner")
        void getRoomData_shouldThrowAccessDenied_whenRoomIsPrivateAndUserNotOwner() {
            sampleRoom.setVisibility(Visibility.PRIVATE);

            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            when(userAuthService.getAuthenticatedUser()).thenReturn(otherUser); // non-owner

            assertThatThrownBy(() -> roomService.getRoomData(10L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied for this room");

            verify(roomMapper, never()).roomModelToDto(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room does not exist")
        void getRoomData_shouldThrowNotFound_whenRoomDoesNotExist() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.getRoomData(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room not found with id: 999");
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void getRoomData_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                Room r = Room.builder().id(id).visibility(Visibility.PUBLIC).user(ownerUser).build();
                when(roomRepository.findById(id)).thenReturn(Optional.of(r));
                when(userAuthService.getAuthenticatedUser()).thenReturn(ownerUser);
                when(roomMapper.roomModelToDto(r)).thenReturn(new RoomDataResponseDto(id, "T", "D", 1L, Visibility.PUBLIC));

                RoomDataResponseDto result = roomService.getRoomData(id);

                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(id);
            }
        }
    }

    @Nested
    @DisplayName("updateRoom() tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should update all fields when title, description, and visibility are non-null")
        void updateRoom_shouldUpdateAllFields_whenAllNonNull() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto("New Title", "New Desc", Visibility.PRIVATE);

            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            RoomDataResponseDto result = roomService.updateRoom(10L, updateDto);

            assertThat(sampleRoom.getTitle()).isEqualTo("New Title");
            assertThat(sampleRoom.getDescription()).isEqualTo("New Desc");
            assertThat(sampleRoom.getVisibility()).isEqualTo(Visibility.PRIVATE);
            verify(roomRepository, times(1)).save(sampleRoom);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should update only title when description and visibility are null")
        void updateRoom_shouldUpdateOnlyTitle_whenOtherFieldsNull() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto("Only Title", null, null);

            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            roomService.updateRoom(10L, updateDto);

            assertThat(sampleRoom.getTitle()).isEqualTo("Only Title");
            assertThat(sampleRoom.getDescription()).isEqualTo("Discussion Room");
            assertThat(sampleRoom.getVisibility()).isEqualTo(Visibility.PUBLIC);
            verify(roomRepository, times(1)).save(sampleRoom);
        }

        @Test
        @DisplayName("Should update only description when title and visibility are null")
        void updateRoom_shouldUpdateOnlyDescription_whenOtherFieldsNull() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto(null, "Only Desc", null);

            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            roomService.updateRoom(10L, updateDto);

            assertThat(sampleRoom.getTitle()).isEqualTo("Conference A");
            assertThat(sampleRoom.getDescription()).isEqualTo("Only Desc");
            assertThat(sampleRoom.getVisibility()).isEqualTo(Visibility.PUBLIC);
            verify(roomRepository, times(1)).save(sampleRoom);
        }

        @Test
        @DisplayName("Should update only visibility when title and description are null")
        void updateRoom_shouldUpdateOnlyVisibility_whenOtherFieldsNull() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto(null, null, Visibility.PRIVATE);

            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            roomService.updateRoom(10L, updateDto);

            assertThat(sampleRoom.getTitle()).isEqualTo("Conference A");
            assertThat(sampleRoom.getDescription()).isEqualTo("Discussion Room");
            assertThat(sampleRoom.getVisibility()).isEqualTo(Visibility.PRIVATE);
            verify(roomRepository, times(1)).save(sampleRoom);
        }

        @Test
        @DisplayName("Should retain all fields when update DTO has all nulls")
        void updateRoom_shouldRetainFields_whenAllFieldsNull() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto(null, null, null);

            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomMapper.roomModelToDto(sampleRoom)).thenReturn(sampleRoomDataDto);

            roomService.updateRoom(10L, updateDto);

            assertThat(sampleRoom.getTitle()).isEqualTo("Conference A");
            assertThat(sampleRoom.getDescription()).isEqualTo("Discussion Room");
            assertThat(sampleRoom.getVisibility()).isEqualTo(Visibility.PUBLIC);
            verify(roomRepository, times(1)).save(sampleRoom);
        }

        @Test
        @DisplayName("Should propagate exception when roomRepository.save throws downstream")
        void updateRoom_shouldPropagateException_whenSaveThrows() {
            UpdateRoomRequestDto updateDto = new UpdateRoomRequestDto("Title", null, null);
            when(roomRepository.getRoomById(10L)).thenReturn(sampleRoom);
            when(roomRepository.save(sampleRoom)).thenThrow(new RuntimeException("Save error"));

            assertThatThrownBy(() -> roomService.updateRoom(10L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Save error");
        }
    }

    @Nested
    @DisplayName("deleteRoom() tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete room when room exists")
        void deleteRoom_shouldDelete_whenRoomExists() {
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));

            roomService.deleteRoom(10L);

            verify(roomRepository, times(1)).findById(10L);
            verify(roomRepository, times(1)).delete(sampleRoom);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when room does not exist")
        void deleteRoom_shouldThrowException_whenRoomNotFound() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.deleteRoom(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room Not Found");

            verify(roomRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle boundary room IDs (0L, -1L, Long.MAX_VALUE)")
        void deleteRoom_shouldHandleBoundaryRoomIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE};

            for (long id : boundaryIds) {
                Room r = Room.builder().id(id).build();
                when(roomRepository.findById(id)).thenReturn(Optional.of(r));

                roomService.deleteRoom(id);

                verify(roomRepository, times(1)).delete(r);
            }
        }

        @Test
        @DisplayName("Should propagate exception when delete throws downstream")
        void deleteRoom_shouldPropagateException_whenDeleteThrows() {
            when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
            org.mockito.Mockito.doThrow(new RuntimeException("Delete error"))
                    .when(roomRepository).delete(sampleRoom);

            assertThatThrownBy(() -> roomService.deleteRoom(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete error");
        }
    }
}
