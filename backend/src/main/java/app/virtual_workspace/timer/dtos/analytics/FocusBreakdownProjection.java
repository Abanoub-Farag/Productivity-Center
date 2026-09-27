package app.virtual_workspace.timer.dtos.analytics;

import java.sql.Date;

/** Used for native SQL date_trunc results (daily, weekly, monthly breakdowns). */
public interface FocusBreakdownProjection {
    Date getDate();
    Long getFocusSeconds();
}
