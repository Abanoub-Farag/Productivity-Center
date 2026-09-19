package app.virtual_workspace.rooms.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.rooms.dtos.room.AllRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomRequestDto;
import app.virtual_workspace.rooms.dtos.room.CreateRoomResponseDto;
import app.virtual_workspace.rooms.dtos.room.RoomDataResponseDto;
import app.virtual_workspace.rooms.dtos.room.UpdateRoomRequestDto;
import app.virtual_workspace.rooms.services.RoomService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

        private final RoomService roomService;

        @GetMapping("")
        public ResponseEntity<ApiResponse<Slice<AllRoomResponseDto>>> getAllRooms(
                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                Slice<AllRoomResponseDto> rooms = roomService.getAllRooms(pageable);

                ApiResponse<Slice<AllRoomResponseDto>> response = ApiResponse.<Slice<AllRoomResponseDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Rooms retrieved successfully")
                                .data(rooms)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PostMapping("")
        public ResponseEntity<ApiResponse<CreateRoomResponseDto>> createRoom(
                        @Valid @RequestBody CreateRoomRequestDto createRoomRequestDto,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                CreateRoomResponseDto createdRoom = roomService.createRoom(userPrincipal.getId(), createRoomRequestDto);

                ApiResponse<CreateRoomResponseDto> response = ApiResponse.<CreateRoomResponseDto>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Room created successfully")
                                .data(createdRoom)
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<RoomDataResponseDto>> room(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                RoomDataResponseDto roomData = roomService.getRoomData(userPrincipal.getId(), id);

                ApiResponse<RoomDataResponseDto> response = ApiResponse.<RoomDataResponseDto>builder()
                                .status(HttpStatus.OK.value())
                                .message("Room data retrieved successfully")
                                .data(roomData)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{roomId}")
        public ResponseEntity<ApiResponse<RoomDataResponseDto>> updateRoom(
                        @PathVariable Long roomId,
                        @Valid @RequestBody UpdateRoomRequestDto updateRoomRequestDto,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                RoomDataResponseDto updatedRoom = roomService.updateRoom(userPrincipal.getId(), roomId,
                                updateRoomRequestDto);

                ApiResponse<RoomDataResponseDto> response = ApiResponse.<RoomDataResponseDto>builder()
                                .status(HttpStatus.OK.value())
                                .message("Room updated successfully")
                                .data(updatedRoom)
                                .build();

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{roomId}")
        public ResponseEntity<ApiResponse<Void>> deleteRoom(
                        @PathVariable Long roomId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                roomService.deleteRoom(userPrincipal.getId(), roomId);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Room deleted successfully")
                                .build();

                return ResponseEntity.ok(response);
        }
}