package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ListingDTO;
import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.ListingImage;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingImageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    @Autowired
    private ListingService listingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ListingImageRepository listingImageRepository;
    private UserRepository userRepository;

    /**
     * Get all active listings
     * @return List of active listings
     */
    @GetMapping
    public ResponseEntity<List<Listing>> getAllListings() {
        List<Listing> activeListings = listingService.getActiveListings();
        return ResponseEntity.ok(activeListings);
    }

    /**
     * Get all listings (including inactive and sold)
     * For admin use
     * @return List of all listings
     */
    @GetMapping("/all")
    public ResponseEntity<List<Listing>> getAllListingsIncludingInactive() {
        List<Listing> allListings = listingService.getAllListings();
        return ResponseEntity.ok(allListings);
    }

    /**
     * Get a listing by ID
     * @param id Listing ID
     * @return The listing with the specified ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getListingById(@PathVariable Long id) {
        try {
            ListingDTO listing = listingService.getListingById(id);
            return ResponseEntity.ok(listing);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/dto/{id}")
    public ResponseEntity<ListingDTO> getListing(@PathVariable Long id) {
    ListingDTO listing = listingService.getListingById(id);
    return ResponseEntity.ok(listing);
    }

    /**
     * Create a new listing
     * @param images Images for the listing
     * @param listing Listing data
     * @param token JWT token for authentication
     * @return Response with status and message
     * @throws IOException If image processing fails
     */
    @PostMapping("/create")
    public ResponseEntity<String> createListing(
            @RequestParam("images") MultipartFile[] images,
            @ModelAttribute Listing listing,
            @RequestHeader(value = "Authorization", required = false) String token) throws IOException {


        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized: Missing or invalid token.");
        }


        String jwtToken = token.substring(7);
        String userEmail = jwtUtil.extractUsername(jwtToken);

        if (userEmail == null || userEmail.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid token.");
        }


        Listing createdListing = listingService.createListing(listing, userEmail, images);


        return ResponseEntity.ok("Listing created successfully with ID: " + createdListing.getId());
    }

    /**
     * Get a specific image for a listing
     * @param listingId Listing ID
     * @param imageId Image ID
     * @return The image data with appropriate content type
     */
    @GetMapping("/{listingId}/images/{imageId}")
    public ResponseEntity<?> getListingImage(@PathVariable Long listingId, @PathVariable Long imageId) {
        try {
            ListingImage image = listingImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));


            if (!image.getListing().getId().equals(listingId)) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getImageType()));
            headers.setContentLength(image.getImageSize());

            return new ResponseEntity<>(image.getImageData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching image: " + e.getMessage());
        }
    }

    /**
     * Get all images for a listing
     * @param listingId Listing ID
     * @return List of image metadata
     */
    @GetMapping("/{listingId}/images")
    public ResponseEntity<?> getListingImages(@PathVariable Long listingId) {
        try {
            List<ListingImage> images = listingImageRepository.findByListingId(listingId);
            if (images.isEmpty()) {
                return ResponseEntity.notFound().build();
            }


            List<ImageMetadata> imageMetadata = images.stream()
                    .map(img -> new ImageMetadata(
                            img.getId(),
                            img.getImageType(),
                            img.getImageSize(),
                            img.getIsPrimary(),
                            "/api/listings/" + listingId + "/images/" + img.getId()
                    ))
                    .toList();

            return ResponseEntity.ok(imageMetadata);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching images: " + e.getMessage());
        }
    }

    /**
     * Search listings by keyword
     * @param keyword Search term
     * @return List of matching listings
     */
    @GetMapping("/search")
    public ResponseEntity<List<Listing>> searchListings(@RequestParam(value = "keyword", required = false) String keyword) {
        List<Listing> listings = listingService.searchListings(keyword);
        return ResponseEntity.ok(listings);
    }

    /**
     * Inner class for image metadata
     */
    private static class ImageMetadata {
        private Long id;
        private String contentType;
        private Long size;
        private Boolean isPrimary;
        private String url;

        public ImageMetadata(Long id, String contentType, Long size, Boolean isPrimary, String url) {
            this.id = id;
            this.contentType = contentType;
            this.size = size;
            this.isPrimary = isPrimary;
            this.url = url;
        }

        // Getters
        public Long getId() { return id; }
        public String getContentType() { return contentType; }
        public Long getSize() { return size; }
        public Boolean getIsPrimary() { return isPrimary; }
        public String getUrl() { return url; }
    }

    @GetMapping("/biddable")
    public ResponseEntity<List<Listing>> getBiddableListings() {
        List<Listing> biddableListings = listingService.getBiddableListings();
        return ResponseEntity.ok(biddableListings);
    }
}