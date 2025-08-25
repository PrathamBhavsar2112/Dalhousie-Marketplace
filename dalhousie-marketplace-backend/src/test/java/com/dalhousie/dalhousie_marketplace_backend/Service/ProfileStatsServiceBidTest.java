package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.*;
import com.dalhousie.dalhousie_marketplace_backend.service.ProfileStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for bid-related functionality in the ProfileStatsService.
 * Focuses on statistics related to bidding activity for both buyers and sellers.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileStatsServiceBidTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private ProfileStatsService profileStatsService;

    private final Long BUYER_ID = 1L;
    private final Long SELLER_ID = 2L;
    private List<Bid> buyerBids;
    private List<Bid> sellerReceivedBids;
    private List<Listing> sellerListings;

    /**
     * Sets up test data before each test.
     * Creates test bids, listings and orders.
     */
    @BeforeEach
    void setUp() {
        // Create test buyer's bids with different statuses
        buyerBids = new ArrayList<>();
        buyerBids.add(createBid(1L, BidStatus.PENDING));
        buyerBids.add(createBid(2L, BidStatus.COUNTERED));
        buyerBids.add(createBid(3L, BidStatus.ACCEPTED));
        buyerBids.add(createBid(4L, BidStatus.REJECTED));
        buyerBids.add(createBid(5L, BidStatus.PAID));
        buyerBids.add(createBid(6L, BidStatus.PAID));
        buyerBids.add(createBid(7L, BidStatus.PENDING));

        // Create seller's listings
        sellerListings = new ArrayList<>();
        sellerListings.add(createListing(1L));
        sellerListings.add(createListing(2L));

        // Create bids received by the seller
        sellerReceivedBids = new ArrayList<>();
        sellerReceivedBids.add(createBid(10L, BidStatus.PENDING));
        sellerReceivedBids.add(createBid(11L, BidStatus.COUNTERED));
        sellerReceivedBids.add(createBid(12L, BidStatus.ACCEPTED));
        sellerReceivedBids.add(createBid(13L, BidStatus.REJECTED));
        sellerReceivedBids.add(createBid(14L, BidStatus.PAID));
        sellerReceivedBids.add(createBid(15L, BidStatus.PENDING));
    }

    /**
     * Creates a test bid with the specified ID and status.
     */
    private Bid createBid(Long id, BidStatus status) {
        Bid bid = new Bid();
        bid.setId(id);
        bid.setStatus(status);
        bid.setProposedPrice(100.0);

        // Create minimal objects to satisfy relationships
        User buyer = new User();
        buyer.setUserId(BUYER_ID);

        User seller = new User();
        seller.setUserId(SELLER_ID);

        Listing listing = new Listing();
        listing.setId(id);
        listing.setSeller(seller);

        bid.setBuyer(buyer);
        bid.setListing(listing);

        return bid;
    }

    /**
     * Creates a test listing with the specified ID.
     */
    private Listing createListing(Long id) {
        Listing listing = new Listing();
        listing.setId(id);

        User seller = new User();
        seller.setUserId(SELLER_ID);
        listing.setSeller(seller);

        return listing;
    }

    /**
     * Tests that buyer bidding statistics are calculated correctly.
     */
    @Test
    void testBuyerBidStats() {
        // Arrange
        when(bidRepository.findByBuyerId(BUYER_ID)).thenReturn(buyerBids);
        when(orderRepository.findByUserId(BUYER_ID)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> buyerStats = profileStatsService.getBuyerStats(BUYER_ID);

        // Assert
        assertNotNull(buyerStats);

        @SuppressWarnings("unchecked")
        Map<String, Object> biddingActivity = (Map<String, Object>) buyerStats.get("biddingActivity");

        assertNotNull(biddingActivity);
        assertEquals(7, biddingActivity.get("totalBids"));
        assertEquals(3L, biddingActivity.get("activeBids")); // PENDING + COUNTERED
        assertEquals(1L, biddingActivity.get("acceptedBids")); // ACCEPTED
        assertEquals(2L, biddingActivity.get("successfulBids")); // PAID

        // Verify repository calls
        verify(bidRepository).findByBuyerId(BUYER_ID);
    }

    /**
     * Tests that seller bidding statistics are calculated correctly.
     */
    @Test
    void testSellerBidStats() {
        // Arrange
        when(listingRepository.findBySellerId(SELLER_ID)).thenReturn(sellerListings);

        List<Long> listingIds = Arrays.asList(1L, 2L);
        when(bidRepository.findBySellerListings(listingIds)).thenReturn(sellerReceivedBids);
        when(bidRepository.findAcceptedAndPaidBidsBySellerId(SELLER_ID)).thenReturn(
                sellerReceivedBids.stream()
                        .filter(bid -> bid.getStatus() == BidStatus.PAID)
                        .toList()
        );

        when(orderItemRepository.findByListingIdIn(listingIds)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> sellerStats = profileStatsService.getSellerStats(SELLER_ID);

        // Assert
        assertNotNull(sellerStats);

        @SuppressWarnings("unchecked")
        Map<String, Object> bidActivity = (Map<String, Object>) sellerStats.get("bidActivity");

        assertNotNull(bidActivity);
        assertEquals(6, bidActivity.get("totalBidsReceived"));
        assertEquals(3L, bidActivity.get("activeBidsReceived")); // PENDING + COUNTERED

        @SuppressWarnings("unchecked")
        Map<String, Object> salesActivity = (Map<String, Object>) sellerStats.get("salesActivity");

        assertNotNull(salesActivity);
        // Check bid sales calculation
        BigDecimal expectedBidSales = new BigDecimal("100.0"); // 1 PAID bid at $100.0
        assertEquals(0, expectedBidSales.compareTo((BigDecimal) salesActivity.get("bidSales")));

        // Verify repository calls
        verify(listingRepository).findBySellerId(SELLER_ID);
        verify(bidRepository).findBySellerListings(listingIds);
        verify(bidRepository).findAcceptedAndPaidBidsBySellerId(SELLER_ID);
    }

    /**
     * Tests that the overall profile stats combine buyer and seller stats correctly.
     */
    @Test
    void testCombinedProfileStats() {
        // Arrange
        // Mock buyer stats creation
        Map<String, Object> buyerStats = new HashMap<>();
        Map<String, Object> biddingActivity = new HashMap<>();
        biddingActivity.put("totalBids", 7);
        biddingActivity.put("activeBids", 3L);
        biddingActivity.put("acceptedBids", 1L);
        biddingActivity.put("successfulBids", 2L);
        buyerStats.put("biddingActivity", biddingActivity);

        // Mock seller stats creation
        Map<String, Object> sellerStats = new HashMap<>();
        Map<String, Object> sellerBidActivity = new HashMap<>();
        sellerBidActivity.put("totalBidsReceived", 6);
        sellerBidActivity.put("activeBidsReceived", 3L);
        sellerStats.put("bidActivity", sellerBidActivity);

        // Setup ProfileStatsService spy to return our mocks
        ProfileStatsService spy = spy(profileStatsService);
        doReturn(buyerStats).when(spy).getBuyerStats(BUYER_ID);
        doReturn(sellerStats).when(spy).getSellerStats(BUYER_ID);

        // Act
        Map<String, Object> profileStats = spy.getProfileStats(BUYER_ID);

        // Assert
        assertNotNull(profileStats);
        assertTrue(profileStats.containsKey("biddingActivity"));
        assertTrue(profileStats.containsKey("bidActivity"));

        // Verify the stats were combined
        assertEquals(biddingActivity, profileStats.get("biddingActivity"));
        assertEquals(sellerBidActivity, profileStats.get("bidActivity"));

        // Verify both buyer and seller stats were fetched
        verify(spy).getBuyerStats(BUYER_ID);
        verify(spy).getSellerStats(BUYER_ID);
    }

    /**
     * Tests that bid sales are correctly calculated when there are multiple paid bids.
     */
    @Test
    void testBidSalesCalc() {
        // Arrange
        when(listingRepository.findBySellerId(SELLER_ID)).thenReturn(sellerListings);

        List<Long> listingIds = Arrays.asList(1L, 2L);

        // Create paid bids with different prices
        List<Bid> paidBids = new ArrayList<>();
        Bid paidBid1 = createBid(20L, BidStatus.PAID);
        paidBid1.setProposedPrice(150.0);

        Bid paidBid2 = createBid(21L, BidStatus.PAID);
        paidBid2.setProposedPrice(200.0);

        paidBids.add(paidBid1);
        paidBids.add(paidBid2);

        when(bidRepository.findBySellerListings(listingIds)).thenReturn(sellerReceivedBids);
        when(bidRepository.findAcceptedAndPaidBidsBySellerId(SELLER_ID)).thenReturn(paidBids);

        when(orderItemRepository.findByListingIdIn(listingIds)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> sellerStats = profileStatsService.getSellerStats(SELLER_ID);

        // Assert
        assertNotNull(sellerStats);

        @SuppressWarnings("unchecked")
        Map<String, Object> salesActivity = (Map<String, Object>) sellerStats.get("salesActivity");

        assertNotNull(salesActivity);

        // Expected bid sales: $150 + $200 = $350
        BigDecimal expectedBidSales = new BigDecimal("350.0");
        assertEquals(0, expectedBidSales.compareTo((BigDecimal) salesActivity.get("bidSales")));

        // Total sales should include bid sales
        assertEquals(0, expectedBidSales.compareTo((BigDecimal) salesActivity.get("totalSales")));

        // Items sold should include bid sales
        assertEquals(2, salesActivity.get("itemsSold"));
    }

    /**
     * Tests that empty lists are handled correctly in getBuyerStats.
     */
    @Test
    void testEmptyBuyerBids() {
        // Arrange
        when(bidRepository.findByBuyerId(BUYER_ID)).thenReturn(Collections.emptyList());
        when(orderRepository.findByUserId(BUYER_ID)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> buyerStats = profileStatsService.getBuyerStats(BUYER_ID);

        // Assert
        assertNotNull(buyerStats);

        @SuppressWarnings("unchecked")
        Map<String, Object> biddingActivity = (Map<String, Object>) buyerStats.get("biddingActivity");

        assertNotNull(biddingActivity);
        assertEquals(0, biddingActivity.get("totalBids"));
        assertEquals(0L, biddingActivity.get("activeBids"));
        assertEquals(0L, biddingActivity.get("acceptedBids"));
        assertEquals(0L, biddingActivity.get("successfulBids"));
    }

    /**
     * Tests that empty lists are handled correctly in getSellerStats.
     */
    @Test
    void testEmptySellerListings() {
        // Arrange
        when(listingRepository.findBySellerId(SELLER_ID)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> sellerStats = profileStatsService.getSellerStats(SELLER_ID);

        // Assert
        assertNotNull(sellerStats);

        @SuppressWarnings("unchecked")
        Map<String, Object> bidActivity = (Map<String, Object>) sellerStats.get("bidActivity");

        assertNotNull(bidActivity);
        assertEquals(0, bidActivity.get("totalBidsReceived"));
        assertEquals(0L, bidActivity.get("activeBidsReceived"));

        @SuppressWarnings("unchecked")
        Map<String, Object> salesActivity = (Map<String, Object>) sellerStats.get("salesActivity");

        assertNotNull(salesActivity);
        assertEquals(0, ((BigDecimal) salesActivity.get("bidSales")).compareTo(BigDecimal.ZERO));
    }
}