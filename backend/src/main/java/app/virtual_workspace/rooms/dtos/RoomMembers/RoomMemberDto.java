package app.virtual_workspace.rooms.dtos.RoomMembers;

import java.time.LocalDate;

import app.virtual_workspace.accounts.models.enums.Gender;
import app.virtual_workspace.rooms.models.enums.Status;
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
public class RoomMemberDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String bio;
    private Status status;
    private Gender gender;
    private LocalDate dateOfBirth;

}
