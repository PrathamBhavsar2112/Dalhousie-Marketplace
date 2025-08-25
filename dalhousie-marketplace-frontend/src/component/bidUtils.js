import { BASE_URL } from "../constant_url";

/**
 * Fetch current highest bid for a listing
 * @param {string|number} listingId - ID of the listing
 * @returns {Promise<number>} - The highest bid amount or 0 if no bids
 */
export const getHighestBid = async (listingId) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/listing/${listingId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch bids");
    }

    const bids = await response.json();
    
    if (bids && bids.length > 0) {
      // Find the highest bid amount
      return Math.max(...bids.map(bid => bid.amount));
    }
    
    return 0;
  } catch (error) {
    console.error("Error fetching highest bid:", error);
    return 0;
  }
};

/**
 * Count active bids for a listing
 * @param {string|number} listingId - ID of the listing
 * @returns {Promise<number>} - Count of active bids
 */
export const getBidCount = async (listingId) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/listing/${listingId}/count`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch bid count");
    }

    const data = await response.json();
    return data.count || 0;
  } catch (error) {
    console.error("Error fetching bid count:", error);
    return 0;
  }
};

/**
 * Submit a new bid
 * @param {string|number} listingId - ID of the listing
 * @param {number} amount - Bid amount
 * @param {string} message - Optional message with the bid
 * @returns {Promise<object>} - The created bid object
 */
export const submitBid = async (listingId, amount, message = "") => {
  try {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("userId");
    
    if (!token || !userId) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/${listingId}`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        amount,
        message,
        userId
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Failed to place bid");
    }

    return response.json();
  } catch (error) {
    console.error("Error submitting bid:", error);
    throw error;
  }
};

/**
 * Update a bid's status
 * @param {string|number} bidId - ID of the bid
 * @param {string} status - New status (ACCEPTED, REJECTED, etc.)
 * @returns {Promise<boolean>} - Success status
 */
export const updateBidStatus = async (bidId, status) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/${bidId}/status`, {
      method: "PUT",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ status })
    });

    if (!response.ok) {
      throw new Error(`Failed to update bid status to ${status}`);
    }

    return true;
  } catch (error) {
    console.error(`Error updating bid status to ${status}:`, error);
    throw error;
  }
};

/**
 * Submit a counter offer to a bid
 * @param {string|number} bidId - ID of the original bid
 * @param {number} amount - Counter offer amount
 * @returns {Promise<object>} - The updated bid object
 */
export const submitCounterOffer = async (bidId, amount) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/${bidId}/counter`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ amount })
    });

    if (!response.ok) {
      throw new Error("Failed to submit counter offer");
    }

    return response.json();
  } catch (error) {
    console.error("Error submitting counter offer:", error);
    throw error;
  }
};

/**
 * Finalize bidding on a listing (select winner)
 * @param {string|number} listingId - ID of the listing
 * @returns {Promise<boolean>} - Success status
 */
export const finalizeBidding = async (listingId) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/bids/listing/${listingId}/finalize`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });

    if (!response.ok) {
      throw new Error("Failed to finalize bidding");
    }

    return true;
  } catch (error) {
    console.error("Error finalizing bidding:", error);
    throw error;
  }
};

/**
 * Get all biddable listings
 * @returns {Promise<Array>} - List of biddable listings
 */
export const getBiddableListings = async () => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("Authentication required");
    }

    const response = await fetch(`${BASE_URL}/api/listings/biddable`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch biddable listings");
    }

    return response.json();
  } catch (error) {
    console.error("Error fetching biddable listings:", error);
    return [];
  }
};