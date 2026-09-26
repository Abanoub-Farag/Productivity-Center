package app.virtual_workspace.timer.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.models.enums.TimerStatus;

public interface TimerSessionRepository extends JpaRepository<TimerSession, Long> {

    Optional<TimerSession> findByUserIdAndStatus(Long userId, TimerStatus status);

    Optional<TimerSession> findByUserIdAndRoomIdAndStatus(Long userId, Long roomId, TimerStatus status);

    List<TimerSession> findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    List<TimerSession> findByUserIdAndRoomIdAndStartedAtBetweenOrderByStartedAtDesc(
            Long userId, Long roomId, LocalDateTime from, LocalDateTime to);

    /**
     * Finds RUNNING sessions whose associated room_member heartbeat has timed out.
     * Uses lastActiveAt of the RoomMembers record as the staleness signal.
     */
    @Query("""
            SELECT ts FROM TimerSession ts
            JOIN RoomMembers rm ON rm.user.id = ts.user.id AND rm.room.id = ts.room.id
            WHERE ts.status = 'RUNNING'
              AND rm.timerActive = true
              AND rm.lastActiveAt < :cutoff
            """)
    List<TimerSession> findTimedOutRunningSessions(@Param("cutoff") LocalDateTime cutoff);

}
