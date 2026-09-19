package app.virtual_workspace.tasks.dtos;

import jakarta.validation.constraints.Size;
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
public class UpdateTaskDto {

    @Size(min = 1, max = 50, message = "Title must be between 1 to 50 characters")
    private String title;

    private Boolean isCompleted;

}
