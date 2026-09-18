package app.virtual_workspace.accounts.dtos.profile;

import java.time.LocalDate;

import app.virtual_workspace.accounts.models.enums.Gender;
import jakarta.validation.constraints.Past;
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
public class UpdateUserProfileDto {

    @Size(max = 300, message = "Bio must not exceed 300 characters")
    private String bio;

    private Gender gender;

    @Past
    private LocalDate dateOfBirth;

    @Size(min = 3, max = 20, message = "First name must be between 3 and 20 characters")
    private String firstName;

    @Size(min = 3, max = 20, message = "Last name must be between 3 and 20 characters")
    private String lastName;
}
