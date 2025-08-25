import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Gavel, 
  Search, 
  Heart, 
  Clock, 
  DollarSign,
  ChevronDown,
  ChevronUp,
  Filter as FilterIcon
} from "lucide-react";
import "../css/BiddableListings.css";
import { BASE_URL } from "../constant_url";
import { useDarkMode } from "./DarkModeContext";

const BiddableCard = ({ listing }) => {
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [imageUrl, setImageUrl] = useState(null);
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();

  useEffect(() => {
    const fetchImage = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        // Fetch the image metadata
        const imagesResponse = await fetch(
          `${BASE_URL}/api/listings/${listing.id}/images`,
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
              setImageUrl(objectUrl);
            }
          }
        }

        // Check wishlist status
        // If needed, implement wishlist checking here
      } catch (error) {
        console.error("Error fetching image:", error);
      }
    };

    fetchImage();

    // Cleanup function
    return () => {
      if (imageUrl) {
        URL.revokeObjectURL(imageUrl);
      }
    };
  }, [listing.id, navigate, imageUrl]);

  const handleClick = () => {
    navigate(`/listing/${listing.id}`);
  };

  const formatTimeLeft = (endDate) => {
    if (!endDate) return "No end date";
    
    const end = new Date(endDate);
    const now = new Date();
    const diff = end - now;
    
    if (diff < 0) return "Ended";
    
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    
    if (days > 0) {
      return `${days}d ${hours}h left`;
    } else {
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      return `${hours}h ${minutes}m left`;
    }
  };

  return (
    <div 
      className={`dalbl-card ${isDarkMode ? "dalbl-dark" : ""}`} 
      onClick={handleClick}
    >
      <div className="dalbl-card-badge">
        <Gavel size={14} />
        <span>Biddable</span>
      </div>
      
      <div className="dalbl-image-container">
        <img 
          src={imageUrl || "/api/placeholder/400/320"} 
          alt={listing.title} 
          className="dalbl-image"
        />
        
        {listing.bidsCount > 0 && (
          <div className="dalbl-bid-count">
            <span>{listing.bidsCount} {listing.bidsCount === 1 ? 'bid' : 'bids'}</span>
          </div>
        )}
      </div>
      
      <div className="dalbl-content">
        <h3 className="dalbl-title">{listing.title}</h3>
        
        <div className="dalbl-price-row">
          <div className="dalbl-price">
            <DollarSign size={16} />
            <span>${listing.startingBid.toFixed(2)}</span>
          </div>
          
          {listing.endDate && (
            <div className="dalbl-time-left">
              <Clock size={14} />
              <span>{formatTimeLeft(listing.endDate)}</span>
            </div>
          )}
        </div>
        
        <p className="dalbl-seller">
          Seller: {listing.seller?.username || "Anonymous"}
        </p>
        
        <div className="dalbl-footer">
          <button className="dalbl-bid-button">Place Bid</button>
          <button className="dalbl-view-button">View Details</button>
        </div>
      </div>
    </div>
  );
};

