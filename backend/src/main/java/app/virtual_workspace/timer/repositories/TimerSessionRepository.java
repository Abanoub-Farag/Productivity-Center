package app.virtual_workspace.timer.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.virtual_workspace.timer.dtos.analytics.FocusBreakdownProjection;
import app.virtual_workspace.timer.dtos.analytics.RoomFocusProjection;
import app.virtual_workspace.timer.dtos.analytics.TimerSummaryProjection;
import app.virtual_workspace.timer.models.TimerSession;
import app.virtual_workspace.timer.models.enums.TimerStatus;

public interface TimerSessionRepository extends JpaRepository<TimerSession, Long> {

  Optional<TimerSession> findByUserIdAndStatus(Long userId, TimerStatus status);

  Optional<TimerSession> findByUserIdAndRoomIdAndStatus(Long userId, Long roomId, TimerStatus status);

  List<TimerSession> findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
      Long userId, Instant from, Instant to);

  List<TimerSession> findByUserIdAndRoomIdAndStartedAtBetweenOrderByStartedAtDesc(
      Long userId, Long roomId, Instant from, Instant to);

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
  List<TimerSession> findTimedOutRunningSessions(@Param("cutoff") Instant cutoff);

  // ── Analytics ─────────────────────────────────────────────────────────────

  @Query("""
      SELECT
        COUNT(ts)                              AS totalSessions,
        COALESCE(SUM(ts.duration), 0)         AS totalFocusSeconds,
        COALESCE(MAX(ts.duration), 0)         AS longestSessionSeconds,
        AVG(ts.duration)                      AS avgSessionSeconds
      FROM TimerSession ts
      WHERE ts.userId = :userId
        AND ts.status = 'DONE'
        AND ts.startedAt >= :from
        AND ts.startedAt  < :to
      """)
  TimerSummaryProjection findTimerSummary(
      @Param("userId") Long userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(value = """
      SELECT
        date_trunc('day', started_at AT TIME ZONE 'UTC')::date AS date,
        COALESCE(SUM(duration), 0)                             AS focusSeconds
      FROM timer_sessions
      WHERE user_id = :userId
        AND status  = 'DONE'
        AND started_at >= :from
        AND started_at  < :to
      GROUP BY 1
      ORDER BY 1
      """, nativeQuery = true)
  List<FocusBreakdownProjection> findDailyFocusBreakdown(
      @Param("userId") Long userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(value = """
      SELECT
        date_trunc('week', started_at AT TIME ZONE 'UTC')::date AS date,
        COALESCE(SUM(duration), 0)                              AS focusSeconds
      FROM timer_sessions
      WHERE user_id = :userId
        AND status  = 'DONE'
        AND started_at >= :from
        AND started_at  < :to
      GROUP BY 1
      ORDER BY 1
      """, nativeQuery = true)
  List<FocusBreakdownProjection> findWeeklyFocusBreakdown(
      @Param("userId") Long userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(value = """
      SELECT
        date_trunc('month', started_at AT TIME ZONE 'UTC')::date AS date,
        COALESCE(SUM(duration), 0)                               AS focusSeconds
      FROM timer_sessions
      WHERE user_id = :userId
        AND status  = 'DONE'
        AND started_at >= :from
        AND started_at  < :to
      GROUP BY 1
      ORDER BY 1
      """, nativeQuery = true)
  List<FocusBreakdownProjection> findMonthlyFocusBreakdown(
      @Param("userId") Long userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query("""
      SELECT ts.roomId    AS roomId,
             r.title     AS roomTitle,
             COALESCE(SUM(ts.duration), 0) AS focusSeconds,
             COUNT(ts)   AS sessionCount
      FROM TimerSession ts
      LEFT JOIN ts.room r
      WHERE ts.userId   = :userId
        AND ts.status   = 'DONE'
        AND ts.startedAt >= :from
        AND ts.startedAt  < :to
      GROUP BY ts.roomId, r.title
      ORDER BY focusSeconds DESC
      """)
  List<RoomFocusProjection> findPerRoomFocusBreakdown(
      @Param("userId") Long userId,
      @Param("from") Instant from,
      @Param("to") Instant to);

}
