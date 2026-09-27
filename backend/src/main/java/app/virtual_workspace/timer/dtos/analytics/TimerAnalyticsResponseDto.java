package app.virtual_workspace.timer.dtos.analytics;

import java.util.List;

public record TimerAnalyticsResponseDto(
        TimerSummaryDto summary,
        List<DailyFocusDto> dailyBreakdown,
        List<WeeklyFocusDto> weeklyBreakdown,
        List<MonthlyFocusDto> monthlyBreakdown,
        List<RoomFocusDto> perRoomBreakdown
) {}
