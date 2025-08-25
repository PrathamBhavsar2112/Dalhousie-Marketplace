package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Category;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.CategoryRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ListingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ListingRepository listingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private JwtUtil jwtUtil;

    private Long listingId;
    private String authHeader;
    private Long categoryId;

    @BeforeEach
    void printProfile() {
        System.out.println("Running with profile: " + System.getProperty("spring.profiles.active"));
    }
    @BeforeEach
    public void setup() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setusername("Seller");
        user.setAccountStatus("ACTIVE");
        user.setIsVerified(true);
        user.setbannerId("B01000000");
        user = userRepository.saveAndFlush(user);

        Category category = new Category();
        category.setName("TestCat");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId(); // Save it

        Listing listing = new Listing();
        listing.setTitle("Test Item");
        listing.setDescription("Some item");
        listing.setPrice(15.5);
        listing.setQuantity(1);
        listing.setSeller(user);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing.setCategoryId(category.getId());
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());

        listing = listingRepository.saveAndFlush(listing);
        listingRepository.flush();
        listingId = listing.getId();

        authHeader = "Bearer " + jwtUtil.generateToken(user);
    }

    @Test
    public void testGetAllListings() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Item"));
    }

    @Test
    public void testGetAllListingsIncludingInactive() throws Exception {
        mockMvc.perform(get("/api/listings/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Item"));
    }

    @Test
    public void testGetListingById_Success() throws Exception {
        mockMvc.perform(get("/api/listings/dto/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Item"));
    }

    @Test
    public void testCreateListing_Success() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images", "image.jpg", "image/jpeg", "dummydata".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/listings/create")
                        .file(image)
                        .param("title", "New Listing")
                        .param("description", "Test description")
                        .param("price", "20.0")
                        .param("quantity", "1")
                        .param("categoryId", String.valueOf(categoryId)) // <== Use actual category ID
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Listing created successfully")));
    }



    @Test
    public void testSearchListings() throws Exception {
        mockMvc.perform(get("/api/listings/search")
                        .param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Item"));
    }

    @Test
    public void testGetBiddableListings_Empty() throws Exception {
        mockMvc.perform(get("/api/listings/biddable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
