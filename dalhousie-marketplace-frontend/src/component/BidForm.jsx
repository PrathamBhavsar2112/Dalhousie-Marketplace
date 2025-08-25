import React, { useState, useEffect } from "react";
import { DollarSign, Send, AlertCircle } from "lucide-react";
import { BASE_URL } from "../constant_url";
import "../css/BidForm.css";
import { useDarkMode } from "../pages/DarkModeContext";

const BidForm = ({ listingId, startingBid, onBidPlaced, currentHighestBid }) => {
  const [bidAmount, setBidAmount] = useState("");
  const [bidMessage, setBidMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const { isDarkMode } = useDarkMode();
  const [minBidAmount, setMinBidAmount] = useState(0);

  useEffect(() => {
    // Set minimum bid amount based on current highest bid or starting bid
    const minimumBid = currentHighestBid > 0 
      ? (currentHighestBid + 0.01).toFixed(2)
      : startingBid;
    
    setMinBidAmount(minimumBid);
    
    // Pre-fill with minimum bid
    if (!bidAmount) {
      setBidAmount(minimumBid.toString());
    }
  }, [currentHighestBid, startingBid, bidAmount]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    setError("");
    setSuccess("");
    
    // Validate bid amount
    const bidValue = parseFloat(bidAmount);
    if (isNaN(bidValue) || bidValue <= 0) {
      setError("Please enter a valid bid amount");
      return;
    }
    
    if (bidValue < minBidAmount) {
      setError(`Your bid must be at least $${minBidAmount}`);
      return;
    }
    
    setLoading(true);
    
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");
      
      if (!token || !userId) {
        setError("You must be logged in to place a bid");
        setLoading(false);
        return;
      }
      
      const response = await fetch(`${BASE_URL}/api/bids/${listingId}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          amount: bidValue,
          message: bidMessage,
          userId: userId
        })
      });
      
      if (!response.ok) {
        throw new Error(await response.text() || "Failed to place bid");
      }
      
      const data = await response.json();
      
      setSuccess("Your bid has been placed successfully!");
      setBidMessage("");
      
      if (onBidPlaced) {
        onBidPlaced(data);
      }
    } catch (error) {
      console.error("Error placing bid:", error);
      setError(error.message || "Failed to place bid. Please try again.");
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className={`dalbid-form-container ${isDarkMode ? 'dalbid-dark' : ''}`}>
      <h3 className="dalbid-form-title">Place Your Bid</h3>
      
      <form onSubmit={handleSubmit} className="dalbid-form">
        <div className="dalbid-form-group">
          <label htmlFor="bidAmount">Bid Amount ($)</label>
          <div className="dalbid-input-container">
            <DollarSign className="dalbid-input-icon" />
            <input
              type="number"
              id="bidAmount"
              value={bidAmount}
              onChange={(e) => setBidAmount(e.target.value)}
              placeholder={`Minimum bid: $${minBidAmount}`}
              step="0.01"
              min={minBidAmount}
              required
              className="dalbid-input"
            />
          </div>
          
          <div className="dalbid-form-group">
            <label htmlFor="bidMessage">Message (Optional)</label>
            <textarea
              id="bidMessage"
              value={bidMessage}
              onChange={(e) => setBidMessage(e.target.value)}
              placeholder="Add a message with your bid..."
              className="dalbid-textarea"
              maxLength={200}
            />
            <small className="dalbid-char-count">
              {bidMessage.length}/200 characters
            </small>
          </div>
          
          {error && (
            <div className="dalbid-error-message">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}
          
          {success && (
            <div className="dalbid-success-message">
              <span>{success}</span>
            </div>
          )}
        </div>
        
        <button type="submit" className="dalbid-submit-button" disabled={loading}>
          {loading ? (
            <>
              <div className="dalbid-spinner"></div>
              Processing...
            </>
          ) : (
            <>
              <Send size={16} />
              Place Bid
            </>
          )}
        </button>
      </form>
      
      <div className="dalbid-bid-info">
        <p>
          <strong>Starting Bid:</strong> ${startingBid.toFixed(2)}
        </p>
        {currentHighestBid > 0 && (
          <p>
            <strong>Current Highest Bid:</strong> ${currentHighestBid.toFixed(2)}
          </p>
        )}
        <p className="dalbid-info-note">
          Your bid should be higher than the current highest bid.
        </p>
      </div>
      
      <div className="dalbid-rules">
        <h4>Bidding Rules:</h4>
        <ul>
          <li>All bids are final and cannot be retracted once submitted.</li>
          <li>The seller may accept your bid, reject it, or make a counter offer.</li>
          <li>If your bid is accepted, you will be required to complete the purchase.</li>
          <li>The seller may choose to accept a bid at any time or wait for more bids.</li>
        </ul>
      </div>
    </div>
  );
};

export default BidForm;