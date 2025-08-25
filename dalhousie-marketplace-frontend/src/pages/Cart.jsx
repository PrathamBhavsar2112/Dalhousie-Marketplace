import React, { useState, useEffect, useCallback } from "react";
import { useNavigate} from "react-router-dom";
import {
  Trash2,
  Plus,
  Minus,
} from "lucide-react";
import "../css/Cart.css";

import { BASE_URL } from "../constant_url";
import { useDarkMode } from "../pages/DarkModeContext";


const CartItem = ({ item, onUpdateQuantity, onRemove }) => {

  const [imageUrl, setImageUrl] = useState(null);
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();


  useEffect(() => {
    const fetchImage = async () => {
      try {
        const token = localStorage.getItem('token');
        if (!token) {
          navigate('/login');
          return;
        }


        const imagesResponse = await fetch(
          `${BASE_URL}/api/listings/${item.listing.id}/images`,
          {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          }
        );

        if (imagesResponse.ok) {
          const imagesData = await imagesResponse.json();

          const primaryImage = imagesData.find(img => img.isPrimary) || imagesData[0];

          if (primaryImage) {
            const imageResponse = await fetch(
              `${BASE_URL}${primaryImage.url}`,
              {
                headers: {
                  'Authorization': `Bearer ${token}`
                }
              }
            );

            if (imageResponse.ok) {
              const blob = await imageResponse.blob();
              const objectUrl = URL.createObjectURL(blob);
              setImageUrl(objectUrl);
            }
          }
        }
      } catch (error) {
        console.error('Error fetching image:', error);
        if (error.response?.status === 401) {
          navigate('/login');
        }
      }
    };

    fetchImage();

    return () => {
      if (imageUrl) {
        URL.revokeObjectURL(imageUrl);
      }
    };
  }, [item.listing.id, navigate, imageUrl]);

  return (
    <div className={`cart-item ${isDarkMode ? "dark" : ""}`}>
      <img
        src={imageUrl || "/api/placeholder/400/320"}
        alt={item.listing.title}
        className="cart-item-image"
      />
      <div className="cart-item-details">
        <h3>{item.listing.title}</h3>
        <p className="seller">
          Seller: {item.listing.seller?.username || "Anonymous"}
        </p>
        <p className="condition">Condition: {item.listing.status || "N/A"}</p>
      </div>
      <div className="cart-item-actions">
        <div className="quantity-controls">
          <button
            onClick={() => onUpdateQuantity(item.listing.id, item.quantity - 1)}
          >
            <Minus className="w-4 h-4" />
          </button>
          <span>{item.quantity}</span>
          <button
            onClick={() => onUpdateQuantity(item.listing.id, item.quantity + 1)}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
        <p className="price">${(item.price * item.quantity).toFixed(2)}</p>
        <button
          className="remove-button"
          onClick={() => onRemove(item.listing.id)}
        >
          <Trash2 className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};

const Cart = () => {
  const navigate = useNavigate();
  
  const { isDarkMode } = useDarkMode();
  const [cartData, setCartData] = useState({
    cartId: null,
    userId: null,
    cartItems: [],
    totalPrice: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isProcessingCheckout, setIsProcessingCheckout] = useState(false);

  const fetchCartItems = useCallback(async () => {
    try {
      const userId = localStorage.getItem("userId");
      const token = localStorage.getItem("token");

      if (!userId || !token) {
        navigate("/login");
        return;
      }

      const response = await fetch(`${BASE_URL}/api/cart/${userId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/login");
          return;
        }
        throw new Error("Failed to fetch cart items");
      }

      const data = await response.json();
      setCartData(data);
      setLoading(false);
    } catch (err) {
      setError(err.message || "Error fetching cart items");
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    fetchCartItems();
  }, [fetchCartItems]);

  const updateQuantity = async (cartItemId, newQuantity) => {
    if (newQuantity < 1) return;

    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      const response = await fetch(
        `${BASE_URL}/api/cart/${userId}/items/${cartItemId}?quantity=${newQuantity}`,
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/login");
          return;
        }
        throw new Error("Failed to update quantity");
      }

      const updatedCart = await response.json();
      setCartData(updatedCart);
    } catch (err) {
      setError(err.message || "Error updating quantity");
    }
  };

  const removeItem = async (itemListingId) => {
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      const response = await fetch(
        // item.listing.id
        `${BASE_URL}/api/cart/${userId}/items/${itemListingId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          navigate("/login");
          return;
        }
        throw new Error("Failed to remove item");
      }

      const updatedCart = await response.json();
      setCartData(updatedCart);
    } catch (err) {
      setError(err.message || "Error removing item");
    }
  };


  const handleCheckout = async () => {
    if (cartData.cartItems.length === 0) {
      alert("Your cart is empty");
      return;
    }

    try {
      setIsProcessingCheckout(true);
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      if (!token || !userId) {
        navigate("/login");
        return;
      }

      // Step 1: Convert cart to order
      const orderResponse = await fetch(`${BASE_URL}/api/orders/cart/${userId}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      if (!orderResponse.ok) {
        throw new Error("Failed to create order");
      }

      const orderData = await orderResponse.json();
      console.log("Order created:", orderData);

      // Step 2: Initiate payment with the new order ID
      const paymentResponse = await fetch(`${BASE_URL}/api/orders/${orderData.orderId}/pay`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      if (!paymentResponse.ok) {
        throw new Error("Failed to initiate payment");
      }

      const paymentData = await paymentResponse.json();
      console.log("Payment initiated:", paymentData);

      // Step 3: Redirect to the Stripe checkout URL
      if (paymentData.checkoutUrl) {
        window.location.href = paymentData.checkoutUrl;
      } else {
        throw new Error("No checkout URL received");
      }

    } catch (err) {
      console.error("Checkout error:", err);
      alert(err.message || "Error during checkout. Please try again.");
    } finally {
      setIsProcessingCheckout(false);
    }
  };

  const subtotal = cartData.totalPrice || 0;
  // Removed tax calculation
  const total = subtotal; // Total is now just the subtotal

  if (loading) {
    return <div>Loading cart...</div>;
  }

  if (error) {
    return <div className="error-message">{error}</div>;
  }

  return (
    <div className={`cart-content ${isDarkMode ? "dark" : ""}`}>
          <div className="cart-container">

            <h1>Shopping Cart ({cartData.cartItems.length} items)</h1>

            <div className={`cart-grid ${isDarkMode ? "dark" : ""}`}>

              <div className="cart-items">
                {cartData.cartItems.map((item) => (
                  <CartItem
                    key={item.cartItemId}
                    item={item}
                    onUpdateQuantity={updateQuantity}
                    onRemove={removeItem}
                    isDarkMode={isDarkMode}
                  />
                ))}
              </div>

              {/* <div className={`cart-summary ${isDarkMode ? "dark" : ""}`}> */}
                
              <div className={`cart-summary ${isDarkMode ? "dark" : ""}`}>
                <h2>Order Summary</h2>
                <div className="summary-row">
                  <span>Subtotal</span>
                  <span>${subtotal.toFixed(2)}</span>
                </div>
                <div className="summary-row total">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
                <button
                  className="checkout-button"
                  onClick={handleCheckout}
                  disabled={isProcessingCheckout || cartData.cartItems.length === 0}
                >
                  {isProcessingCheckout ? "Processing..." : "Proceed to Checkout"}
                </button>
              </div>
            </div>
          </div>
        </div>
  );
};

export default Cart;
