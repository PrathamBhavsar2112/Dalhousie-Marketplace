import React from "react";
import "../css/BidIndicator.css";

/**
 * Simple component to show bid information on listing cards
 * Use this in product cards to indicate bidding functionality
 */
const BidIndicator = ({ biddingAllowed, startingBid, currentPrice }) => {
  if (!biddingAllowed) {
    return null;
  }

  return (
    <div className="bid-indicator">
      <div className="bid-indicator-label">Bidding Available</div>
      {startingBid && (
        <div className="bid-indicator-price">
          <span>Starting at:</span>
          <span className="starting-bid">${startingBid.toFixed(2)}</span>
        </div>
      )}
    </div>
  );
};

export default BidIndicator;