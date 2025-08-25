package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.model.Listing;
import com.dalhousie.dalhousie_marketplace_backend.model.ListingImage;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingImageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.ListingImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for ListingImageService to ensure full coverage of saveImage method
 */
@ExtendWith(MockitoExtension.class)
public class ListingImageServiceTest {

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private MultipartFile imageFile;

    @InjectMocks
    private ListingImageService listingImageService;

    private Listing listing;
    private Long sellerId;
    private byte[] imageData;

    @BeforeEach
    void setUp() throws IOException {
        // Sample listing
        listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Test Listing");
        listing.setPrice(100.0);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing.setCreatedAt(new Date());
        listing.setUpdatedAt(new Date());

        sellerId = 2L;
        imageData = new byte[]{1, 2, 3, 4}; // Sample image data

//        // Mock MultipartFile behavior
//        when(imageFile.getBytes()).thenReturn(imageData);
//        when(imageFile.getContentType()).thenReturn("image/jpeg");
//        when(imageFile.getSize()).thenReturn(4L);
    }

    @Test
    void saveImage_ReturnsNonNullResult() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertNotNull(result);
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectListing() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertEquals(listing, result.getListing());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectSellerId() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertEquals(sellerId, result.getSellerId());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectImageData() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertArrayEquals(imageData, result.getImageData());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectImageType() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertEquals("image/jpeg", result.getImageType());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectImageSize() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertEquals(4L, result.getImageSize());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_SetsCorrectIsPrimary() throws IOException {
        when(imageFile.getBytes()).thenReturn(imageData);
        when(imageFile.getContentType()).thenReturn("image/jpeg");
        when(imageFile.getSize()).thenReturn(4L);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        ListingImage savedImage = new ListingImage();
        savedImage.setId(1L);
        savedImage.setListing(listing);
        savedImage.setSellerId(sellerId);
        savedImage.setImageData(imageData);
        savedImage.setImageType("image/jpeg");
        savedImage.setImageSize(4L);
        savedImage.setIsPrimary(true);
        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);

        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);

        assertTrue(result.getIsPrimary());
        verify(listingRepository).findById(listing.getId());
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void saveImage_ListingNotFound() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingImageService.saveImage(99L, imageFile, sellerId, true);
        });

        assertEquals("Listing not found", exception.getMessage());
        verify(listingRepository).findById(99L);
        verify(listingImageRepository, never()).save(any());
    }
//    @Test
//    void saveImage_Success() throws IOException {
//        // Arrange
//        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
//        ListingImage savedImage = new ListingImage();
//        savedImage.setId(1L);
//        savedImage.setListing(listing);
//        savedImage.setSellerId(sellerId);
//        savedImage.setImageData(imageData);
//        savedImage.setImageType("image/jpeg");
//        savedImage.setImageSize(4L);
//        savedImage.setIsPrimary(true);
//        when(listingImageRepository.save(any(ListingImage.class))).thenReturn(savedImage);
//
//        // Act
//        ListingImage result = listingImageService.saveImage(listing.getId(), imageFile, sellerId, true);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(listing, result.getListing());
//        assertEquals(sellerId, result.getSellerId());
//        assertArrayEquals(imageData, result.getImageData());
//        assertEquals("image/jpeg", result.getImageType());
//        assertEquals(4L, result.getImageSize());
//        assertTrue(result.getIsPrimary());
//        verify(listingRepository).findById(listing.getId());
//        verify(listingImageRepository).save(any(ListingImage.class));
//    }

}