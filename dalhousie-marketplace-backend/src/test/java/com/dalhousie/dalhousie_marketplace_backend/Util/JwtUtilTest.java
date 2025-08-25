package com.dalhousie.dalhousie_marketplace_backend.Util;

import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserRepository userRepository;
    private final String secretKey = "mysupersecretkeymysupersecretkeymysupersecretkey"; // ≥32 bytes

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository = mock(UserRepository.class);
        jwtUtil = new JwtUtil(userRepository);

        // Inject secret key using reflection (since it's @Value)
        Field secretKeyField = JwtUtil.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtUtil, secretKey);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGenerateTokenAndExtractUsername() {
        String token = jwtUtil.generateToken(testUser);
        String extractedUsername = jwtUtil.extractUsername("Bearer " + token);

        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    void testGenerateTokenAndExtractUserId() {
        String token = jwtUtil.generateToken(testUser);
        Long extractedUserId = jwtUtil.extractUserId("Bearer " + token);

        assertEquals(1L, extractedUserId);
    }

    @Test
    void testValidateToken_Valid() {
        String token = jwtUtil.generateToken(testUser);
        boolean valid = jwtUtil.validateToken("Bearer " + token, "test@example.com");

        assertTrue(valid);
    }

    @Test
    void testValidateUserAccess_Valid() {
        String token = jwtUtil.generateToken(testUser);
        boolean valid = jwtUtil.validateUserAccess("Bearer " + token, 1L);

        assertTrue(valid);
    }

    @Test
    void testIsTokenExpired_False() {
        String token = jwtUtil.generateToken(testUser);
        boolean expired = jwtUtil.validateToken("Bearer " + token, "test@example.com");

        assertTrue(expired); // single assert already in validateToken test
    }

    @Test
    void testGenerateAndValidateResetToken() {
        String resetToken = jwtUtil.generateResetToken("reset@example.com");
        String result = jwtUtil.validateResetToken(resetToken);

        assertEquals("reset@example.com", result);
    }

    @Test
    void testValidateResetToken_Invalid() {
        String result = jwtUtil.validateResetToken("invalid.token.string");

        assertNull(result);
    }

    @Test
    void testGetUserIdFromUsername_Found() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Long id = jwtUtil.getUserIdFromUsername("test@example.com");

        assertEquals(1L, id);
    }

    @Test
    void testGetUserIdFromUsername_NotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(UsernameNotFoundException.class, () ->
                jwtUtil.getUserIdFromUsername("notfound@example.com"));

        assertEquals("User not found with email: notfound@example.com", ex.getMessage());
    }

//    @Test
//    void testExtractUserId_InvalidFormat() {
//        // Create a token without "userId" claim
//        String token = jwtUtil.generateResetToken("noUserId@example.com");
//
//        Exception ex = assertThrows(JwtException.class, () ->
//                jwtUtil.extractUserId("Bearer " + token));
//
//        assertEquals("Invalid user ID format in JWT", ex.getMessage());
//    }
}
