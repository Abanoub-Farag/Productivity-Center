package app.virtual_workspace.exceptions;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private Instant timeStamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<ValidationError> errors;

    @Getter
    @Builder
    public static class ValidationError{
        private String field;
        private String message;
    }

}
