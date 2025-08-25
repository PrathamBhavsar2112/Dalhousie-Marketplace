import React, { useState, useEffect, useCallback } from "react";
import { Star } from "lucide-react";
import "../css/Reviews.css";
import { BASE_URL } from "../constant_url";

const ReviewComponent = ({ listingId, isAuthenticated, handleLoginPrompt }) => {
  const [reviews, setReviews] = useState([]);
  const [userReview, setUserReview] = useState("");
  const [userRating, setUserRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [hasUserReviewed, setHasUserReviewed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [eligibility, setEligibility] = useState(null);
  const [averageRating, setAverageRating] = useState(0);
  const [reviewCount, setReviewCount] = useState(0);

  const fetchReviews = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      
      const response = await fetch(`${BASE_URL}/api/reviews/listing/${listingId}`, {
        headers: token ? {
          Authorization: `Bearer ${token}`
        } : {}
      });

      if (response.ok) {
        const data = await response.json();
        setReviews(data.reviews || []);
        setAverageRating(data.averageRating || 0);
        setReviewCount(data.reviewCount || 0);
        
        // Check if the current user has already submitted a review
        if (isAuthenticated) {
          const userId = localStorage.getItem("userId");
          const userHasReviewed = (data.reviews || []).some(review => review.userId === Number(userId));
          setHasUserReviewed(userHasReviewed);
        }
      }
      setLoading(false);
    } catch (error) {
      console.error("Error fetching reviews:", error);
      setLoading(false);
    }
  }, [listingId, isAuthenticated]);

  const checkEligibility = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;

      const response = await fetch(`${BASE_URL}/api/reviews/eligibility/listing/${listingId}`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setEligibility(data);
      }
    } catch (error) {
      console.error("Error checking review eligibility:", error);
    }
  }, [listingId]);

  useEffect(() => {
    if (listingId) {
      fetchReviews();
      if (isAuthenticated) {
        checkEligibility();
      }
    }
  }, [listingId, isAuthenticated, fetchReviews, checkEligibility]);

  const handleSubmitReview = async (e) => {
    e.preventDefault();

    if (!isAuthenticated) {
      handleLoginPrompt();
      return;
    }

    if (userRating === 0) {
      alert("Please select a rating");
      return;
    }

    if (!userReview.trim()) {
      alert("Please write a review");
      return;
    }

    try {
      const token = localStorage.getItem("token");
      const orderItemId = eligibility?.orderItemId;
      
      const response = await fetch(`${BASE_URL}/api/reviews`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          orderItemId: orderItemId,
          listingId: Number(listingId),
          rating: userRating,
          reviewText: userReview.trim(),
        }),
      });

      if (response.ok) {
        alert("Review submitted successfully!");
        setUserReview("");
        setUserRating(0);
        fetchReviews(); // Refresh the reviews
      } else {
        const errorData = await response.json().catch(() => null);
        throw new Error(errorData?.message || "Failed to submit review");
      }
    } catch (error) {
      console.error("Error submitting review:", error);
      alert(error.message || "Failed to submit review. Please try again.");
    }
  };

  const StarRating = ({ rating, onRatingChange, interactable = false, hoveredRating = 0 }) => {
    return (
      <div className="review-star-rating">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            className={`review-star ${
              star <= (hoveredRating || rating) ? "review-star-filled" : ""
            } ${interactable ? "review-star-interactive" : ""}`}
            onClick={interactable ? () => onRatingChange(star) : undefined}
            onMouseEnter={interactable ? () => setHoveredRating(star) : undefined}
            onMouseLeave={interactable ? () => setHoveredRating(0) : undefined}
            fill={star <= (hoveredRating || rating) ? "#FFD700" : "none"}
            stroke={star <= (hoveredRating || rating) ? "#FFD700" : "#6b7280"}
          />
        ))}
      </div>
    );
  };

  const formatDate = (dateString) => {
    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return new Date(dateString).toLocaleDateString(undefined, options);
  };

  if (loading) {
    return <div className="reviews-loading">Loading reviews...</div>;
  }

  return (
    <div className="reviews-container">
      <div className="reviews-header">
        <h3 className="reviews-title">Product Reviews</h3>
        {reviewCount > 0 && (
          <div className="reviews-summary">
            <div className="review-average">
              <span className="average-rating">{averageRating.toFixed(1)}</span>
              <StarRating rating={Math.round(averageRating)} />
            </div>
            <span className="review-count">({reviewCount} {reviewCount === 1 ? 'review' : 'reviews'})</span>
          </div>
        )}
      </div>

      {reviews.length > 0 ? (
        <div className="reviews-list">
          {reviews.map((review) => (
            <div key={review.reviewId} className="review-item">
              <div className="review-header">
                <div className="reviewer-info">
                  <span className="reviewer-name">{review.username || "Anonymous"}</span>
                  <StarRating rating={review.rating} />
                </div>
                <span className="review-date">{formatDate(review.createdAt)}</span>
              </div>
              <p className="review-comment">{review.reviewText}</p>
            </div>
          ))}
        </div>
      ) : (
        <div className="no-reviews">No reviews yet. Be the first to review this product!</div>
      )}

      {isAuthenticated ? (
        hasUserReviewed ? (
          <div className="already-reviewed">
            You've already reviewed this product. Thank you for your feedback!
          </div>
        ) : eligibility?.eligible ? (
          <div className="review-form">
            <h4 className="write-review-title">Write a Review</h4>
            <form onSubmit={handleSubmitReview}>
              <div className="rating-selector">
                <label>Your Rating:</label>
                <StarRating 
                  rating={userRating} 
                  onRatingChange={setUserRating} 
                  interactable={true} 
                  hoveredRating={hoveredRating} 
                />
              </div>
              <textarea
                className="review-textarea"
                placeholder="Share your thoughts about this product..."
                value={userReview}
                onChange={(e) => setUserReview(e.target.value)}
              />
              <button type="submit" className="submit-review-button">
                Submit Review
              </button>
            </form>
          </div>
        ) : (
          <div className="purchase-required">
            You can only review products you've purchased. Purchase this item to leave a review.
          </div>
        )
      ) : (
        <div className="login-prompt">
          Please log in to write a review for this product.
        </div>
      )}
    </div>
  );
};

export default ReviewComponent;