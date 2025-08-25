package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.AuthService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        authService = new AuthService(jwtUtil);

        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "mailSender", mailSender);

        mockUser = new User();
        mockUser.setEmail("test@dal.ca");
        mockUser.setPasswordHash("password");
        mockUser.setIsVerified(false);
        mockUser.setbannerId("B12345678");
    }

    @Test
    void registerUser_shouldReturnSuccess_whenNewUser() {
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.empty());
        when(userRepository.findByBannerId("B12345678")).thenReturn(Optional.empty());

        String result = authService.registerUser(mockUser);

        verify(userRepository).save(any(User.class));
        assertEquals("User registered successfully", result);
    }

    @Test
    void registerUser_shouldReturnBannerIdError_whenBannerIdExists() {
        User conflictingUser = new User();
        conflictingUser.setEmail("another@dal.ca"); // Different email than mockUser
        conflictingUser.setbannerId("B12345678");

        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.empty());
        when(userRepository.findByBannerId("B12345678")).thenReturn(Optional.of(conflictingUser)); // not mockUser

        mockUser.setEmail("test@dal.ca");
        mockUser.setbannerId("B12345678");

        String result = authService.registerUser(mockUser);

        assertEquals("Banner ID is already in use.", result);
    }

    @Test
    void registerUser_shouldReturnEmailExistsError_whenAlreadyVerified() {
        // Existing verified user
        User existing = new User();
        existing.setEmail("test@dal.ca");
        existing.setIsVerified(true);
        existing.setUserId(1L);
        existing.setbannerId("B12345678");

        // New user trying to register
        User newUser = new User();
        newUser.setEmail("test@dal.ca");
        newUser.setPasswordHash("pass");
        newUser.setbannerId("B12345678");

        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.of(existing));

        String result = authService.registerUser(newUser);

        assertEquals("Email is already in use. Please use a different email or log in.", result);
    }


    @Test
    void registerUser_shouldUpdateExistingUser_whenUnverified() {
        User existing = new User();
        existing.setEmail("test@dal.ca");
        existing.setIsVerified(false);
        existing.setUserId(100L);
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.of(existing));
        when(userRepository.findByBannerId("B12345678")).thenReturn(Optional.of(existing));

        mockUser.setUserId(100L);

        String result = authService.registerUser(mockUser);

        verify(userRepository).save(any(User.class));
        assertEquals("User registered successfully", result);
    }

    @Test
    void authenticate_shouldReturnUserIfFound() {
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.of(mockUser));
        Optional<User> result = authService.authenticate("test@dal.ca");
        assertTrue(result.isPresent());
    }

    @Test
    void authenticate_shouldReturnEmptyIfNotFound() {
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.empty());
        Optional<User> result = authService.authenticate("test@dal.ca");
        assertTrue(result.isEmpty());
    }

    @Test
    void sendEmail_shouldSendSuccessfully() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> authService.sendEmail("user@dal.ca", "Subject", "Text"));
    }

    @Test
    void generateAndSendOtp_shouldSaveOtpAndSendEmail() {
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        authService.generateAndSendOtp(mockUser);

        verify(userRepository).save(any(User.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertNotNull(mockUser.getOtp());
    }

    @Test
    void sendResetPasswordLink_shouldSendEmail() {
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateResetToken("test@dal.ca")).thenReturn("test-token");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        try (MockedStatic<ServletUriComponentsBuilder> mockBuilder = mockStatic(ServletUriComponentsBuilder.class)) {
            ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class);
            mockBuilder.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);
            when(builder.toUriString()).thenReturn("http://localhost:3000");

            authService.sendResetPasswordLink("test@dal.ca");

            verify(mailSender).send(any(SimpleMailMessage.class));
        }
    }


    @Test
    void sendResetPasswordLink_shouldThrowIfUserNotFound() {
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.sendResetPasswordLink("test@dal.ca"));
    }

    @Test
    void resetPassword_shouldUpdatePassword() {
        when(jwtUtil.validateResetToken("token")).thenReturn("test@dal.ca");
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.of(mockUser));

        authService.resetPassword("token", "newpass");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void resetPassword_shouldThrowIfTokenInvalid() {
        when(jwtUtil.validateResetToken("token")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.resetPassword("token", "newpass"));
    }

    @Test
    void resetPassword_shouldThrowIfUserNotFound() {
        when(jwtUtil.validateResetToken("token")).thenReturn("test@dal.ca");
        when(userRepository.findByEmail("test@dal.ca")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.resetPassword("token", "newpass"));
    }

    @Test
    void changePassword_shouldUpdatePasswordIfOldMatches() {
        String oldHash = new BCryptPasswordEncoder().encode("oldpass");
        mockUser.setPasswordHash(oldHash);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        boolean result = authService.changePassword(1L, "oldpass", "newpass");

        assertTrue(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_shouldReturnFalseIfOldDoesNotMatch() {
        mockUser.setPasswordHash(new BCryptPasswordEncoder().encode("correctpass"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        boolean result = authService.changePassword(1L, "wrongpass", "newpass");

        assertFalse(result);
    }

    @Test
    void changePassword_shouldReturnFalseIfUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(authService.changePassword(1L, "old", "new"));
    }

    @Test
    void getAuthorities_shouldReturnRoleUser() {
        var authorities = authService.getAuthorities("user");
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
