import React, { useState } from "react";
import { Calendar, DollarSign, Clock, HelpCircle } from "lucide-react";
import "./EnhancedBiddingOptions.css";
import { useDarkMode } from "../pages/DarkModeContext";

const EnhancedBiddingOptions = ({ formData, onChange }) => {
  const { isDarkMode } = useDarkMode();
  const [showHelp, setShowHelp] = useState(false);
  
  // Calculate minimum end date (1 day from now)
  const minEndDate = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0]; // Format as YYYY-MM-DD
  };
  
  // Calculate maximum end date (30 days from now)
  const maxEndDate = () => {
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 30);
    return futureDate.toISOString().split('T')[0]; // Format as YYYY-MM-DD
  };
  
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    
    // For radio buttons
    if (type === "radio") {
      onChange({
        ...formData,
        [name]: value
      });
      
      // If bidding is disabled, clear bidding-related fields
      if (name === "biddingAllowed" && value === "no") {
        onChange({
          ...formData,
          biddingAllowed: "no",
          startingBid: "",
          bidIncrement: "",
          bidEndDate: "",
          allowAutoBidding: false,
          autoAcceptThreshold: ""
        });
      }
    }
    // For checkboxes
    else if (type === "checkbox") {
      onChange({
        ...formData,
        [name]: checked
      });
      
      // If auto-bidding is disabled, clear the threshold
      if (name === "allowAutoBidding" && !checked) {
        onChange({
          ...formData,
          [name]: checked,
          autoAcceptThreshold: ""
        });
      }
    } 
    // For all other inputs
    else {
      onChange({
        ...formData,
        [name]: value
      });
    }
  };
  
  return (
    <div className={`ebid-container ${isDarkMode ? "ebid-dark" : ""}`}>
      <div className="ebid-section-header">
        <h3>Bidding Options</h3>
        <button 
          type="button"
          className="ebid-help-button"
          onClick={() => setShowHelp(!showHelp)}
          aria-label="Toggle bidding help"
        >
          <HelpCircle size={16} />
        </button>
      </div>
      
      {showHelp && (
        <div className="ebid-help-box">
          <h4>About Bidding</h4>
          <p>
            Enabling bidding allows buyers to place bids on your item instead of purchasing it directly.
            You can set a starting bid, minimum bid increment, and an end date for the bidding period.
          </p>
          <p>
            <strong>Auto Accept Threshold:</strong> If enabled, bids above this amount will be automatically accepted.
          </p>
          <button 
            type="button"
            className="ebid-close-help"
            onClick={() => setShowHelp(false)}
          >
            Close
          </button>
        </div>
      )}
      
      <div className="ebid-options">
        <div className="ebid-radio-group">
          <label className="ebid-radio-option">
            <input
              type="radio"
              name="biddingAllowed"
              value="yes"
              checked={formData.biddingAllowed === "yes"}
              onChange={handleChange}
            />
            <span>Enable Bidding</span>
          </label>
          
          <label className="ebid-radio-option">
            <input
              type="radio"
              name="biddingAllowed"
              value="no"
              checked={formData.biddingAllowed === "no"}
              onChange={handleChange}
            />
            <span>Disable Bidding (Fixed Price)</span>
          </label>
        </div>
        
        {formData.biddingAllowed === "yes" && (
          <div className="ebid-bid-settings">
            <div className="ebid-form-row">
              <div className="ebid-form-group">
                <label>
                  <DollarSign size={16} />
                  Starting Bid ($)
                </label>
                <input
                  type="number"
                  name="startingBid"
                  value={formData.startingBid}
                  onChange={handleChange}
                  min="0.01"
                  step="0.01"
                  required
                  placeholder="Minimum starting bid"
                />
              </div>
              
              <div className="ebid-form-group">
                <label>
                  <DollarSign size={16} />
                  Minimum Bid Increment ($)
                </label>
                <input
                  type="number"
                  name="bidIncrement"
                  value={formData.bidIncrement}
                  onChange={handleChange}
                  min="0.01"
                  step="0.01"
                  placeholder="Optional"
                />
              </div>
            </div>
            
            <div className="ebid-form-row">
              <div className="ebid-form-group">
                <label>
                  <Calendar size={16} />
                  Bidding End Date
                </label>
                <input
                  type="date"
                  name="bidEndDate"
                  value={formData.bidEndDate}
                  onChange={handleChange}
                  min={minEndDate()}
                  max={maxEndDate()}
                  required
                />
              </div>
              
              <div className="ebid-form-group">
                <label>
                  <Clock size={16} />
                  End Time
                </label>
                <input
                  type="time"
                  name="bidEndTime"
                  value={formData.bidEndTime}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
            
            <div className="ebid-checkbox-group">
              <label className="ebid-checkbox">
                <input
                  type="checkbox"
                  name="allowAutoBidding"
                  checked={formData.allowAutoBidding}
                  onChange={handleChange}
                />
                <span>Enable Auto-Accept for High Bids</span>
              </label>
              
              {formData.allowAutoBidding && (
                <div className="ebid-form-group ebid-threshold">
                  <label>Auto-Accept Threshold ($)</label>
                  <input
                    type="number"
                    name="autoAcceptThreshold"
                    value={formData.autoAcceptThreshold}
                    onChange={handleChange}
                    min="0.01"
                    step="0.01"
                    placeholder="Bids above this amount will be automatically accepted"
                  />
                </div>
              )}
            </div>
            
            <div className="ebid-note">
              <p>
                <strong>Note:</strong> Once bidding is enabled and your listing is published, you'll be able to review, accept, or reject bids from your seller dashboard.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EnhancedBiddingOptions;