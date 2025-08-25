package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.model.ListingImage;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingImageService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
public class ListingImageController {

    @Autowired
    private ListingImageService listingImageService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@PathVariable Long listingId,
                                              @RequestParam("image") MultipartFile imageFile,
                                              @RequestHeader(value = "Authorization", required = false) String token) {
        // Validate token
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized: Missing or invalid token.");
        }

        // Extract user email from token
        String jwtToken = token.substring(7);
        String userEmail = jwtUtil.extractUsername(jwtToken);

        if (userEmail == null || userEmail.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid token.");
        }

        // Fetch seller by email
        Optional<User> userOptional = userRepository.findByEmail(userEmail);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body("User not found.");
        }

        User seller = userOptional.get();

        // Ensure only verified users can upload images
        if (!seller.isVerified()) {
            return ResponseEntity.status(403).body("Forbidden: Please verify your email before uploading images.");
        }

        try {
            listingImageService.saveImage(listingId, imageFile, seller.getUserId(), false);
            return ResponseEntity.ok("Image uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error uploading image: " + e.getMessage());
        }
    }
}
