package alien4cloud.security.users.rest;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String lastName;
    private String firstName;
    private String information;
    private String email;
    private String[] roles;
}