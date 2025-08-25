import React, { useState, useEffect,useRef  } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Heart,
  ChevronDown,
  DollarSign,
} from "lucide-react";
import "../css/BuyingPage.css";
import { BASE_URL } from "../constant_url";
import PriceFilter from "../component/PriceFilter";
import axios from "axios";

const ProductCard = ({ product, onToggleWishlist }) => {
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [imageUrl, setImageUrl] = useState(null);
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const navigate = useNavigate();
  const imageUrlRef = useRef(null);

  useEffect(() => {
    const fetchImage = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        //fetch the image metadata
        const imagesResponse = await fetch(
          `${BASE_URL}/api/listings/${product.id}/images`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        if (imagesResponse.ok) {
          const imagesData = await imagesResponse.json();
          // Find the primary image or use the first image
          const primaryImage =
            imagesData.find((img) => img.isPrimary) || imagesData[0];

          if (primaryImage) {
            // Fetch the actual image using the URL from the metadata
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
              imageUrlRef.current = objectUrl;
              setImageUrl(objectUrl);
            }
          }
        }

        // Check wishlist status
        const isInWishlist = await checkWishlistStatus(product.id);
        setIsWishlisted(isInWishlist);
      } catch (error) {
        console.error("Error fetching image:", error);
        if (error.response?.status === 401) {
          navigate("/login");
        }
      }
    };

    fetchImage();

    // Cleanup function
    return () => {
      if (imageUrlRef.current) {
        URL.revokeObjectURL(imageUrlRef.current);
      }
    };
  }, [product.id, navigate]);

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

  const handleWishlist = async (e) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      if (!token || !userId) {
        navigate("/login");
        return;
      }

      const endpoint = isWishlisted
        ? `${BASE_URL}/api/wishlist/${userId}/items/${product.id}`
        : `${BASE_URL}/api/wishlist/${userId}/add/${product.id}`;

      const response = await fetch(endpoint, {
        method: isWishlisted ? "DELETE" : "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        setIsWishlisted(!isWishlisted);
        onToggleWishlist(product.id);
      } else if (response.status === 401) {
        navigate("/login");
      } else {
        throw new Error(
          `Failed to ${isWishlisted ? "remove from" : "add to"} wishlist`
        );
      }
    } catch (error) {
      console.error("Error updating wishlist:", error);
      if (error.response?.status === 401) {
        navigate("/login");
      } else {
        alert("Failed to update wishlist. Please try again.");
      }
    }
  };

  const handleCardClick = (e) => {
    if (
      e.target.closest(".cart-button") ||
      e.target.closest(".wishlist-button")
    ) {
      return;
    }
    navigate(`/listing/${product.id}`);
  };

  const handleAddToCart = async () => {
    if (isAddingToCart) return;

    try {
      setIsAddingToCart(true);
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      if (!token) {
        navigate("/login");
        return;
      }

      if (!userId) {
        console.warn("User ID not found. Unable to add item to cart.");
        alert("Please login again to add items to cart.");
        navigate("/login");
        return;
      }

      const response = await fetch(
        `${BASE_URL}/api/cart/${userId}/add/${product.id}?quantity=1`,
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
      } else if (response.status === 401) {
        navigate("/login");
      } else {
        throw new Error("Failed to add item to cart");
      }
    } catch (error) {
      console.error("Error adding to cart:", error);
      alert("Failed to add item to cart. Please try again.");
    } finally {
      setIsAddingToCart(false);
    }
  };

  return (
    <div className="product-card" onClick={handleCardClick}>
      <button onClick={handleWishlist} className="wishlist-button">
        <Heart
          className={`heart-icon ${isWishlisted ? "wishlist-active" : ""}`}
        />
      </button>

      <div className="product-image-container">
        <img
          src={imageUrl || "/api/placeholder/200/160"}
          alt={product.title}
          className="product-image"
        />
      </div>

      <div className="product-info">
        <h3 className="product-title multi-line-description-1">
          {product.title}
        </h3>
        <p className="product-description multi-line-description-2">
          {product.description}
        </p>
        <div className="product-price-row">
          <span className="product-price">${product.price}</span>
          <button
            className={`cart-button ${isAddingToCart ? "disabled" : ""}`}
            onClick={handleAddToCart}
            disabled={isAddingToCart}
          >
            {isAddingToCart ? "Adding..." : "Add to Cart"}
          </button>
        </div>
      </div>
    </div>
  );
};

