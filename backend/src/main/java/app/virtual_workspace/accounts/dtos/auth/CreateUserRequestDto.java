package app.virtual_workspace.accounts.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CreateUserRequestDto {

    @NotBlank(message = "Email field is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 3, max = 20)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 20)
    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,64}$", message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;
}
