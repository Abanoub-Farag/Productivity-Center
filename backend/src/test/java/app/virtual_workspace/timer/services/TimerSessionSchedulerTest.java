package app.virtual_workspace.timer.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.models.enums.TimerStatus;
import app.virtual_workspace.timer.repositories.TimerSessionRepository;

@ExtendWith(MockitoExtension.class)
public class TimerSessionSchedulerTest {

    @Mock private TimerSessionRepository timerSessionRepository;
    @Mock private TimerSessionService timerSessionService;

    @InjectMocks
    private TimerSessionScheduler timerSessionScheduler;

    @Nested
    @DisplayName("finalizeTimedOutSessions() tests")
    class FinalizeTimedOutSessionsTests {

        @Test
        @DisplayName("Should call finalizeAbandoned for each timed-out session")
        void finalizeTimedOutSessions_shouldFinalizeAllTimedOutSessions() {
            TimerSession s1 = TimerSession.builder().id(1L).status(TimerStatus.RUNNING).build();
            TimerSession s2 = TimerSession.builder().id(2L).status(TimerStatus.RUNNING).build();

            when(timerSessionRepository.findTimedOutRunningSessions(any(Instant.class)))
                    .thenReturn(List.of(s1, s2));

            timerSessionScheduler.finalizeTimedOutSessions();

            verify(timerSessionService, times(1)).finalizeAbandoned(s1);
            verify(timerSessionService, times(1)).finalizeAbandoned(s2);
        }

        @Test
        @DisplayName("Should not call finalizeAbandoned when no timed-out sessions exist")
        void finalizeTimedOutSessions_shouldDoNothing_whenNoTimedOutSessions() {
            when(timerSessionRepository.findTimedOutRunningSessions(any(Instant.class)))
                    .thenReturn(List.of());

            timerSessionScheduler.finalizeTimedOutSessions();

            verify(timerSessionService, never()).finalizeAbandoned(any());
        }

        @Test
        @DisplayName("Should pass a cutoff Instant that is ~30 seconds before now")
        void finalizeTimedOutSessions_shouldPassCutoffApproximately30sInPast() {
            when(timerSessionRepository.findTimedOutRunningSessions(any(Instant.class)))
                    .thenReturn(List.of());

            Instant before = Instant.now().minusSeconds(31);
            timerSessionScheduler.finalizeTimedOutSessions();
            Instant after = Instant.now().minusSeconds(29);

            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(timerSessionRepository).findTimedOutRunningSessions(cutoffCaptor.capture());

            Instant cutoff = cutoffCaptor.getValue();
            assertThat(cutoff).isBetween(before, after);
        }

        @Test
        @DisplayName("Should handle a single timed-out session correctly")
        void finalizeTimedOutSessions_shouldHandleSingleSession() {
            TimerSession session = TimerSession.builder().id(99L).status(TimerStatus.RUNNING).build();

            when(timerSessionRepository.findTimedOutRunningSessions(any(Instant.class)))
                    .thenReturn(List.of(session));

            timerSessionScheduler.finalizeTimedOutSessions();

            verify(timerSessionService, times(1)).finalizeAbandoned(session);
        }
    }
}
