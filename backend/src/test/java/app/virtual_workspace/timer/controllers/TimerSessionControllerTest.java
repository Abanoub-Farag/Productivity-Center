package app.virtual_workspace.timer.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.shared.dtos.ApiResponse;
import app.virtual_workspace.timer.dtos.TimerSessionResponseDto;
import app.virtual_workspace.timer.models.enums.TimerStatus;
import app.virtual_workspace.timer.services.TimerSessionService;

@ExtendWith(MockitoExtension.class)
public class TimerSessionControllerTest {

    @Mock
    private TimerSessionService timerSessionService;

    @InjectMocks
    private TimerSessionController timerSessionController;

    private UserPrincipal userPrincipal;
    private TimerSessionResponseDto runningDto;
    private TimerSessionResponseDto doneDto;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("user@example.com")
                .password("password")
                .active(true)
                .role(Role.ROLE_USER)
                .build();

        runningDto = TimerSessionResponseDto.builder()
                .id(100L).userId(1L).roomId(10L)
                .startedAt(Instant.now())
                .status(TimerStatus.RUNNING)
                .build();

        doneDto = TimerSessionResponseDto.builder()
                .id(100L).userId(1L).roomId(10L)
                .startedAt(Instant.now().minusSeconds(300))
                .endedAt(Instant.now())
                .duration(300L)
                .status(TimerStatus.DONE)
                .build();
    }

    // ── startTimer ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startTimer() tests")
    class StartTimerTests {

        @Test
        @DisplayName("Should return 201 CREATED with running session DTO")
        void startTimer_shouldReturn201_whenSuccessful() {
            when(timerSessionService.startTimer(1L, 10L)).thenReturn(runningDto);

            ResponseEntity<ApiResponse<TimerSessionResponseDto>> response =
                    timerSessionController.startTimer(10L, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Timer session started");
            assertThat(response.getBody().getData()).isEqualTo(runningDto);

            verify(timerSessionService).startTimer(1L, 10L);
        }

        @Test
        @DisplayName("Should propagate ResourceAlreadyExistsException when session is already running")
        void startTimer_shouldPropagateException_whenAlreadyRunning() {
            doThrow(new ResourceAlreadyExistsException("A timer session is already running"))
                    .when(timerSessionService).startTimer(1L, 10L);

            assertThatThrownBy(() -> timerSessionController.startTimer(10L, userPrincipal))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("A timer session is already running");
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when user is not a room member")
        void startTimer_shouldPropagateException_whenNotMember() {
            doThrow(new ResourceNotFoundException("Room membership not found"))
                    .when(timerSessionService).startTimer(1L, 10L);

            assertThatThrownBy(() -> timerSessionController.startTimer(10L, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room membership not found");
        }
    }

    // ── completeTimer ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeTimer() tests")
    class CompleteTimerTests {

        @Test
        @DisplayName("Should return 200 OK with completed session DTO")
        void completeTimer_shouldReturn200_whenSuccessful() {
            when(timerSessionService.completeTimer(100L, 1L)).thenReturn(doneDto);

            ResponseEntity<ApiResponse<TimerSessionResponseDto>> response =
                    timerSessionController.completeTimer(100L, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Timer session completed");
            assertThat(response.getBody().getData()).isEqualTo(doneDto);
            assertThat(response.getBody().getData().getStatus()).isEqualTo(TimerStatus.DONE);

            verify(timerSessionService).completeTimer(100L, 1L);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when session is not found")
        void completeTimer_shouldPropagateException_whenNotFound() {
            doThrow(new ResourceNotFoundException("Timer session not found"))
                    .when(timerSessionService).completeTimer(999L, 1L);

            assertThatThrownBy(() -> timerSessionController.completeTimer(999L, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");
        }

        @Test
        @DisplayName("Should propagate ResourceAlreadyExistsException when session is already DONE")
        void completeTimer_shouldPropagateException_whenAlreadyDone() {
            doThrow(new ResourceAlreadyExistsException("Timer session is already completed"))
                    .when(timerSessionService).completeTimer(100L, 1L);

            assertThatThrownBy(() -> timerSessionController.completeTimer(100L, userPrincipal))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("Timer session is already completed");
        }
    }

    // ── getSession ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSession() tests")
    class GetSessionTests {

        @Test
        @DisplayName("Should return 200 OK with session DTO")
        void getSession_shouldReturn200_whenFound() {
            when(timerSessionService.getSession(100L, 1L)).thenReturn(runningDto);

            ResponseEntity<ApiResponse<TimerSessionResponseDto>> response =
                    timerSessionController.getSession(100L, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Timer session retrieved");
            assertThat(response.getBody().getData()).isEqualTo(runningDto);

            verify(timerSessionService).getSession(100L, 1L);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when session is not found")
        void getSession_shouldPropagateException_whenNotFound() {
            doThrow(new ResourceNotFoundException("Timer session not found"))
                    .when(timerSessionService).getSession(999L, 1L);

            assertThatThrownBy(() -> timerSessionController.getSession(999L, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Timer session not found");
        }
    }

    // ── getRoomTimerSessions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoomTimerSessions() tests")
    class GetRoomTimerSessionsTests {

        @Test
        @DisplayName("Should return 200 OK with list of room sessions")
        void getRoomTimerSessions_shouldReturn200_withSessions() {
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 27);

            Instant expectedFrom = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant expectedTo = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

            when(timerSessionService.getUserSessionsInRoom(eq(1L), eq(10L), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(doneDto));

            ResponseEntity<ApiResponse<List<TimerSessionResponseDto>>> response =
                    timerSessionController.getRoomTimerSessions(10L, userPrincipal, startDate, endDate);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Room timer sessions retrieved");
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().getFirst()).isEqualTo(doneDto);
        }

        @Test
        @DisplayName("Should pass correct UTC Instant boundaries derived from LocalDate params")
        void getRoomTimerSessions_shouldPassCorrectInstantBoundaries() {
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 27);

            Instant expectedFrom = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant expectedTo = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

            when(timerSessionService.getUserSessionsInRoom(anyLong(), anyLong(), any(), any()))
                    .thenReturn(List.of());

            timerSessionController.getRoomTimerSessions(10L, userPrincipal, startDate, endDate);

            ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

            verify(timerSessionService).getUserSessionsInRoom(eq(1L), eq(10L), fromCaptor.capture(), toCaptor.capture());

            assertThat(fromCaptor.getValue()).isEqualTo(expectedFrom);
            assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
        }

        @Test
        @DisplayName("Should return empty list when no sessions in range")
        void getRoomTimerSessions_shouldReturnEmpty_whenNoSessions() {
            when(timerSessionService.getUserSessionsInRoom(anyLong(), anyLong(), any(), any()))
                    .thenReturn(List.of());

            ResponseEntity<ApiResponse<List<TimerSessionResponseDto>>> response =
                    timerSessionController.getRoomTimerSessions(10L, userPrincipal,
                            LocalDate.now(), LocalDate.now());

            assertThat(response.getBody().getData()).isEmpty();
        }
    }

    // ── getUserTimerSessions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserTimerSessions() tests")
    class GetUserTimerSessionsTests {

        @Test
        @DisplayName("Should return 200 OK with list of user sessions")
        void getUserTimerSessions_shouldReturn200_withSessions() {
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 27);

            when(timerSessionService.getUserSessions(eq(1L), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(doneDto));

            ResponseEntity<ApiResponse<List<TimerSessionResponseDto>>> response =
                    timerSessionController.getUserTimerSessions(userPrincipal, startDate, endDate);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getMessage()).isEqualTo("Timer sessions retrieved");
            assertThat(response.getBody().getData()).hasSize(1);
        }

        @Test
        @DisplayName("Should pass correct UTC Instant boundaries derived from LocalDate params")
        void getUserTimerSessions_shouldPassCorrectInstantBoundaries() {
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);

            Instant expectedFrom = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant expectedTo = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

            when(timerSessionService.getUserSessions(anyLong(), any(), any())).thenReturn(List.of());

            timerSessionController.getUserTimerSessions(userPrincipal, startDate, endDate);

            ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

            verify(timerSessionService).getUserSessions(eq(1L), fromCaptor.capture(), toCaptor.capture());

            assertThat(fromCaptor.getValue()).isEqualTo(expectedFrom);
            assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
        }

        @Test
        @DisplayName("Should propagate exception when service throws")
        void getUserTimerSessions_shouldPropagateException() {
            doThrow(new RuntimeException("DB error"))
                    .when(timerSessionService).getUserSessions(anyLong(), any(), any());

            assertThatThrownBy(() ->
                    timerSessionController.getUserTimerSessions(userPrincipal, LocalDate.now(), LocalDate.now()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }
}
