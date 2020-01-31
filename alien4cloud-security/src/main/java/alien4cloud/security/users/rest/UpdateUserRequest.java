package alien4cloud.security.users.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String password;
    private String lastName;
    private String firstName;
    private String information;
    private String email;
    //private String[] roles;
}