const CategoryDropdown = ({
  categories,
  selectedCategory,
  onSelectCategory,
}) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="category-dropdown">
      <button className="dropdown-button" onClick={() => setIsOpen(!isOpen)}>
        {selectedCategory || "All Categories"}
        <ChevronDown className="dropdown-icon" />
      </button>
      {isOpen && (
        <div className="dropdown-content">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => {
                onSelectCategory(category);
                setIsOpen(false);
              }}
              className="dropdown-item"
            >
              {category}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

const Pagination = ({ currentPage, totalPages, onPageChange }) => (
  <div className="pagination">
    {[...Array(totalPages)].map((_, index) => (
      <button
        key={index}
        onClick={() => onPageChange(index + 1)}
        className={`page-button ${currentPage === index + 1 ? "active" : ""}`}
      >
        {index + 1}
      </button>
    ))}
  </div>
);

const ProductGrid = ({ searchQuery }) => {
  const [
    selectedCategory, setSelectedCategory] = useState("All");
  const [currentPage, setCurrentPage] = useState(1);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [priceRange, setPriceRange] = useState({ min: 0, max: Infinity });
  const navigate = useNavigate();
  const itemsPerPage = 10;
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const searchParam = searchQuery
          ? `?keyword=${encodeURIComponent(searchQuery)}`
          : "";
        const response = await fetch(`${BASE_URL}/api/listings${searchParam}`, {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) {
          if (response.status === 401) {
            navigate("/login");
            return;
          }
          throw new Error("Failed to fetch listings");
        }

        const data = await response.json();
        setProducts(data);
      } catch (error) {
        console.error("Error fetching products:", error);
        if (error.response?.status === 401) {
          navigate("/login");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [navigate, searchQuery]);
  // Fetching categories
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await axios.get(`${BASE_URL}/api/categories`);
        if (response.data && Array.isArray(response.data)) {
          // Add "All" to the beginning of the categories array
          setCategories([
            "All",
            ...response.data.map((category) => category.name),
          ]);
        } else {
          console.error(
            "Error: Categories data is missing or not an array",
            response.data
          );
        }
      } catch (error) {
        console.error("Error fetching categories:", error);
      }
    };

    fetchCategories();
  }, []);
  // const categories = [
  //   "All",
  //   "Electronics",
  //   "Furniture",
  //   "Clothing",
  //   "Books",
  //   "Sports",
  //   "Toys",
  //   "Health & Beauty",
  //   "Automotive",
  //   "Real Estate",
  //   "Others",
  // ];

  const filteredProducts = products.filter((product) => {
    const categoryMatch =
      selectedCategory === "All" ||
      product.categoryId === categories.indexOf(selectedCategory);
    const searchMatch =
      !searchQuery ||
      product.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      product.description.toLowerCase().includes(searchQuery.toLowerCase());
    const priceMatch =
      product.price >= priceRange.min &&
      (priceRange.max === Infinity || product.price <= priceRange.max);

    return categoryMatch && searchMatch && priceMatch;
  });

  const handlePriceChange = (newPriceRange) => {
    setPriceRange(newPriceRange);
    setCurrentPage(1); // Reset to first page when filter changes
  };

  const totalPages = Math.ceil(filteredProducts.length / itemsPerPage);
  const currentProducts = filteredProducts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handleToggleWishlist = async (productId) => {
    console.log(`Toggled wishlist for product ${productId}`);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="products-section">
      <div
        className="products-header"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "0 20px",
        }}
      >
        <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
          <CategoryDropdown
            categories={categories}
            selectedCategory={selectedCategory}
            onSelectCategory={setSelectedCategory}
          />
          <div style={{ width: "12px" }}></div>
          <PriceFilter onPriceChange={handlePriceChange} />
        </div>
        <div className="best-deals">
          <DollarSign className="dollar-icon" />
          <span>Best Deals</span>
        </div>
      </div>

      <div className="product-grid">
        {filteredProducts.length > 0 ? (
          currentProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              onToggleWishlist={handleToggleWishlist}
            />
          ))
        ) : (
          <div className="no-products-message">
            <p>No products found matching your criteria</p>
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      )}
    </div>
  );
};

const BuyingPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  // const [isDarkMode, setIsDarkMode] = useState(false);
  const [searchQuery, setSearchQuery] = useState(
    new URLSearchParams(location.search).get("keyword") || ""
  );

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    // const prefersDark = window.matchMedia(
    //   "(prefers-color-scheme: dark)"
    // ).matches;
    // setIsDarkMode(prefersDark);
  }, [navigate]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setSearchQuery(params.get("keyword") || "");
  }, [location.search]);

  return (
    <div className="content-wrapper">
      <ProductGrid searchQuery={searchQuery} />
    </div>
  );
};

export default BuyingPage;
