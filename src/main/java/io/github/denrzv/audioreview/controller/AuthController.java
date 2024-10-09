package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.model.User;
import io.github.denrzv.audioreview.repository.UserRepository;
import io.github.denrzv.audioreview.security.JwtUtils;
import io.github.denrzv.audioreview.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    AuthenticationManager authenticationManager;

    JwtUtils jwtUtils;

    BCryptPasswordEncoder passwordEncoder;

    UserRepository userRepository;

    /**
     * Login endpoint.
     *
     * @param loginRequest the login request containing username and password
     * @return JWT token upon successful authentication
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), 
                        loginRequest.getPassword())
        );

        // Set authentication in context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication.getName());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Build response
        return ResponseEntity.ok(new JwtResponse(jwt, 
                userDetails.getUsername(), 
                userDetails.getAuthorities()));
    }

    /**
     * (Optional) Registration endpoint.
     * Implement if you plan to allow user registration via API.
     *
     * @param signupRequest the signup request containing user details
     * @return success message or error
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Error: Username is already taken!");
        }

        // Create new user's account
        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .role(User.Role.USER) // Default role
                .active(true)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    // DTO for Signup Request
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SignupRequest {
        private String username;
        private String password;
    }

    // DTOs for requests and responses

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class JwtResponse {
        private String token;
        private String username;
        private Object authorities;
    }
}