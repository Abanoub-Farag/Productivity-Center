package app.virtual_workspace.timer.dtos.analytics;

public record RoomFocusDto(Long roomId, String roomName, Long focusSeconds, Long sessionCount) {}
