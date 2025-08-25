package com.dalhousie.dalhousie_marketplace_backend.service;

import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.ListingImage;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingImageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ListingImageService {

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private ListingRepository listingRepository;

    public ListingImage saveImage(Long listingId, MultipartFile imageFile, Long sellerId, boolean isPrimary) throws IOException {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        ListingImage image = new ListingImage();
        image.setListing(listing);
        image.setSellerId(sellerId);
        image.setImageData(imageFile.getBytes());
        image.setImageType(imageFile.getContentType());
        image.setImageSize(imageFile.getSize());
        image.setIsPrimary(isPrimary);

        return listingImageRepository.save(image);
    }
}
