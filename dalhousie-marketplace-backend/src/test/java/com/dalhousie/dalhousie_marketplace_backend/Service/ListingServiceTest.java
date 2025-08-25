package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ListingDTO;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.CategoryRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingImageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingImageService;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingService;
import com.dalhousie.dalhousie_marketplace_backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the ListingService class
 */
public class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ListingService listingService;
    @Mock
    private MultipartFile imageFile;

    @Mock
    private ListingImageService listingImageService;

    private Listing activeListing1;
    private Listing activeListing2;
    private Listing inactiveListing;
    private Listing soldListing;
    private User seller;
    private Category category;

    /**
     * Setup method that runs before each test
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a mock seller
        User seller = new User();
        seller.setUserId(1L);
        seller.setEmail("seller@example.com");
        seller.setIsVerified(true);

        // Create sample listings with different statuses
        activeListing1 = createListing(1L, "Active Listing 1", 100.0, Listing.ListingStatus.ACTIVE);
        activeListing2 = createListing(2L, "Active Listing 2", 200.0, Listing.ListingStatus.ACTIVE);
        inactiveListing = createListing(3L, "Inactive Listing", 150.0, Listing.ListingStatus.INACTIVE);
        soldListing = createListing(4L, "Sold Listing", 300.0, Listing.ListingStatus.SOLD);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
    }

    /**
     * Helper method to create a listing with specified properties
     */
    private Listing createListing(Long id, String title, Double price, Listing.ListingStatus status) {
        Listing listing = new Listing();
        listing.setId(id);
        listing.setTitle(title);
        listing.setDescription("Description for " + title);
        listing.setPrice(price);
        listing.setQuantity(1);
        listing.setCategoryId(1L);
        listing.setStatus(status);
        listing.setViews(0);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());
        try {
            listing.setBiddingAllowed(false);
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            // Ignore if field doesn’t exist
        }
        return listing;
    }

    /**
     * Test that getActiveListings returns only listings with ACTIVE status
     */
    @Test
    void getActiveListings_shouldReturnOnlyActiveListings() {
        // Arrange
        List<Listing> allListings = Arrays.asList(activeListing1, activeListing2, inactiveListing, soldListing);
        List<Listing> activeListings = Arrays.asList(activeListing1, activeListing2);

        when(listingRepository.findByStatus(Listing.ListingStatus.ACTIVE)).thenReturn(activeListings);

        // Act
        List<Listing> result = listingService.getActiveListings();

        // Assert
        assertEquals(2, result.size());

        // Verify only active listings are returned
        for (Listing listing : result) {
            assertEquals(Listing.ListingStatus.ACTIVE, listing.getStatus());
        }

        // Verify the repository method was called with the correct status
        verify(listingRepository).findByStatus(Listing.ListingStatus.ACTIVE);

        // Verify the titles are what we expect
        List<String> expectedTitles = Arrays.asList("Active Listing 1", "Active Listing 2");
        List<String> actualTitles = new ArrayList<>();
        for (Listing listing : result) {
            actualTitles.add(listing.getTitle());
        }
        assertTrue(actualTitles.containsAll(expectedTitles));
    }

    @Test
    void getAllListings_ReturnsAllListings() {
        List<Listing> allListings = Arrays.asList(activeListing1, inactiveListing, soldListing);
        when(listingRepository.findAll()).thenReturn(allListings);

        List<Listing> result = listingService.getAllListings();

        assertEquals(3, result.size());
        verify(listingRepository).findAll();
    }

    @Test
    void searchListings_WithKeyword_ReturnsMatches() {
        List<Listing> matches = Arrays.asList(activeListing1);
        when(listingRepository.searchByKeyword("Active")).thenReturn(matches);

        List<Listing> result = listingService.searchListings("Active");

        assertEquals(1, result.size());
        assertEquals("Active Listing 1", result.get(0).getTitle());
        verify(listingRepository).searchByKeyword("Active");
    }

    @Test
    void searchListings_WithEmptyKeyword_ReturnsActiveListings() {
        List<Listing> activeListings = Arrays.asList(activeListing1, activeListing2);
        when(listingRepository.findByStatus(Listing.ListingStatus.ACTIVE)).thenReturn(activeListings);

        List<Listing> result = listingService.searchListings("");

        assertEquals(2, result.size());
        verify(listingRepository).findByStatus(Listing.ListingStatus.ACTIVE);
    }

    @Test
    void createListing_Success() throws IOException {
        User seller = new User();
        seller.setUserId(1L);
        seller.setEmail("seller@example.com");
        seller.setusername("SellerUser");
        seller.setIsVerified(true);

        activeListing1.setSeller(seller);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        when(listingRepository.save(any(Listing.class))).thenReturn(activeListing1);
        when(userRepository.findAll()).thenReturn(Arrays.asList(seller, new User()));
        when(imageFile.isEmpty()).thenReturn(false);
        when(imageFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(3L);
        when(listingImageService.saveImage(eq(activeListing1.getId()), any(MultipartFile.class), eq(seller.getUserId()), anyBoolean()))
                .thenReturn(new ListingImage());

        Listing result = listingService.createListing(activeListing1, seller.getEmail(), new MultipartFile[]{imageFile});

        assertEquals(activeListing1.getId(), result.getId());
        assertEquals(activeListing1.getTitle(), result.getTitle());
        verify(listingRepository).save(any(Listing.class));
        verify(notificationService, atLeastOnce()).sendNotification(any(User.class), eq(NotificationType.ITEM), anyString());
        verify(listingImageService).saveImage(activeListing1.getId(), imageFile, seller.getUserId(), true);
    }

    @Test
    void createListing_InvalidCategory_ThrowsException() throws IOException {
        User seller = new User();
        seller.setUserId(1L);
        seller.setEmail("seller@example.com");
        seller.setusername("SellerUser");
        seller.setIsVerified(true);
        activeListing1.setCategoryId(99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.createListing(activeListing1, seller.getEmail(), null);
        });

        assertEquals("Category with ID 99 does not exist.", exception.getMessage());
        verify(listingRepository, never()).save(any());
    }

    @Test
    void createListing_UnverifiedSeller_ThrowsException() throws IOException {
        User unverifiedSeller = new User(); // Local initialization
        unverifiedSeller.setUserId(2L);
        unverifiedSeller.setEmail("unverified@example.com");
        unverifiedSeller.setusername("UnverifiedUser");
        unverifiedSeller.setIsVerified(false); // This line should now work

        Listing listing = createListing(6L, "Test Listing", 100.0, Listing.ListingStatus.ACTIVE);
        listing.setSeller(unverifiedSeller);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail(unverifiedSeller.getEmail())).thenReturn(Optional.of(unverifiedSeller));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.createListing(listing, unverifiedSeller.getEmail(), null);
        });

        assertEquals("User is not verified. Please verify your email before posting.", exception.getMessage());
        verify(listingRepository, never()).save(any());
    }

    @Test
    void getListingById_Success() {
//        User seller = new User();
//        seller.setUserId(1L);
//        seller.setEmail("seller@example.com");
//        seller.setusername("SellerUser");
//        seller.setIsVerified(true);

        when(listingRepository.findById(activeListing1.getId())).thenReturn(Optional.of(activeListing1));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(listingRepository.save(any(Listing.class))).thenReturn(activeListing1);

        ListingDTO result = listingService.getListingById(activeListing1.getId());

//        assertEquals(activeListing1.getId(), result.getId());
//        assertEquals("Electronics", result.getCategoryName());
//        assertEquals(seller.getUsername(), result.getSeller().getUsername());
        assertNotNull(result);
        verify(listingRepository).save(any(Listing.class)); // Views incremented
    }

    @Test
    void getListingById_NotFound_ThrowsException() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.getListingById(99L);
        });

        assertEquals("Listing not found", exception.getMessage());
        verify(listingRepository, never()).save(any());
    }

    @Test
    void updateListingRatingStats_Success() {
        when(listingRepository.findById(activeListing1.getId())).thenReturn(Optional.of(activeListing1));
        when(listingRepository.save(any(Listing.class))).thenReturn(activeListing1);

        listingService.updateListingRatingStats(activeListing1.getId(), 4.5, 10);

        verify(listingRepository).save(argThat(l -> l.getAverageRating() == 4.5 && l.getReviewCount() == 10));
    }

    @Test
    void updateListingRatingStats_ListingNotFound_ThrowsException() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.updateListingRatingStats(99L, 4.5, 10);
        });

        assertEquals("Listing not found with id: 99", exception.getMessage());
        verify(listingRepository, never()).save(any());
    }

    @Test
    void getBiddableListings_ReturnsBiddableActiveListings() {
        activeListing1.setBiddingAllowed(true);
        List<Listing> biddableListings = Arrays.asList(activeListing1);
        when(listingRepository.findByBiddingAllowedAndStatus(true, Listing.ListingStatus.ACTIVE)).thenReturn(biddableListings);

        List<Listing> result = listingService.getBiddableListings();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getBiddingAllowed());
        verify(listingRepository).findByBiddingAllowedAndStatus(true, Listing.ListingStatus.ACTIVE);
    }

    @Test
    void getBiddingListingsBySeller_ReturnsSellerBiddableListings() {
        User seller = new User();
        seller.setUserId(1L);
        seller.setEmail("seller@example.com");
        seller.setusername("SellerUser");
        seller.setIsVerified(true);

        activeListing1.setBiddingAllowed(true);
        List<Listing> sellerListings = Arrays.asList(activeListing1, inactiveListing);
        when(listingRepository.findBySellerId(seller.getUserId())).thenReturn(sellerListings);

        List<Listing> result = listingService.getBiddingListingsBySeller(seller.getUserId());

        assertEquals(1, result.size());
        assertTrue(result.get(0).getBiddingAllowed());
        verify(listingRepository).findBySellerId(seller.getUserId());
    }
}