import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { CheckCircle, ArrowLeft, ShoppingCart } from "lucide-react";
import DalLogo from "../assets/Dalhousie Logo.svg";
import { BASE_URL } from "../constant_url";
import "../css/PaymentRedirect.css";

const PaymentSuccess = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [orderDetails, setOrderDetails] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchOrderDetails = async () => {
      try {
        // Parse query parameters from URL
        const params = new URLSearchParams(location.search);
        const sessionId = params.get("session_id");
        const orderId = params.get("order_id");
        
        console.log("Session ID:", sessionId);
        console.log("Order ID:", orderId);
        
        // Handle cases where parameters might be missing
        if (!orderId) {
          // If we're missing order ID but have session ID, we can still show a success page
          if (sessionId) {
            setLoading(false);
            return;
          }
          throw new Error("Missing order information");
        }

        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        // Fetch the order details
        const response = await fetch(`${BASE_URL}/api/orders/${orderId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          console.error("Failed to fetch order details. Status:", response.status);
          
          // Even if we can't fetch order details, we don't need to show an error
          // as the payment was still successful
          setLoading(false);
          return;
        }

        const data = await response.json();
        console.log("Order details:", data);
        setOrderDetails(data);
        
        // Only try to update if we have both orderId and sessionId
        if (sessionId) {
          try {
            // Update the order status to completed if needed
            const updateResponse = await fetch(`${BASE_URL}/api/orders/${orderId}/complete`, {
              method: "POST",
              headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
              },
              body: JSON.stringify({ sessionId }),
            });

            if (!updateResponse.ok) {
              console.warn("Could not update order status, may be already updated by webhook");
            }
          } catch (updateErr) {
            console.error("Error updating order status:", updateErr);
            // Don't set an error for this - the payment was still successful
          }
        }

      } catch (err) {
        console.error("Error fetching order details:", err);
        // Don't immediately show error - we can still show a success page
        setError(err.message || "An error occurred");
      } finally {
        setLoading(false);
      }
    };

    fetchOrderDetails();
  }, [location, navigate]);

  // Format date to a readable string
  const formatDate = (dateString) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (err) {
      return new Date().toLocaleDateString() + ' ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  };

  if (loading) {
    return (
      <div className="payment-redirect-container">
        <div className="payment-redirect-card">
          <img src={DalLogo} alt="Dalhousie Logo" className="logo" />
          <div className="loading-spinner"></div>
          <h2>Processing your order...</h2>
          <p>Please wait while we confirm your payment.</p>
        </div>
      </div>
    );
  }

  // Only show error if it's critical
  if (error) {
    return (
      <div className="payment-redirect-container">
        <div className="payment-redirect-card">
          <img src={DalLogo} alt="Dalhousie Logo" className="logo" />
          <div className="status-icon error">!</div>
          <h2>Something went wrong</h2>
          <p>{error}</p>
          <div className="action-buttons">
            <button className="secondary-button" onClick={() => navigate("/orders")}>
              <ArrowLeft className="button-icon" />
              View My Orders
            </button>
            <button className="primary-button" onClick={() => navigate("/buying")}>
              <ShoppingCart className="button-icon" />
              Continue Shopping
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="payment-redirect-container">
      <div className="payment-redirect-card">
        <img src={DalLogo} alt="Dalhousie Logo" className="logo" />
        <div className="status-icon success">
          <CheckCircle size={40} />
        </div>
        <h2>Payment Successful!</h2>
        <p>Your order has been successfully processed.</p>
        
        {orderDetails && (
          <div className="order-summary">
            <h3>Order Summary</h3>
            <div className="summary-row">
              <span>Order ID:</span>
              <span>#{orderDetails.orderId || orderDetails.id || 'N/A'}</span>
            </div>
            <div className="summary-row">
              <span>Date:</span>
              <span>{formatDate(orderDetails.createdAt || orderDetails.orderDate || new Date())}</span>
            </div>
            <div className="summary-row">
              <span>Total Amount:</span>
              <span>${(orderDetails.amount || orderDetails.total || 0).toFixed(2)}</span>
            </div>
            <div className="summary-row">
              <span>Payment Method:</span>
              <span>{orderDetails.paymentMethod || 'Credit Card'}</span>
            </div>
          </div>
        )}
        
        {!orderDetails && (
          <div className="order-summary">
            <h3>Order Confirmation</h3>
            <p style={{ textAlign: 'center', padding: '10px' }}>
              Your payment has been processed successfully. You can view your order details in the Orders section.
            </p>
          </div>
        )}
        
        <div className="confirmation-message">
          <p>A confirmation email has been sent to your registered email address.</p>
        </div>
        
        <div className="action-buttons">
          <button className="secondary-button" onClick={() => navigate("/orders")}>
            <ArrowLeft className="button-icon" />
            View My Orders
          </button>
          <button className="primary-button" onClick={() => navigate("/buying")}>
            <ShoppingCart className="button-icon" />
            Continue Shopping
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentSuccess;