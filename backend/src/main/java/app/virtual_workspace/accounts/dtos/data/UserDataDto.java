package app.virtual_workspace.accounts.dtos.data;

import app.virtual_workspace.accounts.models.enums.Gender;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserDataDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Instant createdAt;
    private Instant updatedAt;
    private String bio;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Long roomsId;

}
