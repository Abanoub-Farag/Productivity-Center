package app.virtual_workspace.rooms.dtos.room;

import app.virtual_workspace.rooms.models.enums.Visibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateRoomRequestDto {

    @Size(min = 2, max = 50, message = "Title must be between 2 to 50 characters")
    private String title;

    private String description;

    private Visibility visibility;

}
