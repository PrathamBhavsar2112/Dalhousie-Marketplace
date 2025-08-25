package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRegisterUser_successful() {
        // Create raw JSON manually to match what the controller expects
        String json = """
        {
          "username": "Test User",
          "email": "testuser@example.com",
          "bannerId": "B00987654",
          "passwordHash": "Password123"
        }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", request, String.class);

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("User registered successfully"));
    }



    @Test
    void testLoginUser_invalidCredentials() {
        User user = new User();
        user.setEmail("invalid@example.com");
        user.setPasswordHash("wrongpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Account does not exist"));
    }
    @Test
    void testGetUserById_missingToken() {
        HttpHeaders headers = new HttpHeaders(); // No Authorization header
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/user/1",
                HttpMethod.GET,
                request,
                String.class
        );

//        System.out.println("Status Code: " + response.getStatusCode());
//        System.out.println("Response Body: " + response.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody()); // Make sure body is not null
    }




}
