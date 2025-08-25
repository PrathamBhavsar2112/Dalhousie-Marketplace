package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.Category;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.CategoryRepository;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ListingImageControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private CategoryRepository categoryRepository;

    private User seller;
    private Long listingId;
    private String authHeader;

    @BeforeEach
    void setup() {
        // Create a seller user
        seller = new User();
        seller.setEmail("seller@example.com");
        seller.setPasswordHash("hashedPassword");
        seller.setusername("Seller");
        seller.setAccountStatus("ACTIVE");
        seller.setIsVerified(true);
        seller.setbannerId("B01010001");
        seller = userRepository.saveAndFlush(seller);

        // Create a category
        Category category = new Category();
        category.setName("Electronics");
        category = categoryRepository.saveAndFlush(category);

        // Create a listing
        Listing listing = new Listing();
        listing.setTitle("Test Listing");
        listing.setDescription("Test description");
        listing.setPrice(100.0);
        listing.setQuantity(5);
        listing.setSeller(seller);
        listing.setCategoryId(category.getId());
        listing = listingRepository.saveAndFlush(listing);
        listingId = listing.getId();

        // Generate JWT for the seller
        authHeader = "Bearer " + jwtUtil.generateToken(seller);
    }

    @Test
    void testUploadImage_Success() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummyimagecontent".getBytes());

        mockMvc.perform(multipart("/api/listings/{listingId}/images/upload", listingId)
                        .file(imageFile)
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("Image uploaded successfully"));
    }

    @Test
    void testUploadImage_UserNotFound() throws Exception {
        // Create a fake user with an email that doesn't exist in the DB
        User fakeUser = new User();
        fakeUser.setEmail("nonexistentuser@example.com");  // Important!

        // Generate a valid token for this fake user
        String validButUnmappedToken = "Bearer " + jwtUtil.generateToken(fakeUser);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "dummyimagecontent".getBytes());

        mockMvc.perform(multipart("/api/listings/{listingId}/images/upload", listingId)
                        .file(imageFile)
                        .header(HttpHeaders.AUTHORIZATION, validButUnmappedToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found."));
    }


    @Test
    void testUploadImage_UnauthorizedWithoutToken() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummyimagecontent".getBytes());

        mockMvc.perform(multipart("/api/listings/{listingId}/images/upload", listingId)
                        .file(imageFile))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized: Missing or invalid token."));
    }

    @Test
    void testUploadImage_NotVerifiedUser() throws Exception {
        // Simulate a non-verified user
        User nonVerifiedUser = new User();
        nonVerifiedUser.setEmail("nonverified@example.com");
        nonVerifiedUser.setPasswordHash("hashedPassword");
        nonVerifiedUser.setusername("NonVerifiedUser");
        nonVerifiedUser.setAccountStatus("ACTIVE");
        nonVerifiedUser.setIsVerified(false);  // Non-verified user
        nonVerifiedUser.setbannerId("B01010002");
        nonVerifiedUser = userRepository.saveAndFlush(nonVerifiedUser);

        String tokenForNonVerifiedUser = "Bearer " + jwtUtil.generateToken(nonVerifiedUser);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummyimagecontent".getBytes());

        mockMvc.perform(multipart("/api/listings/{listingId}/images/upload", listingId)
                        .file(imageFile)
                        .header(HttpHeaders.AUTHORIZATION, tokenForNonVerifiedUser))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Forbidden: Please verify your email before uploading images."));
    }
}
