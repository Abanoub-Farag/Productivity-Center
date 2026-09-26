package app.virtual_workspace.rooms.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.rooms.dtos.RoomMembers.HeartbeatRequestDto;
import app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto;
import app.virtual_workspace.rooms.services.RoomMembersService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomMembersController {

    private final RoomMembersService roomMembersService;

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<Void>> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        roomMembersService.joinRoom(userPrincipal.getId(), roomId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Joined the room successfully")
                .build();

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/{roomId}/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartBeat(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) HeartbeatRequestDto body) {

        boolean timerActive = body != null && Boolean.TRUE.equals(body.getTimerActive());
        roomMembersService.heartBeat(userPrincipal.getId(), roomId, timerActive);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Heartbeat done")
                .build();

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<List<RoomMemberDto>>> getRoomMembers(@PathVariable Long roomId) {
        List<RoomMemberDto> roomMemberDtos = roomMembersService.getRoomMembers(roomId);

        ApiResponse<List<RoomMemberDto>> response = ApiResponse.<List<RoomMemberDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Room members retrieved successfully")
                .data(roomMemberDtos)
                .build();

        return ResponseEntity.ok(response);
    }

}