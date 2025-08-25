package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ListingDTO;
import com.dalhousie.dalhousie_marketplace_backend.DTO.SellerDTO;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.CategoryRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ListingService {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ListingImageService listingImageService;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all listings regardless of status
     */
    @Transactional(readOnly = true)
    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    /**
     * Get all active listings only
     */
    @Transactional(readOnly = true)
    public List<Listing> getActiveListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE);
    }

    /**
     * Search listings by keyword
     */
    @Transactional(readOnly = true)
    public List<Listing> searchListings(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE);
        }
        return listingRepository.searchByKeyword(keyword.trim());
    }

    /**
     * Create a new listing
     */
    public Listing createListing(Listing listing, String sellerEmail, MultipartFile[] images) throws IOException {
        if (listing.getCategoryId() == null || listing.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Category ID is required and must be valid.");
        }

        Long categoryId = listing.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    String msg = "Category with ID " + categoryId + " does not exist.";
                    return new RuntimeException(msg);
                });

        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> {
                    String msg = "User with email " + sellerEmail + " not found";
                    return new RuntimeException(msg);
                });

        if (!seller.isVerified()) {
            throw new RuntimeException("User is not verified. Please verify your email before posting.");
        }

        listing.setPurchaseDate(listing.getPurchaseDate());
        listing.setSeller(seller);

        Listing savedListing = listingRepository.save(listing);
        notifyUsers(savedListing, seller);

        if (images != null && images.length > 0) {
            boolean isFirstImage = true;
            for (MultipartFile image : images) {
                boolean isValidImage = !image.isEmpty();
                if (isValidImage) {
                    Long listingId = savedListing.getId();
                    Long sellerId = seller.getUserId();
                    listingImageService.saveImage(listingId, image, sellerId, isFirstImage);
                    isFirstImage = false;
                }
            }
        }

        return savedListing;
    }

    private void notifyUsers(Listing listing, User seller) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (!user.equals(seller)) {
                notificationService.sendNotification(
                        user,
                        NotificationType.ITEM,
                        "New post: " + listing.getTitle()
                );
                if (listing.getBiddingAllowed()) {
                    notificationService.sendNotification(
                            user,
                            NotificationType.BID,
                            "New Bidding: " + listing.getTitle()
                    );
                }
            }
        }
    }

    /**
     * Get a listing by ID and increment its view count
     */
    @Transactional
    public ListingDTO getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setViews(listing.getViews() + 1);
        listingRepository.save(listing);

        Category category = categoryRepository.findById(listing.getCategoryId()).orElse(null);

        ListingDTO dto = new ListingDTO();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setDescription(listing.getDescription());
        dto.setPrice(listing.getPrice());
        dto.setQuantity(listing.getQuantity());
        dto.setPurchaseDate(listing.getPurchaseDate());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setCategoryId(listing.getCategoryId());
        dto.setCategoryName(category != null ? category.getName() : "Not specified");
        dto.setBiddingAllowed(listing.getBiddingAllowed());

        User seller = listing.getSeller();
        if (seller != null) {
            dto.setSeller(new SellerDTO(seller.getUserId(), seller.getUsername()));
        }

        return dto;
    }

    @Transactional
    public void updateListingRatingStats(Long listingId, Double averageRating, Integer reviewCount) {
        Optional<Listing> optionalListing = listingRepository.findById(listingId);
        String errorMsg = "Listing not found with id: " + listingId;

        Listing listing = optionalListing.orElseThrow(() -> new RuntimeException(errorMsg));

        listing.setAverageRating(averageRating);
        listing.setReviewCount(reviewCount);

        listingRepository.save(listing);
    }

    /**
     * Get all active listings that allow bidding
     */
    @Transactional(readOnly = true)
    public List<Listing> getBiddableListings() {
        return listingRepository.findByBiddingAllowedAndStatus(true, Listing.ListingStatus.ACTIVE);
    }

    /**
     * Get all biddable listings for a specific seller
     */
    @Transactional(readOnly = true)
    public List<Listing> getBiddingListingsBySeller(Long sellerId) {
        List<Listing> allListings = listingRepository.findBySellerId(sellerId);
        return allListings.stream()
                .filter(Listing::getBiddingAllowed)
                .collect(Collectors.toList());
    }
}
