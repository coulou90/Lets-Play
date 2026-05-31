package lets_play.dto;

import lets_play.model.User;
import lombok.Data;

@Data
public class UserResponse {

    private String id;
    private String name;
    private String email;
    private String role;

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }
}