package app.virtual_workspace.rooms.dtos.RoomMembers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HeartbeatRequestDto {

    private Boolean timerActive;

}
