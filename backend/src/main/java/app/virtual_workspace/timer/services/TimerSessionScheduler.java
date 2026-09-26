package app.virtual_workspace.timer.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.repositories.TimerSessionRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TimerSessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimerSessionScheduler.class);

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 30;

    private final TimerSessionRepository timerSessionRepository;
    private final TimerSessionService timerSessionService;

    @Scheduled(fixedRate = 30_000)
    public void finalizeTimedOutSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        List<TimerSession> timedOut = timerSessionRepository.findTimedOutRunningSessions(cutoff);

        if (!timedOut.isEmpty()) {
            log.info("Finalizing {} timed-out timer session(s)", timedOut.size());
            timedOut.forEach(timerSessionService::finalizeAbandoned);
        }
    }

}
