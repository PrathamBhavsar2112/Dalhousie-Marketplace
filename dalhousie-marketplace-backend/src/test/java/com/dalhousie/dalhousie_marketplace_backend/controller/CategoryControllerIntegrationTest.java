package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.Category;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    public void setUp() {
        // Insert sample categories into DB
        Category cat1 = new Category();
        cat1.setName("Books");

        Category cat2 = new Category();
        cat2.setName("Electronics");

        categoryRepository.save(cat1);
        categoryRepository.save(cat2);
    }

    @Test
    public void testGetAllCategories_Success() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.name == 'Books')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Electronics')]").exists());
    }


    @Test
    public void testGetAllCategories_NoContent() throws Exception {
        reviewRepository.deleteAll();           // 1. delete reviews
        orderItemRepository.deleteAll();        // 2. delete order_items
        messageRepository.deleteAll();          // 3. delete messages (if not already)
        listingRepository.deleteAll();          // 4. delete listings
        categoryRepository.deleteAll();         // 5. finally, delete categories

        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

}
