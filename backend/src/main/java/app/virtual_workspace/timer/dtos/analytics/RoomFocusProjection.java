package app.virtual_workspace.timer.dtos.analytics;

public interface RoomFocusProjection {
    Long getRoomId();
    String getRoomTitle();
    Long getFocusSeconds();
    Long getSessionCount();
}
