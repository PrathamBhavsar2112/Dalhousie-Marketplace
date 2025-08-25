import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Heart,
  ShoppingCart
} from 'lucide-react';
import '../css/Wishlist.css';
import { BASE_URL } from "../constant_url";
import { useDarkMode } from "../pages/DarkModeContext";

const WishlistCard = ({ item, onRemove, onAddToCart }) => {
  const [imageUrl, setImageUrl] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchImage = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const imagesResponse = await fetch(
          `${BASE_URL}/api/listings/${item.listing.id}/images`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        if (imagesResponse.ok) {
          const imagesData = await imagesResponse.json();
          const primaryImage =
            imagesData.find((img) => img.isPrimary) || imagesData[0];

          if (primaryImage) {
            const imageResponse = await fetch(
              `${BASE_URL}${primaryImage.url}`,
              {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
              }
            );

            if (imageResponse.ok) {
              const blob = await imageResponse.blob();
              const objectUrl = URL.createObjectURL(blob);
              setImageUrl(objectUrl);
            }
          }
        }
      } catch (error) {
        console.error("Error fetching image:", error);
        if (error.response?.status === 401) {
          navigate("/login");
        }
      }
    };

    fetchImage();

    return () => {
      if (imageUrl) {
        URL.revokeObjectURL(imageUrl);
      }
    };
  }, [item.listing.id, navigate, imageUrl]);

  const handleCardClick = (e) => {
    if (
      e.target.closest(".wishlist-button") ||
      e.target.closest(".cart-button")
    ) {
      return;
    }
    navigate(`/listing/${item.listing.id}`);
  };

  return (
    <div className="product-card" onClick={handleCardClick}>
      <button
        onClick={(e) => {
          e.preventDefault();
          onRemove(item.listing.id);
        }}
        className="wishlist-button"
      >
        <Heart className="heart-icon wishlist-active" />
      </button>

      <div className="product-image-container">
        <img
          src={imageUrl || "/api/placeholder/400/320"}
          alt={item.listing.title}
          className="product-image"
        />
      </div>

      <div className="product-info">
        <h3 className="product-title multi-line-description-1">
          {item.listing.title}
        </h3>
        <p className="product-description multi-line-description-2">
          {item.listing.description}
        </p>
        <div className="product-price-row">
          <span className="product-price">${item.listing.price}</span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onAddToCart(item.listing.id);
            }}
            className="cart-button"
          >
            <ShoppingCart className="w-4 h-4" />
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
};

const WishlistPage = () => {
  const { isDarkMode } = useDarkMode();
  const [wishlistItems, setWishlistItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchWishlist = async () => {
      try {
        const token = localStorage.getItem("token");
        const userId = localStorage.getItem("userId");

        if (!token || !userId) {
          navigate("/login");
          return;
        }

        const response = await fetch(`${BASE_URL}/api/wishlist/${userId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.ok) {
          const data = await response.json();
          setWishlistItems(data.wishlistItems || []);
        }
      } catch (error) {
        console.error("Error fetching wishlist:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchWishlist();
  }, [navigate]);

  const handleRemoveFromWishlist = async (listingId) => {
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      const response = await fetch(
        `${BASE_URL}/api/wishlist/${userId}/items/${listingId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.ok) {
        setWishlistItems((prev) =>
          prev.filter((item) => item.listing.id !== listingId)
        );
      }
    } catch (error) {
      console.error("Error removing from wishlist:", error);
    }
  };

  const handleAddToCart = async (listingId) => {
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      const response = await fetch(
        `${BASE_URL}/api/cart/${userId}/add/${listingId}?quantity=1`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (response.ok) {
        window.dispatchEvent(new Event("cartUpdate"));
        localStorage.setItem("cartLastUpdated", Date.now().toString());
        alert("Added to cart successfully!");
      }
    } catch (error) {
      console.error("Error adding to cart:", error);
      alert("Failed to add item to cart. Please try again.");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="loading-spinner"></div>
      </div>
    );
  }

  return (

    <div className={`items-container ${isDarkMode ? "dark" : ""}`}>
      <div className="content-wrapper">
          <div className="p-6">
            <h1 className="page-title">
              My Wishlist ({wishlistItems.length} items)
            </h1>

            {wishlistItems.length === 0 ? (
              <div className="empty-state">
                <Heart className="empty-state-icon" />
                <h2 className="empty-state-title">Your wishlist is empty</h2>
                <p className="empty-state-description">
                  Start adding items you love!
                </p>
                <button
                  onClick={() => navigate("/buying")}
                  className="browse-button"
                >
                  Browse Products
                </button>
              </div>
            ) : (
              <div className="product-grid">
                {wishlistItems.map((item) => (
                  <WishlistCard
                    key={item.wishlistItemId}
                    item={item}
                    onRemove={handleRemoveFromWishlist}
                    onAddToCart={handleAddToCart}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
    </div>
  );
};

export default WishlistPage;
