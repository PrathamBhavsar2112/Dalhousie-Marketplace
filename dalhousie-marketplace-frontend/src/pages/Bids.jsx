import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { 
  Gavel, 
  Clock, 
  AlertCircle, 
  Eye, 
  Star, 
  Calendar, 
  Search, 
  ChevronUp, 
  ChevronDown,
  CheckCircle,
  XCircle,
  ArrowRight,
  Package,
  ChevronRight 
} from "lucide-react";
import { BASE_URL } from "../constant_url";
import { useDarkMode } from "./DarkModeContext";
import "../css/Bids.css";

const Bids = () => {
  const [activeTab, setActiveTab] = useState("my-bids"); // "my-bids" or "my-listings"
  const [myBids, setMyBids] = useState([]);
  const [myListings, setMyListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortOrder, setSortOrder] = useState("desc");
  const { isDarkMode } = useDarkMode();
  const navigate = useNavigate();

  // Fetch user data based on active tab
  useEffect(() => {
    const fetchData = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        setLoading(true);
        setError(null);

        if (activeTab === "my-bids" || activeTab === "all") {
          // Fetch bids placed by the user
          const bidsResponse = await fetch(`${BASE_URL}/api/bids/user`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });

          if (!bidsResponse.ok) {
            if (bidsResponse.status === 401) {
              navigate("/login");
              return;
            }
            throw new Error("Failed to fetch your bids");
          }

          const bidsData = await bidsResponse.json();
          setMyBids(bidsData);
        }

        if (activeTab === "my-listings" || activeTab === "all") {
          // Fetch listings with bidding enabled
          const listingsResponse = await fetch(`${BASE_URL}/api/listings/bidding/my-listings`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });

          if (!listingsResponse.ok) {
            if (listingsResponse.status === 401) {
              navigate("/login");
              return;
            }
            throw new Error("Failed to fetch your bid listings");
          }

          const listingsData = await listingsResponse.json();
          setMyListings(listingsData);
        }
      } catch (err) {
        console.error("Error fetching data:", err);
        setError(err.message || "Failed to load data");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [activeTab, navigate]);

  // Handler for bid checkout
  const handleBidCheckout = async (bidId) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        navigate("/login");
        return;
      }

      setLoading(true);

      const response = await fetch(`${BASE_URL}/api/bids/${bidId}/pay`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        
        if (data.checkoutUrl) {
          // Redirect to Stripe checkout
          window.location.href = data.checkoutUrl;
        } else {
          throw new Error("No checkout URL returned from server");
        }
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to initiate checkout");
      }
    } catch (error) {
      setError(error.message || "An error occurred during checkout");
      console.error("Checkout error:", error);
      setLoading(false);
    }
  };

  // Handle sorting
  const handleSort = (field) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortBy(field);
      setSortOrder("desc");
    }
  };

  // Format date
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // Status icon for bids
  const getStatusIcon = (status) => {
    switch (status) {
      case "PENDING":
        return <Clock className="bid-status-icon pending" />;
      case "ACCEPTED":
        return <CheckCircle className="bid-status-icon accepted" />;
      case "REJECTED":
        return <XCircle className="bid-status-icon rejected" />;
      default:
        return <AlertCircle className="bid-status-icon" />;
    }
  };

  // Get bid count for a listing (placeholder for demo)
  const getBidCount = (listingId) => {
    return Math.floor(Math.random() * 10); // Placeholder
  };

  // Filter data based on search query and apply sorting
  const getFilteredSortedData = (data, type) => {
    if (!data || data.length === 0) return [];

    // Apply search filter
    let filtered = data;
    
    if (searchQuery.trim() !== "") {
      if (type === "bids") {
        filtered = data.filter(bid => 
          bid.listing.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
          bid.additionalTerms?.toLowerCase().includes(searchQuery.toLowerCase())
        );
      } else { // listings
        filtered = data.filter(listing => 
          listing.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
          listing.description.toLowerCase().includes(searchQuery.toLowerCase())
        );
      }
    }

    // Apply sorting
    return [...filtered].sort((a, b) => {
      let comparison = 0;
      
      if (type === "bids") {
        switch(sortBy) {
          case "proposedPrice":
            comparison = a.proposedPrice - b.proposedPrice;
            break;
          case "title":
            comparison = a.listing.title.localeCompare(b.listing.title);
            break;
          case "status":
            comparison = a.status.localeCompare(b.status);
            break;
          case "createdAt":
          default:
            comparison = new Date(a.timestamp) - new Date(b.timestamp);
            break;
        }
      } else { // listings
        switch(sortBy) {
          case "title":
            comparison = a.title.localeCompare(b.title);
            break;
          case "price":
            comparison = a.price - b.price;
            break;
          case "startingBid":
            comparison = a.startingBid - b.startingBid;
            break;
          case "views":
            comparison = a.views - b.views;
            break;
          case "createdAt":
          default:
            comparison = new Date(a.createdAt) - new Date(b.createdAt);
            break;
        }
      }
      
      return sortOrder === "asc" ? comparison : -comparison;
    });
  };

  // Apply filters and sorting
  const filteredBids = getFilteredSortedData(myBids, "bids");
  const filteredListings = getFilteredSortedData(myListings, "listings");

  // Render loading state
  if (loading) {
    return (
      <div className={`bids-center-loading ${isDarkMode ? "dark" : ""}`}>
        <div className="loading-spinner"></div>
        <p>Loading your bidding data...</p>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className={`bids-center-error ${isDarkMode ? "dark" : ""}`}>
        <AlertCircle size={24} />
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div className={`bids-center-container ${isDarkMode ? "dark" : ""}`}>
      <div className="bids-center-header">
        <h2>Bids</h2>
        <p>Manage all your bidding activity in one place</p>
      </div>

      {/* Tabs */}
      <div className="bids-center-tabs">
        <button 
          className={`tab ${activeTab === "my-bids" ? "active" : ""}`}
          onClick={() => setActiveTab("my-bids")}
        >
          <Gavel size={18} />
          My Bids ({myBids.length})
        </button>
        <button 
          className={`tab ${activeTab === "my-listings" ? "active" : ""}`}
          onClick={() => setActiveTab("my-listings")}
        >
          <Package size={18} />
          My Listings with Bidding ({myListings.length})
        </button>
      </div>

      {/* Controls */}
      <div className="bids-center-controls">
        <div className="search-container">
          <input
            type="text"
            placeholder={`Search ${activeTab === "my-bids" ? "bids" : "listings"}...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
          <Search className="search-icon" />
        </div>

        <div className="sort-controls">
          <span>Sort by:</span>
          {activeTab === "my-bids" ? (
            // Sort controls for bids
            <>
              <button 
                className={`sort-button ${sortBy === "createdAt" ? "active" : ""}`}
                onClick={() => handleSort("createdAt")}
              >
                Date
                {sortBy === "createdAt" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
              <button 
                className={`sort-button ${sortBy === "proposedPrice" ? "active" : ""}`}
                onClick={() => handleSort("proposedPrice")}
              >
                Price
                {sortBy === "proposedPrice" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
              <button 
                className={`sort-button ${sortBy === "status" ? "active" : ""}`}
                onClick={() => handleSort("status")}
              >
                Status
                {sortBy === "status" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
            </>
          ) : (
            // Sort controls for listings
            <>
              <button 
                className={`sort-button ${sortBy === "createdAt" ? "active" : ""}`}
                onClick={() => handleSort("createdAt")}
              >
                Date
                {sortBy === "createdAt" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
              <button 
                className={`sort-button ${sortBy === "price" ? "active" : ""}`}
                onClick={() => handleSort("price")}
              >
                Price
                {sortBy === "price" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
              <button 
                className={`sort-button ${sortBy === "views" ? "active" : ""}`}
                onClick={() => handleSort("views")}
              >
                Views
                {sortBy === "views" && (
                  sortOrder === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                )}
              </button>
            </>
          )}
        </div>
      </div>

      {/* My Bids Tab Content */}
      {activeTab === "my-bids" && (
        <>
          {filteredBids.length === 0 ? (
            <div className="empty-content">
              <Gavel size={48} className="empty-icon" />
              <h3>No Bids Yet</h3>
              <p>You haven't placed any bids on listings yet.</p>
              <button 
                className="primary-action-button"
                onClick={() => navigate("/buying")}
              >
                Browse Listings
              </button>
            </div>
          ) : (
            <div className="bids-grid">
              {filteredBids.map((bid) => (
                <div key={bid.id} className={`bid-card status-${bid.status.toLowerCase()}`}>
                  <div className="bid-header">
                    <div className="bid-status">
                      {getStatusIcon(bid.status)}
                      <span className={`status-text ${bid.status.toLowerCase()}`}>
                        {bid.status}
                      </span>
                    </div>
                    {/* <span className="bid-date">{formatDate(bid.timestamp)}</span> */}
                  </div>

                  <h3 
                    className="bid-listing-title" 
                    onClick={() => navigate(`/listing/${bid.listing.id}`)}
                  >
                    {bid.listing.title}
                  </h3>

                  <div className="bid-details">
                    <div className="bid-info-row">
                      <span className="bid-info-label">Your Bid:</span>
                      <span className="bid-info-value bid-price">${bid.proposedPrice.toFixed(2)}</span>
                    </div>
                    
                    <div className="bid-info-row">
                      <span className="bid-info-label">Listing Price:</span>
                      <span className="bid-info-value">${bid.listing.price.toFixed(2)}</span>
                    </div>
                    
                    {bid.additionalTerms && (
                      <div className="bid-terms">
                        <span className="bid-info-label">Your Terms:</span>
                        <p className="bid-terms-text">{bid.additionalTerms}</p>
                      </div>
                    )}
                  </div>

                  <div className="bid-actions">
                    <button 
                      className="view-listing-button" 
                      onClick={() => navigate(`/listing/${bid.listing.id}`)}
                    >
                      View Listing
                      <ArrowRight size={16} />
                    </button>
                    
                    {bid.status === "ACCEPTED" && (
                      <button 
                        className="checkout-button" 
                        onClick={() => handleBidCheckout(bid.id)}
                      >
                        Checkout
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* My Listings Tab Content */}
      {activeTab === "my-listings" && (
        <>
          {filteredListings.length === 0 ? (
            <div className="empty-content">
              <Package size={48} className="empty-icon" />
              <h3>No Bid Listings Found</h3>
              <p>You don't have any active listings with bidding enabled.</p>
              <button 
                className="primary-action-button"
                onClick={() => navigate("/sellItems")}
              >
                Create Listing with Bidding
              </button>
            </div>
          ) : (
            <div className="listings-grid">
              {filteredListings.map((listing) => (
                <div key={listing.id} className="listing-card">
                  <div className="listing-header">
                    <div className="bid-tag">
                      <Gavel size={14} />
                      <span>Bidding Enabled</span>
                    </div>
                    <div className="view-count">
                      <Eye size={14} />
                      <span>{listing.views}</span>
                    </div>
                  </div>

                  <h3 
                    className="listing-title" 
                    onClick={() => navigate(`/listing/${listing.id}`)}
                  >
                    {listing.title}
                  </h3>

                  <p className="listing-description">{listing.description}</p>

                  <div className="listing-details">
                    <div className="price-details">
                      <div className="listing-price">
                        <span className="detail-label">Listing Price:</span>
                        <span className="price-value">${listing.price.toFixed(2)}</span>
                      </div>
                      <div className="starting-bid">
                        <span className="detail-label">Starting Bid:</span>
                        <span className="bid-value">${listing.startingBid.toFixed(2)}</span>
                      </div>
                    </div>

                    <div className="bid-status">
                      <div className="bid-count">
                        <span className="detail-label">Bids Received:</span>
                        <span className="count-value">{getBidCount(listing.id)}</span>
                      </div>
                      <div className="listing-date">
                        <Calendar size={14} />
                        <span>{formatDate(listing.createdAt)}</span>
                      </div>
                    </div>

                    {listing.averageRating && (
                      <div className="rating">
                        <Star size={14} className="star-icon" />
                        <span>{listing.averageRating.toFixed(1)}</span>
                        <span className="review-count">({listing.reviewCount} reviews)</span>
                      </div>
                    )}
                  </div>

                  <div className="listing-actions">
                    <button 
                      className="manage-bids-button"
                      onClick={() => navigate(`/listing/${listing.id}`)}
                    >
                      View & Manage Bids
                      <ChevronRight size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Bids;