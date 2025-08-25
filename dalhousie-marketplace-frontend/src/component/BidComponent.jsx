import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { BASE_URL } from "../constant_url";
import "../css/BidComponent.css";

/**
 * Component that handles bidding functionality for a listing
 * Used by buyers to place bids on listings with bidding enabled
 */
const BidComponent = ({ listingId, listingTitle, startingBid, currentPrice }) => {
  const [proposedPrice, setProposedPrice] = useState("");
  const [additionalTerms, setAdditionalTerms] = useState("");
  const [isBidding, setIsBidding] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [userBid, setUserBid] = useState(null);
  
  const navigate = useNavigate();

  // Check if the user has already placed a bid on this listing
  useEffect(() => {
    const checkExistingBid = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          return;
        }

        const response = await fetch(`${BASE_URL}/api/bids/user`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.ok) {
          const bids = await response.json();
          const existingBid = bids.find(bid => bid.listingId === parseInt(listingId));
          if (existingBid) {
            setUserBid(existingBid);
            // Pre-fill form with existing bid details for reference
            setProposedPrice(existingBid.proposedPrice);
          }
        }
      } catch (error) {
        console.error("Error checking existing bids:", error);
      }
    };

    checkExistingBid();
  }, [listingId]);

  // Handle checkout for accepted bids
  const handleBidCheckout = async (bidId) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        navigate("/login");
        return;
      }

      setIsBidding(true);
      setErrorMessage("");

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
      setErrorMessage(error.message || "An error occurred during checkout");
      console.error("Checkout error:", error);
    } finally {
      setIsBidding(false);
    }
  };

  // Handle bid submission
  const handleSubmitBid = async (e) => {
    e.preventDefault();
    
    // Form validation
    if (!proposedPrice || parseFloat(proposedPrice) <= 0) {
      setErrorMessage("Please enter a valid bid amount");
      return;
    }

    // Starting bid validation
    if (startingBid && parseFloat(proposedPrice) < parseFloat(startingBid)) {
      setErrorMessage(`Your bid must be at least the starting bid amount (${startingBid})`);
      return;
    }

    // Ensure user is logged in
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login", { state: { from: `/listing/${listingId}` } });
      return;
    }

    setIsBidding(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const response = await fetch(`${BASE_URL}/api/bids/${listingId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          proposedPrice: parseFloat(proposedPrice),
          additionalTerms: additionalTerms.trim() || null,
        }),
      });

      if (response.ok) {
        const bidData = await response.json();
        setUserBid(bidData);
        setSuccessMessage("Your bid has been submitted successfully!");
        
        // Optionally refresh the page after a short delay
        setTimeout(() => {
          window.location.reload();
        }, 2000);
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to place bid");
      }
    } catch (error) {
      setErrorMessage(error.message || "An error occurred while placing your bid");
    } finally {
      setIsBidding(false);
    }
  };

  // Render bid status UI if user has already placed a bid
  const renderBidStatus = () => {
    if (!userBid) return null;

    const statusColors = {
      PENDING: "bid-status-pending",
      ACCEPTED: "bid-status-accepted",
      REJECTED: "bid-status-rejected",
      COUNTERED: "bid-status-countered"
    };

    return (
      <div className={`bid-status-container ${statusColors[userBid.status] || ""}`}>
        <h4>Your Bid Status: {userBid.status}</h4>
        <p><strong>Bid Amount:</strong> ${userBid.proposedPrice.toFixed(2)}</p>
        {userBid.additionalTerms && (
          <p><strong>Additional Terms:</strong> {userBid.additionalTerms}</p>
        )}
        {userBid.status === "PENDING" && (
          <p className="bid-waiting-message">Waiting for seller response...</p>
        )}
        {userBid.status === "ACCEPTED" && (
          <div className="bid-success-message">
            <p>Congratulations! Your bid has been accepted.</p>
            <button 
              className="proceed-to-checkout-button"
              onClick={() => handleBidCheckout(userBid.id)}
            >
              Proceed to Checkout
            </button>
          </div>
        )}
        {userBid.status === "REJECTED" && (
          <p className="bid-rejected-message">Your bid was not accepted. You may place a new bid below.</p>
        )}
      </div>
    );
  };

  // Don't render the form if the bid is accepted
  const shouldShowBidForm = !userBid || userBid.status !== "ACCEPTED";

  return (
    <div className="bid-component">
      <h3>Make an Offer</h3>
      
      {renderBidStatus()}
      
      {shouldShowBidForm && (
        <form onSubmit={handleSubmitBid} className="bid-form">
          <div className="bid-form-group">
            <label htmlFor="proposedPrice">Your Offer ($):</label>
            <input
              type="number"
              id="proposedPrice"
              name="proposedPrice"
              value={proposedPrice}
              onChange={(e) => setProposedPrice(e.target.value)}
              min={startingBid || "0.01"}
              step="0.01"
              placeholder={`Starting bid: $${startingBid || currentPrice}`}
              required
            />
          </div>

          <div className="bid-form-group">
            <label htmlFor="additionalTerms">Additional Terms (optional):</label>
            <textarea
              id="additionalTerms"
              name="additionalTerms"
              value={additionalTerms}
              onChange={(e) => setAdditionalTerms(e.target.value)}
              placeholder="E.g., Pickup details, questions about the item, etc."
              rows="3"
            />
          </div>

          {errorMessage && <div className="bid-error-message">{errorMessage}</div>}
          {successMessage && <div className="bid-success-message">{successMessage}</div>}

          <button
            type="submit"
            className="submit-bid-button"
            disabled={isBidding}
          >
            {isBidding ? "Submitting..." : "Submit Offer"}
          </button>
        </form>
      )}
      
      <div className="bid-info">
        <p>
          By placing a bid, you're making an offer to purchase this item. The seller
          can accept, reject, or counter your offer. You'll be notified of any updates.
        </p>
      </div>
    </div>
  );
};

export default BidComponent;