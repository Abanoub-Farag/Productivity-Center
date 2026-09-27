package app.virtual_workspace.analytics.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.analytics.dtos.DashboardResponseDto;
import app.virtual_workspace.analytics.services.DashboardService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ── GET /api/v1/dashboard?startDate=&endDate= ──────────────────────────────

    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Instant from = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(); // exclusive

        DashboardResponseDto data = dashboardService.getDashboard(userPrincipal.getId(), from, to);

        ApiResponse<DashboardResponseDto> response = ApiResponse.<DashboardResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("Dashboard analytics retrieved")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
