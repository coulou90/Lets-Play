package lets_play.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiException {

    private int status;
    private String message;
    private LocalDateTime timestamp;
}