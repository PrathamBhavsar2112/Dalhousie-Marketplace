import React, { useState, useEffect,useRef  } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ReviewComponent from "../component/ReviewComponent";
import {
  Heart,
  ShoppingCart,
  Share2,
  ArrowLeft,
  ArrowRight,
  X,
  Twitter,
  Send,
  Link,
  Share,
} from "lucide-react";
import "../css/MarketplaceListing.css";
import { BASE_URL } from "../constant_url";
import ChatBox from "./ChatBox.jsx";
import SellerConversations from "./SellerConversations";
import { useDarkMode } from "../pages/DarkModeContext";
import BidComponent from "../component/BidComponent";
import BidManagement from "../component/BidManagement";

const checkWishlistStatus = async (productId) => {
  try {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("userId");
    

    if (!token || !userId) {
      return false;
    }

    const response = await fetch(`${BASE_URL}/api/wishlist/${userId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.ok) {
      const wishlistData = await response.json();
      return (
        wishlistData.wishlistItems?.some(
          (item) => item.listing.id === productId
        ) || false
      );
    }
    return false;
  } catch (error) {
    console.error("Error checking wishlist status:", error);
    return false;
  }
};

const SharePopup = ({ onShare, listing }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [nativeShareAvailable, setNativeShareAvailable] = useState(false);

  useEffect(() => {
    setNativeShareAvailable(!!navigator.share);
  }, []);

  const handleShare = async (platform) => {
    const shareUrl = window.location.href;
    const shareTitle = listing
      ? `Check out ${listing.title} on Dalhousie Marketplace`
      : "";
    const shareDescription = listing ? listing.description : "";

    try {
      switch (platform) {
        case "native":
          await navigator.share({
            title: shareTitle,
            text: shareDescription,
            url: shareUrl,
          });
          break;
        case "twitter":
          window.open(
            `https://twitter.com/intent/tweet?text=${encodeURIComponent(
              shareTitle
            )}&url=${encodeURIComponent(shareUrl)}`,
            "_blank",
            "width=550,height=420"
          );
          break;
        case "whatsapp":
          const whatsappMessage = `${shareTitle}\n\n${shareDescription}\n\n${shareUrl}`;
          window.open(
            `https://wa.me/?text=${encodeURIComponent(whatsappMessage)}`,
            "_blank"
          );
          break;
        case "copy":
          await navigator.clipboard.writeText(shareUrl);
          alert("Link copied to clipboard!");
          break;
        default:
          if (navigator.share) {
            await navigator.share({
              title: shareTitle,
              text: shareDescription,
              url: shareUrl,
            });
          }
      }
      setIsOpen(false);
    } catch (error) {
      console.error("Error sharing:", error);
      alert("Failed to share. Please try again.");
    }
  };

  return (
    <div className="share-container">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="share-button"
        aria-label="Share"
      >
        <Share2 className="w-5 h-5" />
      </button>

      {isOpen && (
        <div className="share-popup">
          <div className="share-popup-content">
            <button className="close-popup" onClick={() => setIsOpen(false)}>
              <X className="w-4 h-4" />
            </button>
            <h3>Share this listing</h3>
            <div className="share-options">
              {nativeShareAvailable && (
                <button
                  onClick={() => handleShare("native")}
                  className="share-option native"
                >
                  <Share className="w-5 h-5" />
                  <span>Share to...</span>
                </button>
              )}
              <button
                onClick={() => handleShare("twitter")}
                className="share-option twitter"
              >
                <Twitter className="w-5 h-5" />
                <span>Twitter</span>
              </button>
              <button
                onClick={() => handleShare("whatsapp")}
                className="share-option whatsapp"
              >
                <Send className="w-5 h-5" />
                <span>WhatsApp</span>
              </button>
              <button
                onClick={() => handleShare("copy")}
                className="share-option copy"
              >
                <Link className="w-5 h-5" />
                <span>Copy Link</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const MarketplaceListing = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const { isDarkMode } = useDarkMode();
  const [listing, setListing] = useState(null);
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [setShowLoginPrompt] = useState(false);
  const buyerId = localStorage.getItem("userId");
  const listingId = id;
  const sellerId = listing?.seller?.userId;
  const imagesRef = useRef([]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    setIsAuthenticated(!!token);
  }, []);

  useEffect(() => {
    const checkInitialWishlistStatus = async () => {
      if (listing) {
        const isInWishlist = await checkWishlistStatus(listing.id);
        setIsWishlisted(isInWishlist);
      }
    };

    checkInitialWishlistStatus();
  }, [listing]);

  useEffect(() => {
    const fetchListingDetails = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const listingResponse = await fetch(`${BASE_URL}/api/listings/${id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!listingResponse.ok) {
          throw new Error("Failed to fetch listing details");
        }

        const listingData = await listingResponse.json();
        setListing(listingData);

        const imagesResponse = await fetch(
          `${BASE_URL}/api/listings/${id}/images`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        if (!imagesResponse.ok) {
          throw new Error("Failed to fetch listing images");
        }

        const imagesMetadata = await imagesResponse.json();

        const imagePromises = imagesMetadata.map(async (imageMetadata) => {
          const imageResponse = await fetch(`${BASE_URL}${imageMetadata.url}`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });

          if (!imageResponse.ok) {
            throw new Error(`Failed to fetch image: ${imageMetadata.id}`);
          }

          const blob = await imageResponse.blob();
          return {
            ...imageMetadata,
            url: URL.createObjectURL(blob),
          };
        });

        const resolvedImages = await Promise.all(imagePromises);
        imagesRef.current = resolvedImages;
        setImages(resolvedImages);
        setLoading(false);
      } catch (error) {
        console.error("Error fetching listing:", error);
        setError(error.message);
        setLoading(false);
      }
    };

    if (id) {
      fetchListingDetails();
    }

    return () => {
      imagesRef.current.forEach((image) => {
        if (image.url.startsWith("blob:")) {
          URL.revokeObjectURL(image.url);
        }
      });
    };
  }, [id, navigate]);

  const handleLoginPrompt = () => {
    if (!isAuthenticated) {
      setShowLoginPrompt(true);
      setTimeout(() => {
        navigate("/login");
      }, 1500);
    }
  };

  const handleWishlist = async () => {
    if (!isAuthenticated) {
      handleLoginPrompt();
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      if (!token || !userId) {
        navigate("/login");
        return;
      }

      const endpoint = isWishlisted
        ? `${BASE_URL}/api/wishlist/${userId}/items/${listing.id}`
        : `${BASE_URL}/api/wishlist/${userId}/add/${listing.id}`;

      const response = await fetch(endpoint, {
        method: isWishlisted ? "DELETE" : "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        setIsWishlisted(!isWishlisted);
      } else if (response.status === 401) {
        navigate("/login");
      } else {
        throw new Error(
          `Failed to ${isWishlisted ? "remove from" : "add to"} wishlist`
        );
      }
    } catch (error) {
      console.error("Error updating wishlist:", error);
      alert("Failed to update wishlist. Please try again.");
    }
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      handleLoginPrompt();
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");
      const response = await fetch(
        `${BASE_URL}/api/cart/${userId}/add/${id}?quantity=1`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (response.ok) {
        alert("Added to cart successfully!");
        navigate("/cart");
      }
    } catch (error) {
      console.error("Error adding to cart:", error);
      alert("Failed to add item to cart. Please try again.");
    }
  };

  const nextImage = () => {
    setCurrentImageIndex((prev) => (prev + 1) % images.length);
  };

  const previousImage = () => {
    setCurrentImageIndex((prev) => (prev - 1 + images.length) % images.length);
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error">Error: {error}</div>;
  if (!listing) return <div className="error">Listing not found</div>;

  const numericBuyerId = Number(buyerId);
  const numericSellerId = Number(sellerId);

  if (!numericBuyerId || !numericSellerId || !listingId) {
    console.warn("Missing required props for chat functionality:", {
      buyerId: numericBuyerId,
      sellerId: numericSellerId,
      listingId: listingId,
    });
  }

  return (
    <div className={`marketplace-container ${isDarkMode ? "dark" : ""}`}>
      <div className="content-area">
        <div className="product-card">
          <button className="close-button" onClick={() => navigate("/buying")}>
            <X className="w-5 h-5" />
          </button>

          <div className="relative">
            <div className="image-container">
              {images.length > 0 && (
                <img
                  src={images[currentImageIndex].url}
                  alt={listing.title}
                  className="product-image"
                />
              )}
              {images.length > 1 && (
                <>
                  <button onClick={previousImage} className="nav-button left">
                    <ArrowLeft className="w-5 h-5" />
                  </button>
                  <button onClick={nextImage} className="nav-button right">
                    <ArrowRight className="w-5 h-5" />
                  </button>
                </>
              )}
            </div>

            <div className="product-info">
              <div className="product-header">
                <div className="product-title">
                  <h2>
                    <b>{listing.title}</b>
                  </h2>
                  <p className="price">${listing.price}</p>
                  <p className="listing-date">
                    Listed - {new Date(listing.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="action-buttons">
                  <SharePopup onShare={() => {}} listing={listing} />
                  <button>
                    <Heart
                      onClick={handleWishlist}
                      aria-label={
                        isWishlisted
                          ? "Remove from wishlist"
                          : "Add to wishlist"
                      }
                      className={`heart-icon ${
                        isWishlisted ? "wishlist-active" : ""
                      }`}
                    />
                  </button>
                  <button
                    onClick={handleAddToCart}
                    className="add-to-cart-button"
                    style={{
                      display:
                        numericBuyerId === numericSellerId ||
                        (listing.biddingAllowed &&
                          numericBuyerId !== numericSellerId)
                          ? "none"
                          : "flex",
                    }}
                  >
                    <ShoppingCart className="w-5 h-5" />
                    Add to Cart
                  </button>
                </div>
              </div>

              <div className="product-details">
                <p className="category">
                  <span className="label">Category:</span>
                  <span className="value">{listing.categoryName || "Not specified"}</span>
                </p>
                <p className="quantity">
                  <span className="label">Quantity:</span>
                  <span className="value">{listing.quantity || "Not specified"}</span>
                </p>
                <p className="purchase-date">
                  <span className="label">Purchased On:</span>
                  <span className="value">
                    {listing.purchaseDate
                      ? new Date(listing.purchaseDate).toLocaleDateString()
                      : "Not specified"}
                  </span>
                </p>
                
                <div className="description">
                  <h2>Description</h2>
                  <p>{listing.description}</p>
                </div>
              </div>

              <div className="seller-info-container">
                <div className="seller-info">
                  <div className="seller-image-placeholder">
                    {listing?.seller?.username ? listing.seller.username.charAt(0).toUpperCase() : "?"}
                  </div>
                  <div>
                    <h3>{listing?.seller?.username || "Anonymous"}</h3>
                    <div className="seller-joined">
                      Seller since{" "}
                      {new Date(listing.createdAt).toLocaleDateString("en-US", {
                        month: "long",
                        year: "numeric",
                      })}
                    </div>
                  </div>
                </div>
              </div>

              <ReviewComponent
                listingId={id}
                isAuthenticated={isAuthenticated}
                handleLoginPrompt={handleLoginPrompt}
              />

              {listing &&
                listing.biddingAllowed &&
                numericBuyerId !== numericSellerId && (
                  <BidComponent
                    listingId={id}
                    listingTitle={listing.title}
                    startingBid={listing.startingBid}
                    currentPrice={listing.price}
                  />
                )}

              {listing &&
                listing.biddingAllowed &&
                numericBuyerId === numericSellerId && (
                  <BidManagement
                    listingId={id}
                    listingTitle={listing.title}
                    isSeller={true}
                  />
                )}
            </div>
          </div>
        </div>
      </div>

      {numericBuyerId && numericSellerId && listingId ? (
        <>
          {numericBuyerId !== numericSellerId && (
            <ChatBox
              senderId={numericBuyerId}
              receiverId={numericSellerId}
              listingId={listingId}
            />
          )}

          {numericBuyerId === numericSellerId && (
            <SellerConversations
              listingId={listingId}
              sellerId={numericSellerId}
            />
          )}
        </>
      ) : (
        <div className="error-container">
          <p>Chat unavailable. Missing user or listing information.</p>
        </div>
      )}
    </div>
  );
};

export default MarketplaceListing;
