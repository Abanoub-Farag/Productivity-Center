package app.virtual_workspace.tasks.dtos;

import java.time.Instant;

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
public class TaskResponseDto {

    private Long id;
    private String title;
    private boolean isCompleted;
    private Instant createdAt;
    private Instant updatedAt;

}
