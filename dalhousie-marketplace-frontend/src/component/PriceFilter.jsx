import React, { useState } from 'react';
import { DollarSign } from 'lucide-react';

const PriceFilter = ({ onPriceChange }) => {
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [isOpen, setIsOpen] = useState(false);

  const handleApplyFilter = () => {
    const min = minPrice === '' ? 0 : parseFloat(minPrice);
    const max = maxPrice === '' ? Infinity : parseFloat(maxPrice);
    onPriceChange({ min, max });
    setIsOpen(false);
  };

  const handleClearFilter = () => {
    setMinPrice('');
    setMaxPrice('');
    onPriceChange({ min: 0, max: Infinity });
    setIsOpen(false);
  };

  return (
    <div className="price-filter">
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="price-filter-button"
      >
        <DollarSign className="icon" />
        <span>Price Filter</span>
      </button>

      {isOpen && (
        <div className="price-filter-popup">
          <div className="price-input-group">
            <label>Min Price ($)</label>
            <input
              type="number"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
              placeholder="0"
              min="0"
            />
          </div>
          
          <div className="price-input-group">
            <label>Max Price ($)</label>
            <input
              type="number"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              placeholder="No limit"
              min="0"
            />
          </div>

          <div className="price-filter-actions">
            <button
              onClick={handleApplyFilter}
              className="apply-button"
            >
              Apply
            </button>
            <button
              onClick={handleClearFilter}
              className="clear-button"
            >
              Clear
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default PriceFilter;