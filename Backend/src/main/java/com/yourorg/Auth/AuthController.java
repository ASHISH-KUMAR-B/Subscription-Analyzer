package com.yourorg.Auth;

import com.yourorg.Auth.dto.AuthResponse;
import com.yourorg.Auth.dto.LoginRequest;
import com.yourorg.Auth.dto.RegisterRequest;
import com.yourorg.Users.User;
import com.yourorg.Users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username, email and password are required");
        }

        if (userRepository.findByEmail(request.email().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.findByUserName(request.username().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User saved = userRepository.save(new User(
                request.username().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password())
        ));
        saved.setLastLoginAt(Instant.now());
        userRepository.save(saved);

        return toAuthResponse(saved);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password are required");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassWordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtTokenService.issueToken(user.getId(), user.getUserName(), user.getEmail());
        return new AuthResponse(token,
                new AuthResponse.UserView(user.getId(), user.getUserName(), user.getEmail()));
    }
}
