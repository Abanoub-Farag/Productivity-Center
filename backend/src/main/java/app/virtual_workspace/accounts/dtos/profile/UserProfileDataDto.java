package app.virtual_workspace.accounts.dtos.profile;

import java.time.Instant;
import java.time.LocalDate;

import app.virtual_workspace.accounts.models.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfileDataDto {
    private String bio;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String email;
    private String firstName;
    private String lastName;
    private Instant createdAt;
    private Instant updatedAt;
}
