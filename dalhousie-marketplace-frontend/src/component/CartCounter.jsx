import React, { useState, useEffect, useCallback } from 'react';
import { ShoppingCart } from 'lucide-react';
import { BASE_URL } from "../constant_url";

const CartCounter = ({ onClick, onCartUpdate }) => {
  const [cartCount, setCartCount] = useState(0);

  const fetchCartCount = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const userId = localStorage.getItem('userId');

      if (!token || !userId) {
        return;
      }

      const response = await fetch(`${BASE_URL}/api/cart/${userId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const cartData = await response.json();
        const itemCount = cartData.cartItems ? cartData.cartItems.length : 0;
        setCartCount(itemCount);
        if (onCartUpdate) {
          onCartUpdate(itemCount);
        }
      }
    } catch (error) {
      console.error('Error fetching cart count:', error);
    }
  }, [onCartUpdate]);

  useEffect(() => {
    fetchCartCount();
    const interval = setInterval(fetchCartCount, 5000);
    return () => clearInterval(interval);
  }, [fetchCartCount]);

  useEffect(() => {
    const handleCartUpdate = () => {
      fetchCartCount();
    };

    window.addEventListener('cartUpdate', handleCartUpdate);
    window.addEventListener('storage', handleCartUpdate);

    return () => {
      window.removeEventListener('cartUpdate', handleCartUpdate);
      window.removeEventListener('storage', handleCartUpdate);
    };
  }, [fetchCartCount]);

  return (
    <div className="relative cart-counter" style={{ display: 'inline-block' }}>
      <button 
        onClick={onClick}
        className="icon-button"
        style={{ position: 'relative' }}
      >
        <ShoppingCart className="icon" />
        {cartCount > 0 && (
          <div 
            style={{
              position: 'absolute',
              top: '-8px',
              left: '55%',
              transform: 'translateX(-50%)',
              backgroundColor: '#EF4444',
              color: 'white',
              borderRadius: '9999px',
              width: '20px',
              height: '20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '12px',
              fontWeight: 'bold'
            }}
          >
            {cartCount > 99 ? '99+' : cartCount}
          </div>
        )}
      </button>
    </div>
  );
};

export default CartCounter;