package app.virtual_workspace.timer.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.shared.dtos.ApiResponse;
import app.virtual_workspace.timer.dtos.TimerSessionResponseDto;
import app.virtual_workspace.timer.services.TimerSessionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TimerSessionController {

    private final TimerSessionService timerSessionService;

    // ── POST /api/v1/rooms/{roomId}/timer-sessions ─────────────────────────────

    @PostMapping("/api/v1/rooms/{roomId}/timer-sessions")
    public ResponseEntity<ApiResponse<TimerSessionResponseDto>> startTimer(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        TimerSessionResponseDto dto = timerSessionService.startTimer(userPrincipal.getId(), roomId);

        ApiResponse<TimerSessionResponseDto> response = ApiResponse.<TimerSessionResponseDto>builder()
                .status(HttpStatus.CREATED.value())
                .message("Timer session started")
                .data(dto)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── PATCH /api/v1/timer-sessions/{sessionId} ───────────────────────────────

    @PatchMapping("/api/v1/timer-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<TimerSessionResponseDto>> completeTimer(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        TimerSessionResponseDto dto = timerSessionService.completeTimer(sessionId, userPrincipal.getId());

        ApiResponse<TimerSessionResponseDto> response = ApiResponse.<TimerSessionResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("Timer session completed")
                .data(dto)
                .build();

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/timer-sessions/{sessionId} ─────────────────────────────────

    @GetMapping("/api/v1/timer-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<TimerSessionResponseDto>> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        TimerSessionResponseDto dto = timerSessionService.getSession(sessionId, userPrincipal.getId());

        ApiResponse<TimerSessionResponseDto> response = ApiResponse.<TimerSessionResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("Timer session retrieved")
                .data(dto)
                .build();

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/rooms/{roomId}/timer-sessions ──────────────────────────────

    @GetMapping("/api/v1/rooms/{roomId}/timer-sessions")
    public ResponseEntity<ApiResponse<List<TimerSessionResponseDto>>> getRoomTimerSessions(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);

        List<TimerSessionResponseDto> dtos = timerSessionService.getUserSessionsInRoom(
                userPrincipal.getId(), roomId, from, to);

        ApiResponse<List<TimerSessionResponseDto>> response = ApiResponse.<List<TimerSessionResponseDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Room timer sessions retrieved")
                .data(dtos)
                .build();

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/timer-sessions?startDate=&endDate= ─────────────────────────

    @GetMapping("/api/v1/timer-sessions")
    public ResponseEntity<ApiResponse<List<TimerSessionResponseDto>>> getUserTimerSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);

        List<TimerSessionResponseDto> dtos = timerSessionService.getUserSessions(
                userPrincipal.getId(), from, to);

        ApiResponse<List<TimerSessionResponseDto>> response = ApiResponse.<List<TimerSessionResponseDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Timer sessions retrieved")
                .data(dtos)
                .build();

        return ResponseEntity.ok(response);
    }

}