// Sort and Filter controls
const SortControl = ({ sortBy, onSortChange }) => {
  const options = [
    { value: "newest", label: "Newest" },
    { value: "endingSoon", label: "Ending Soon" },
    { value: "startingBidLow", label: "Starting Bid: Low to High" },
    { value: "startingBidHigh", label: "Starting Bid: High to Low" },
    { value: "mostBids", label: "Most Bids" }
  ];

  const [isOpen, setIsOpen] = useState(false);
  const currentOption = options.find(opt => opt.value === sortBy) || options[0];

  return (
    <div className="dalbl-sort-control">
      <button 
        className="dalbl-sort-button"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span>Sort By: {currentOption.label}</span>
        {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      
      {isOpen && (
        <div className="dalbl-sort-dropdown">
          {options.map(option => (
            <button 
              key={option.value}
              className={`dalbl-sort-option ${option.value === sortBy ? 'dalbl-active' : ''}`}
              onClick={() => {
                onSortChange(option.value);
                setIsOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

const BiddableListings = () => {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState("newest");
  const [showFilters, setShowFilters] = useState(false);
  const [priceRange, setPriceRange] = useState({ min: 0, max: Infinity });
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();

  useEffect(() => {
    const fetchBiddableListings = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const response = await fetch(`${BASE_URL}/api/listings/biddable`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error("Failed to fetch biddable listings");
        }

        let data = await response.json();
        
        // Add additional bid count information
        const listingsWithBidCounts = await Promise.all(
          data.map(async (listing) => {
            try {
              const countResponse = await fetch(
                `${BASE_URL}/api/bids/listing/${listing.id}/count`,
                {
                  headers: {
                    Authorization: `Bearer ${token}`,
                  },
                }
              );
              
              if (countResponse.ok) {
                const countData = await countResponse.json();
                return {
                  ...listing,
                  bidsCount: countData.count || 0
                };
              }
              return { ...listing, bidsCount: 0 };
            } catch (error) {
              console.error("Error fetching bid count:", error);
              return { ...listing, bidsCount: 0 };
            }
          })
        );
        
        setListings(listingsWithBidCounts);
        setLoading(false);
      } catch (error) {
        console.error("Error fetching biddable listings:", error);
        setError(error.message);
        setLoading(false);
      }
    };

    fetchBiddableListings();
  }, [navigate]);

  const handleSearch = (e) => {
    if (e.key === 'Enter') {
      // Filter listings based on search query
      // Or navigate to search page with query parameter
    }
  };

  const filterListings = () => {
    let filtered = [...listings];
    
    // Apply search filter
    if (searchQuery) {
      filtered = filtered.filter(listing => 
        listing.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        listing.description.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }
    
    // Apply price range filter
    filtered = filtered.filter(listing => 
      listing.startingBid >= priceRange.min && 
      (priceRange.max === Infinity || listing.startingBid <= priceRange.max)
    );
    
    // Apply sorting
    filtered.sort((a, b) => {
      switch (sortBy) {
        case "newest":
          return new Date(b.createdAt) - new Date(a.createdAt);
        case "endingSoon":
          if (!a.endDate && !b.endDate) return 0;
          if (!a.endDate) return 1;
          if (!b.endDate) return -1;
          return new Date(a.endDate) - new Date(b.endDate);
        case "startingBidLow":
          return a.startingBid - b.startingBid;
        case "startingBidHigh":
          return b.startingBid - a.startingBid;
        case "mostBids":
          return b.bidsCount - a.bidsCount;
        default:
          return 0;
      }
    });
    
    return filtered;
  };

  const filteredListings = filterListings();

  if (loading) {
    return <div className="dalbl-loading">Loading biddable listings...</div>;
  }

  if (error) {
    return <div className="dalbl-error">Error: {error}</div>;
  }

  return (
    <div className={`dalbl-container ${isDarkMode ? "dalbl-dark" : ""}`}>
      <div className="dalbl-header">
        <h1 className="dalbl-main-title">
          <Gavel className="dalbl-main-icon" />
          Biddable Listings
        </h1>
        
        <div className="dalbl-search-container">
          <input 
            type="text"
            placeholder="Search biddable listings..."
            className="dalbl-search-input"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearch}
          />
          <Search className="dalbl-search-icon" />
        </div>
      </div>
      
      <div className="dalbl-filters-bar">
        <button 
          className="dalbl-filter-toggle"
          onClick={() => setShowFilters(!showFilters)}
        >
          <FilterIcon size={16} />
          {showFilters ? "Hide Filters" : "Show Filters"}
        </button>
        
        <SortControl sortBy={sortBy} onSortChange={setSortBy} />
        
        <div className="dalbl-result-count">
          {filteredListings.length} {filteredListings.length === 1 ? 'listing' : 'listings'}
        </div>
      </div>
      
      {showFilters && (
        <div className="dalbl-filter-panel">
          <div className="dalbl-price-filter">
            <h3>Price Range</h3>
            <div className="dalbl-range-inputs">
              <div className="dalbl-range-input">
                <label>Min ($)</label>
                <input 
                  type="number" 
                  min="0"
                  value={priceRange.min}
                  onChange={(e) => setPriceRange({...priceRange, min: parseFloat(e.target.value) || 0})}
                />
              </div>
              <div className="dalbl-range-input">
                <label>Max ($)</label>
                <input 
                  type="number"
                  min="0"
                  value={priceRange.max === Infinity ? "" : priceRange.max}
                  onChange={(e) => setPriceRange({
                    ...priceRange, 
                    max: e.target.value === "" ? Infinity : parseFloat(e.target.value) || 0
                  })}
                />
              </div>
            </div>
          </div>
          
          <button 
            className="dalbl-reset-filters"
            onClick={() => {
              setSearchQuery("");
              setPriceRange({ min: 0, max: Infinity });
            }}
          >
            Reset Filters
          </button>
        </div>
      )}
      
      {filteredListings.length === 0 ? (
        <div className="dalbl-no-results">
          <p>No biddable listings found matching your criteria.</p>
          <button 
            className="dalbl-back-button"
            onClick={() => {
              setSearchQuery("");
              setPriceRange({ min: 0, max: Infinity });
            }}
          >
            Clear Filters
          </button>
        </div>
      ) : (
        <div className="dalbl-grid">
          {filteredListings.map(listing => (
            <BiddableCard key={listing.id} listing={listing} />
          ))}
        </div>
      )}
    </div>
  );
};

export default BiddableListings;