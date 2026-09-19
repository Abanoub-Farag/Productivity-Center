package app.virtual_workspace.tasks.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CreateTaskDto {

    @NotBlank(message = "Title is a required field")
    @Size(min = 1, max = 50, message = "Title must be between 1 to 50 characters")
    private String title;

    @Builder.Default
    private Boolean isCompleted = false;

}
