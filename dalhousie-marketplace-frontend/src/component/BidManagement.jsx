import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Check, X, AlertCircle, ArrowUp, ArrowDown } from "lucide-react";
import { BASE_URL } from "../constant_url";
import "../css/BidManagement.css";

/**
 * Component for sellers to manage bids on their listings
 * Shows all received bids and allows accepting/rejecting/finalizing
 */
const BidManagement = ({ listingId, listingTitle, isSeller }) => {
  const [bids, setBids] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortOrder, setSortOrder] = useState("desc");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const navigate = useNavigate();

  // Fetch all bids for this listing
  useEffect(() => {
    const fetchBids = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const response = await fetch(`${BASE_URL}/api/bids/listing/${listingId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          if (response.status === 403) {
            setError("You don't have permission to view these bids");
          } else if (response.status === 404) {
            setError("No bids found for this listing");
          } else {
            throw new Error("Failed to fetch bids");
          }
          setBids([]);
        } else {
          const data = await response.json();
          console.log("Fetched bids:", data); // Debug log
          setBids(data);
        }
      } catch (err) {
        console.error("Error fetching bids:", err);
        setError(err.message || "Failed to load bids");
      } finally {
        setLoading(false);
      }
    };

    if (isSeller && listingId) {
      fetchBids();
    }
  }, [listingId, navigate, isSeller]);

  // Handle sorting of bids
  const getSortedBids = () => {
    // First apply the status filter
    let filteredBids = [...bids];
    if (statusFilter !== "ALL") {
      filteredBids = filteredBids.filter(bid => bid.status === statusFilter);
    }

    // Then sort the filtered bids
    return filteredBids.sort((a, b) => {
      let comparison = 0;
      
      if (sortBy === "proposedPrice") {
        comparison = a.proposedPrice - b.proposedPrice;
      } else if (sortBy === "createdAt") {
        comparison = new Date(a.createdAt) - new Date(b.createdAt);
      } else if (sortBy === "status") {
        comparison = a.status.localeCompare(b.status);
      } else if (sortBy === "bidder") {
        // Handle potential null/undefined values
        const usernameA = a.buyer?.username || "";
        const usernameB = b.buyer?.username || "";
        comparison = usernameA.localeCompare(usernameB);
      }
      
      return sortOrder === "asc" ? comparison : -comparison;
    });
  };

  // Handle accepting a bid
  const handleAcceptBid = async (bidId) => {
    if (actionInProgress) return;
    
    if (!window.confirm("Are you sure you want to accept this bid? All other bids will be automatically rejected, and the listing will be marked as inactive.")) {
      return;
    }
    
    setActionInProgress(true);

    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${BASE_URL}/api/bids/${bidId}/accept`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const data = await response.json();
        
        // Update the bid status in the UI
        setBids(prevBids =>
          prevBids.map(bid =>
            bid.id === bidId
              ? { ...bid, status: "ACCEPTED", orderId: data.bid.orderId }
              : { ...bid, status: bid.status === "PENDING" ? "REJECTED" : bid.status }
          )
        );
        
        alert("Bid accepted successfully! The buyer can now proceed to checkout.");
        
        // Refresh the listing page to show the updated status
        window.location.reload();
      } else {
        const data = await response.json();
        throw new Error(data.message || "Failed to accept bid");
      }
    } catch (err) {
      console.error("Error accepting bid:", err);
      alert(err.message || "An error occurred while accepting the bid");
    } finally {
      setActionInProgress(false);
    }
  };

  // Handle rejecting a bid
  const handleRejectBid = async (bidId) => {
    if (actionInProgress) return;
    
    if (!window.confirm("Are you sure you want to reject this bid?")) {
      return;
    }
    
    setActionInProgress(true);

    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${BASE_URL}/api/bids/${bidId}/reject`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        await response.json(); // Still parse but don't assign to unused variable
        
        // Update the bid status in the UI
        setBids(prevBids =>
          prevBids.map(bid =>
            bid.id === bidId ? { ...bid, status: "REJECTED" } : bid
          )
        );
        alert("Bid rejected successfully");
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to reject bid");
      }
    } catch (err) {
      console.error("Error rejecting bid:", err);
      alert(err.message || "An error occurred while rejecting the bid");
    } finally {
      setActionInProgress(false);
    }
  };

  // Handle finalizing the bidding process
  const handleFinalizeBidding = async () => {
    if (actionInProgress) return;
    
    const pendingBids = bids.filter(bid => bid.status === "PENDING");
    if (pendingBids.length === 0) {
      alert("There are no pending bids to finalize");
      return;
    }

    if (!window.confirm("Are you sure you want to finalize the bidding? This will automatically accept the highest bid and reject all others.")) {
      return;
    }

    setActionInProgress(true);

    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${BASE_URL}/api/bids/listing/${listingId}/finalize`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const result = await response.json();
        
        // Check if the response contains the expected data
        if (result && result.acceptedBid && result.acceptedBid.proposedPrice) {
          alert(`Bidding finalized! The highest bid of $${result.acceptedBid.proposedPrice} was accepted.`);
        } else {
          // Handle the case where the response structure isn't as expected
          alert("Bidding finalized successfully! Refreshing bid data...");
          console.log("Finalize response:", result);
        }
        
        // Refresh bids after finalizing
        const refreshResponse = await fetch(`${BASE_URL}/api/bids/listing/${listingId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        
        if (refreshResponse.ok) {
          const refreshedBids = await refreshResponse.json();
          setBids(refreshedBids);
        }
        
        // Reload the page to reflect the changes
        window.location.reload();
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to finalize bidding");
      }
    } catch (err) {
      console.error("Error finalizing bidding:", err);
      alert(err.message || "An error occurred while finalizing the bidding");
    } finally {
      setActionInProgress(false);
    }
  };

  // Handle sorting changes
  const handleSort = (field) => {
    if (sortBy === field) {
      // Toggle sort order if clicking on the same field
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      // Set new sort field and default to descending
      setSortBy(field);
      setSortOrder("desc");
    }
  };

  // Format date to be more readable
  const formatDate = (dateString) => {
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return "Invalid Date";
      }
      return date.toLocaleString();
    } catch (error) {
      console.error("Error formatting date:", error);
      return "Invalid Date";
    }
  };

  if (!isSeller) {
    return null;
  }

  if (loading) {
    return <div className="bid-management-loading">Loading bids...</div>;
  }

  if (error) {
    return <div className="bid-management-error">{error}</div>;
  }

  const sortedBids = getSortedBids();

  return (
    <div className="bid-management">
      <div className="bid-management-header">
        <h3>Manage Bids for "{listingTitle}"</h3>
        <div className="bid-management-actions">
          <button 
            className="finalize-bidding-button"
            onClick={handleFinalizeBidding}
            disabled={actionInProgress || bids.filter(b => b.status === "PENDING").length === 0}
          >
            Finalize Bidding
          </button>
          <div className="bid-filter">
            <label htmlFor="status-filter">Filter by Status:</label>
            <select 
              id="status-filter" 
              value={statusFilter} 
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Bids</option>
              <option value="PENDING">Pending</option>
              <option value="ACCEPTED">Accepted</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
        </div>
      </div>

      {sortedBids.length === 0 ? (
        <div className="no-bids-message">
          <AlertCircle size={24} />
          <p>No bids {statusFilter !== "ALL" ? `with status ${statusFilter}` : ""} have been placed on this listing yet.</p>
        </div>
      ) : (
        <div className="bids-table-container">
          <table className="bids-table">
            <thead>
              <tr>
                <th className="sortable" onClick={() => handleSort("bidder")}>
                  Bidder
                  {sortBy === "bidder" && (
                    sortOrder === "asc" ? <ArrowUp size={16} /> : <ArrowDown size={16} />
                  )}
                </th>
                <th className="sortable" onClick={() => handleSort("proposedPrice")}>
                  Offered Price
                  {sortBy === "proposedPrice" && (
                    sortOrder === "asc" ? <ArrowUp size={16} /> : <ArrowDown size={16} />
                  )}
                </th>
                <th>Additional Terms</th>
                <th className="sortable" onClick={() => handleSort("createdAt")}>
                  Date
                  {sortBy === "createdAt" && (
                    sortOrder === "asc" ? <ArrowUp size={16} /> : <ArrowDown size={16} />
                  )}
                </th>
                <th className="sortable status-column" onClick={() => handleSort("status")}>
                  Status
                  {sortBy === "status" && (
                    sortOrder === "asc" ? <ArrowUp size={16} /> : <ArrowDown size={16} />
                  )}
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedBids.map((bid) => (
                <tr key={bid.id} className={`status-${bid.status.toLowerCase()}`}>
                  <td>{bid.buyer?.username || "Anonymous"}</td>
                  <td className="price-cell">${bid.proposedPrice.toFixed(2)}</td>
                  <td className="terms-cell">{bid.additionalTerms || "—"}</td>
                  <td>{formatDate(bid.createdAt)}</td>
                  <td className={`status status-${bid.status.toLowerCase()}`}>
                    {bid.status}
                    {bid.status === "PENDING" && (
                      <div className="inline-actions">
                        <button
                          className="accept-bid-button"
                          onClick={() => handleAcceptBid(bid.id)}
                          disabled={actionInProgress}
                          title="Accept Bid"
                        >
                          <Check size={16} />
                        </button>
                        <button
                          className="reject-bid-button"
                          onClick={() => handleRejectBid(bid.id)}
                          disabled={actionInProgress}
                          title="Reject Bid"
                        >
                          <X size={16} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BidManagement;