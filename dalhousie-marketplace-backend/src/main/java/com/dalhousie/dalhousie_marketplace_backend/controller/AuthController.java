package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ChangePasswordRequest;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.AuthService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import exception.InvalidPasswordException;
import exception.UserNotFoundException;
// import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthService authService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }


    private Optional<String> extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return Optional.of(authorizationHeader.substring(7));
        }
        return Optional.empty();
    }

    private Optional<String> validateTokenAndGetUsername(String token) {
        String username = jwtUtil.extractUsername(token);
        if (username != null && jwtUtil.validateToken(token, username)) {
            return Optional.of(username);
        }
        return Optional.empty();
    }

    private ResponseEntity<Map<String, String>> unauthorizedResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }



    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        if (isInvalidUser(user)) {
            return badRequest("Invalid user data. Email cannot be empty.");
        }

        try {
            String result = authService.registerUser(user);

            if (result == null) {
                return badRequest("An error occurred during registration: No response from registration service.");
            }

            switch (result) {
                case "User registered successfully":
                    authService.generateAndSendOtp(user);
                    return created("User registered successfully. Please verify your email using the OTP sent to your email.");
                case "Email is already in use":
                    return conflict("Email is already in use");
                case "Banner ID is already in use":
                    return conflict("Banner ID is already in use");
                default:
                    // Default case to handle any unexpected result
                    return badRequest("An error occurred during registration: Unexpected result - " + result);
            }
        } catch (Exception e) {
            return internalServerError("An error occurred during registration: " + e.getMessage());
        }
    }



    private boolean isInvalidUser(User user) {
        return user == null || user.getEmail() == null || user.getEmail().isEmpty();
    }

    private ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    private ResponseEntity<String> created(String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    private ResponseEntity<String> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    private ResponseEntity<String> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user) {
        Optional<User> authenticatedUser = authService.authenticate(user.getEmail());
        Map<String, String> response = new HashMap<>();
        if (authenticatedUser.isEmpty()) {
            response.put("message", "Account does not exist. Please sign up.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        User loggedInUser = authenticatedUser.get();
        if (!passwordEncoder.matches(user.getPasswordHash(), loggedInUser.getPasswordHash())) {
            response.put("message", "Please enter valid credentials");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if (!loggedInUser.isVerified()) {
            response.put("message", "User verification is pending. Please try again.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        String token = jwtUtil.generateToken(loggedInUser);
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("userId", String.valueOf(loggedInUser.getUserId()));
        response.put("username", String.valueOf(loggedInUser.getUsername()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId,
                                         @RequestHeader("Authorization") String authorizationHeader) {
        Optional<String> tokenOpt = extractToken(authorizationHeader);
        if (tokenOpt.isEmpty()) {
            return unauthorizedResponse("Missing or invalid authorization token.");
        }
        Optional<String> usernameOpt = validateTokenAndGetUsername(tokenOpt.get());
        if (usernameOpt.isEmpty()) {
            return unauthorizedResponse("Invalid or expired token.");
        }
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found."));
        }
        User user = userOptional.get();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("fullName", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("bannerId", user.getbannerId());
        userData.put("verificationStatus", user.getVerificationStatus());
        return ResponseEntity.ok(userData);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");
        String otp = requestData.get("otp");

        if (isInvalidOtpRequest(email, otp)) {
            return badRequest("Email and OTP are required.");
        }

        try {
            User user = getUserByEmail(email);

            if (isOtpExpired(user)) {
                return badRequest("OTP expired. Please request a new one.");
            }

            if (isOtpInvalid(user, otp)) {
                return badRequest("Invalid OTP. Please try again.");
            }

            verifyUser(user);

            return ResponseEntity.status(HttpStatus.OK).body("Email verified successfully.");
        } catch (Exception e) {
            return internalServerError("An error occurred during OTP verification: " + e.getMessage());
        }
    }

    private boolean isInvalidOtpRequest(String email, String otp) {
        return email == null || email.isEmpty() || otp == null || otp.isEmpty();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isOtpExpired(User user) {
        return user.getOtp() == null || user.getOtpExpiry().isBefore(LocalDateTime.now());
    }

    private boolean isOtpInvalid(User user, String otp) {
        return !user.getOtp().equals(otp);
    }

    private void verifyUser(User user) {
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setVerificationStatus("verified");
        userRepository.save(user);
    }



    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        authService.sendResetPasswordLink(email);
        return ResponseEntity.ok("Reset password link sent to email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token,
                                                @RequestParam("newPassword") String newPassword) {
        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password reset successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while resetting password.");
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteUserAccount(@RequestHeader("Authorization") String authorizationHeader) {
        Optional<String> tokenOpt = extractToken(authorizationHeader);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization token.");
        }
        Optional<String> usernameOpt = validateTokenAndGetUsername(tokenOpt.get());
        if (usernameOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token.");
        }
        String username = usernameOpt.get();
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User with email " + username + " not found.");
        }
        userRepository.delete(userOptional.get());
        return ResponseEntity.ok("User deleted permanently. You can re-register anytime.");
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request,
                                                 @RequestHeader("Authorization") String authorizationHeader) {
        Optional<String> tokenOpt = extractToken(authorizationHeader);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization token.");
        }
        Long userId;
        try {
            userId = jwtUtil.extractUserId(tokenOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or missing token");
        }
        if (request.getOldPassword() == null || request.getNewPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required fields");
        }
        try {
            boolean isUpdated = authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            if (isUpdated) {
                return ResponseEntity.ok("Password updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Old password is incorrect");
            }
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (InvalidPasswordException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid old password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while changing password");
        }
    }
}






