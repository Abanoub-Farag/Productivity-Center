package app.virtual_workspace.accounts.dtos.data;

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
public class UpdateUserDataDto {

    private String firstName;
    private String lastName;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;

}
