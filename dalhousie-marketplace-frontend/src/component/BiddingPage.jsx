import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  LayoutGrid,
  FileText,
  Heart,
  User,
  Settings,
  LogOut,
  ShoppingBag,
  Bell,
  Moon,
  Sun,
  Search,
  ArrowLeft,
  DollarSign
} from "lucide-react";
import DalLogo from "../assets/Dalhousie Logo.svg";
import smillingWoman from "../assets/smillingWoman.jpg";
import { BASE_URL } from "../constant_url";
import CartCounter from "./CartCounter";
import "../css/BiddingPage.css";

const BiddingPage = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [listing, setListing] = useState(null);
  const [image, setImage] = useState(null);
  const [bidAmount, setBidAmount] = useState("");
  const [currentHighestBid, setCurrentHighestBid] = useState(0);
  const [bidHistory, setBidHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [bidError, setBidError] = useState("");
  const [bidSuccess, setBidSuccess] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("token");
    
    if (!token) {
      navigate("/login");
      return;
    }

    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    setIsDarkMode(prefersDark);
  }, [navigate]);

  useEffect(() => {
    // Apply dark mode to body
    if (isDarkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
    
    const appContainer = document.querySelector(".bidding-app-container");
    if (isDarkMode) {
      appContainer?.classList.add("dark");
    } else {
      appContainer?.classList.remove("dark");
    }
  }, [isDarkMode]);

  const fetchListingAndBids = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        navigate("/login");
        return;
      }

      // Fetch listing details
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

      // Check if bidding is allowed
      if (!listingData.biddingAllowed) {
        navigate(`/listing/${id}`);
        return;
      }

      // Set initial current highest bid
      setCurrentHighestBid(listingData.startingBid || 0);

      // Fetch primary image
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
      const primaryImage = imagesMetadata.find(img => img.isPrimary) || imagesMetadata[0];
      
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
          setImage(URL.createObjectURL(blob));
        }
      }

      // Fetch bid history - wrapped in try/catch in case endpoint doesn't exist yet
      try {
        const bidsResponse = await fetch(`${BASE_URL}/api/bids/listing/${id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (bidsResponse.ok) {
          const bidsData = await bidsResponse.json();
          setBidHistory(bidsData);
          
          // Set current highest bid
          if (bidsData.length > 0) {
            const highestBid = Math.max(...bidsData.map(bid => bid.amount));
            setCurrentHighestBid(highestBid);
          }
        }
      } catch (bidError) {
        console.log("Bid history endpoint may not exist yet:", bidError);
        
        // Placeholder bid history for development
        const mockBids = [
          { id: 1, userId: "user1", username: "JohnDoe", amount: listingData.startingBid + 10, createdAt: new Date(Date.now() - 86400000).toISOString() },
          { id: 2, userId: "user2", username: "JaneSmith", amount: listingData.startingBid + 20, createdAt: new Date(Date.now() - 43200000).toISOString() },
          { id: 3, userId: "user3", username: "MikeJohnson", amount: listingData.startingBid + 30, createdAt: new Date(Date.now() - 3600000).toISOString() },
        ];
        setBidHistory(mockBids);
        setCurrentHighestBid(Math.max(...mockBids.map(bid => bid.amount)));
      }

      setLoading(false);
    } catch (error) {
      console.error("Error fetching data:", error);
      setError(error.message);
      setLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    if (id) {
      fetchListingAndBids();
    }

    return () => {
      // Cleanup object URL
      if (image) {
        URL.revokeObjectURL(image);
      }
    };
  }, [id, fetchListingAndBids, image]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    navigate("/login");
  };

  const handleBidSubmit = async (e) => {
    e.preventDefault();
    setBidError("");
    setBidSuccess("");
    
    // Parse the bid amount to a number
    const bidValue = parseFloat(bidAmount);
    
    // Validate the bid amount
    if (isNaN(bidValue) || bidValue <= 0) {
      setBidError("Please enter a valid bid amount.");
      return;
    }
    
    if (bidValue <= currentHighestBid) {
      setBidError(`Your bid must be higher than the current highest bid: $${currentHighestBid.toFixed(2)}`);
      return;
    }
    
    try {
      const token = localStorage.getItem("token");
      
      if (!token) {
        navigate("/login");
        return;
      }
      
      // Now using a fetch API call with proper headers and JSON
      const url = `${BASE_URL}/api/bids/${id}`;
      
      // The exact format expected by Spring Boot's @RequestBody
      // Including proper Content-Type header and stringifying the object
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          proposedPrice: bidValue  // Exactly matching the Spring Boot entity field
        })
      });
      
      if (response.ok) {
        const data = await response.json();
        setBidSuccess("Your bid has been placed successfully!");
        setCurrentHighestBid(bidValue);
        
        // Add the new bid to history
        const newBid = data || {
          id: Date.now(),
          userId: localStorage.getItem("userId"),
          username: "You", 
          amount: bidValue,
          createdAt: new Date().toISOString()
        };
        
        setBidHistory([newBid, ...bidHistory]);
        setBidAmount("");
      } else {
        const errorData = await response.text();
        console.error("Error from server:", errorData);
        
        try {
          const parsedError = JSON.parse(errorData);
          setBidError(parsedError.message || "Failed to place bid. Please try again.");
        } catch (e) {
          setBidError("Failed to place bid. Please try again.");
        }
      }
    } catch (error) {
      console.error("Error placing bid:", error);
      setBidError("Failed to place bid. Please try again.");
    }
  };

  const formatDate = (dateString) => {
    const options = { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    };
    return new Date(dateString).toLocaleDateString(undefined, options);
  };

  if (loading) return <div className="bidding-loading">Loading...</div>;
  if (error) return <div className="bidding-error">Error: {error}</div>;
  if (!listing) return <div className="bidding-error">Listing not found</div>;

  return (
    <div className={`bidding-app-container ${isDarkMode ? "dark" : ""}`}>
      {/* Left Sidebar */}
      <div className="bidding-sidebar">
        <img 
          src={DalLogo} 
          alt="Logo" 
          className="bidding-logo" 
          onClick={() => navigate("/dashboard")} 
        />
        <div className="bidding-sidebar-icons">
          <button onClick={() => navigate("/dashboard")} className="bidding-icon-button">
            <LayoutGrid className="bidding-icon" />
          </button>
          <button onClick={() => navigate("/buying")} className="bidding-icon-button">
            <ShoppingBag className="bidding-icon" />
          </button>
          <button onClick={() => navigate("/selling")} className="bidding-icon-button">
            <FileText className="bidding-icon" />
          </button>
          <button onClick={() => navigate("/wishlist")} className="bidding-icon-button">
            <Heart className="bidding-icon" />
          </button>
          <button onClick={() => navigate("/profilepage")} className="bidding-icon-button">
            <User className="bidding-icon" />
          </button>
          <button onClick={() => navigate("/settings")} className="bidding-icon-button">
            <Settings className="bidding-icon" />
          </button>
          <button
            className="bidding-logout-icon bidding-icon-button"
            onClick={handleLogout}
          >
            <LogOut className="bidding-icon" />
          </button>
        </div>
      </div>

      <div className="bidding-main-content">
        <div className="bidding-top-bar">
          <div className="bidding-search-container">
            <input
              type="text"
              placeholder="Search Here"
              className="bidding-search-input"
            />
            <Search className="bidding-search-icon" />
          </div>
          <div className="bidding-top-bar-icons">
            <CartCounter onClick={() => navigate("/cart")} />
            <button onClick={() => navigate("/notifications")} className="bidding-icon-button">
              <Bell className="bidding-icon" />
            </button>
            <button
              onClick={() => setIsDarkMode(!isDarkMode)}
              className="bidding-icon-button dark-mode-icon"
            >
              {isDarkMode ? <Sun className="bidding-icon" /> : <Moon className="bidding-icon" />}
            </button>
            <img src={smillingWoman} alt="Profile" className="bidding-profile-image" />
          </div>
        </div>

        <div className="bidding-content-wrapper">
          <div className="bidding-header">
            <button 
              className="bidding-back-button" 
              onClick={() => navigate(`/listing/${id}`)}
            >
              <ArrowLeft size={20} />
              <span>Back to Listing</span>
            </button>
            <h1 className="bidding-title">Place a Bid on {listing.title}</h1>
          </div>

          <div className="bidding-container">
            {/* Item Overview */}
            <div className="bidding-item-overview">
              <div className="bidding-image-container">
                <img 
                  src={image || "/api/placeholder/400/320"} 
                  alt={listing.title} 
                  className="bidding-item-image" 
                />
              </div>
              <div className="bidding-item-details">
                <h2 className="bidding-item-title">{listing.title}</h2>
                <div className="bidding-item-info">
                  <div className="bidding-info-row">
                    <span className="bidding-info-label">Starting Bid:</span>
                    <span className="bidding-info-value">${listing.startingBid?.toFixed(2) || "N/A"}</span>
                  </div>
                  <div className="bidding-info-row">
                    <span className="bidding-info-label">Current Highest Bid:</span>
                    <span className="bidding-info-value bidding-highlight">${currentHighestBid.toFixed(2)}</span>
                  </div>
                  <div className="bidding-info-row">
                    <span className="bidding-info-label">Seller:</span>
                    <span className="bidding-info-value">{listing.sellerName || "Anonymous"}</span>
                  </div>
                  <div className="bidding-info-row">
                    <span className="bidding-info-label">Listed:</span>
                    <span className="bidding-info-value">{new Date(listing.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Bidding Form */}
            <div className="bidding-form-section">
              <h3 className="bidding-section-title">Place Your Bid</h3>
              <form onSubmit={handleBidSubmit} className="bidding-form">
                <div className="bidding-form-group">
                  <label htmlFor="bidAmount">Your Bid Amount ($)</label>
                  <div className="bidding-input-container">
                    <DollarSign className="bidding-input-icon" />
                    <input
                      type="number"
                      id="bidAmount"
                      value={bidAmount}
                      onChange={(e) => setBidAmount(e.target.value)}
                      placeholder={`Minimum bid: $${(currentHighestBid + 0.01).toFixed(2)}`}
                      step="0.01"
                      min={currentHighestBid + 0.01}
                      required
                    />
                  </div>
                  {bidError && <p className="bidding-error-message">{bidError}</p>}
                  {bidSuccess && <p className="bidding-success-message">{bidSuccess}</p>}
                </div>
                <button type="submit" className="bidding-submit-button">
                  Place Bid
                </button>
              </form>
            </div>

            {/* Bid History */}
            <div className="bidding-history-section">
              <h3 className="bidding-section-title">Bid History</h3>
              {bidHistory.length > 0 ? (
                <div className="bidding-history">
                  <table className="bidding-history-table">
                    <thead>
                      <tr>
                        <th>Bidder</th>
                        <th>Amount</th>
                        <th>Date & Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bidHistory.map((bid) => (
                        <tr key={bid.id}>
                          <td>{bid.username}</td>
                          <td>${bid.amount.toFixed(2)}</td>
                          <td>{formatDate(bid.createdAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="bidding-no-history">No bids yet. Be the first to bid!</p>
              )}
            </div>

            {/* Bidding Rules */}
            <div className="bidding-rules">
              <h3 className="bidding-section-title">Bidding Rules</h3>
              <ul className="bidding-rules-list">
                <li>Your bid must be higher than the current highest bid.</li>
                <li>All bids are final and cannot be retracted.</li>
                <li>The highest bidder at the end of the auction will win the item.</li>
                <li>In case of a tie, the earlier bid will be considered the winner.</li>
                <li>The seller reserves the right to cancel the auction at any time.</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BiddingPage;