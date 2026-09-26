package app.virtual_workspace.timer.dtos;

import java.time.LocalDateTime;

import app.virtual_workspace.timer.models.enums.TimerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TimerSessionResponseDto {

    private Long id;
    private Long userId;
    private Long roomId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long duration;
    private TimerStatus status;

}
