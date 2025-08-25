import React, { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { XCircle, ArrowLeft, ShoppingCart, RefreshCw } from "lucide-react";
import DalLogo from "../assets/Dalhousie Logo.svg";
import { BASE_URL } from "../constant_url";
import "../css/PaymentRedirect.css";

const PaymentCancelled = () => {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const updateOrderStatus = async () => {
      try {
        // Parse query parameters from URL
        const params = new URLSearchParams(location.search);
        const orderId = params.get("order_id");
        
        if (!orderId) {
          console.warn("Missing order ID in URL parameters");
          return;
        }

        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        // Update order status to cancelled
        const response = await fetch(`${BASE_URL}/api/orders/${orderId}/cancel`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          }
        });

        if (!response.ok) {
          console.warn("Could not update order status to cancelled");
        }
      } catch (err) {
        console.error("Error updating order status:", err);
      }
    };

    updateOrderStatus();
  }, [location, navigate]);

  // Function to return to cart with items restored
  const returnToCart = async () => {
    try {
      const params = new URLSearchParams(location.search);
      const orderId = params.get("order_id");
      
      if (!orderId) {
        navigate("/cart");
        return;
      }

      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");
      
      if (!token || !userId) {
        navigate("/login");
        return;
      }

      // Restore cart items from the cancelled order
      const response = await fetch(`${BASE_URL}/api/orders/${orderId}/restore-cart`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ userId }),
      });

      if (!response.ok) {
        console.warn("Could not restore cart items");
      }

      // Navigate back to cart
      navigate("/cart");
    } catch (err) {
      console.error("Error restoring cart:", err);
      navigate("/cart");
    }
  };

  return (
    <div className="payment-redirect-container">
      <div className="payment-redirect-card">
        <img src={DalLogo} alt="Dalhousie Logo" className="logo" />
        <div className="status-icon cancelled">
          <XCircle size={40} />
        </div>
        <h2>Payment Cancelled</h2>
        <p>Your payment was cancelled and no charges were made.</p>
        
        <div className="cancelled-info">
          <p>If you experienced any issues during checkout, please try again or contact our support team for assistance.</p>
        </div>
        
        <div className="action-buttons">
          <button className="secondary-button" onClick={() => navigate("/orders")}>
            <ArrowLeft className="button-icon" />
            View My Orders
          </button>
          <button className="retry-button" onClick={returnToCart}>
            <RefreshCw className="button-icon" />
            Return to Cart
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

export default PaymentCancelled;