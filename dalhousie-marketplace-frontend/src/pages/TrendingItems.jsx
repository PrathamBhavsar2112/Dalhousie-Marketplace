import { useState } from "react";
import "../css/TrendingItems.css";

const TrendingItems = () => {
  const items = [
    // { id: 1, name: "iPhone 13 Pro", price: "$800", image: "https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/refurb-iphone-13-pro-max-gold-2023?wid=1144&hei=1144&fmt=jpeg&qlt=90&.v=1679072988850" },
    // { id: 2, name: "MacBook Air M1", price: "$950", image: "https://store.storeimages.cdn-apple.com/4982/as-images.apple.com/is/mba13-midnight-config-202402?wid=820&hei=498&fmt=jpeg&qlt=90&.v=1708371033110" },
    // { id: 3, name: "Gaming PC", price: "$1200", image: "https://m.media-amazon.com/images/I/71XnPsbbLPL.__AC_SY300_SX300_QL70_ML2_.jpg" },
    // { id: 4, name: "Sony Headphones", price: "$200", image: "https://m.media-amazon.com/images/I/71Q4Kk3zyeL._AC_SY550_.jpg" }
  ];

  const [selectedItem, setSelectedItem] = useState(null);
  const [bidAmount, setBidAmount] = useState("");

  const openBidModal = (item) => {
    setSelectedItem(item);
    setBidAmount("");
  };

  const submitBid = () => {
    alert(`Bid of $${bidAmount} placed on ${selectedItem.name}`);
    setSelectedItem(null);
  };

  return (
    <div className="trending-items">
      <h3>🔥 Trending Items</h3>
      <div className="items-grid">
        {items.map((item) => (
          <div key={item.id} className="item-card">
            <img src={item.image} alt={item.name} />
            <h4>{item.name}</h4>
            <p>{item.price}</p>
            <button className="bid-btn" onClick={() => openBidModal(item)}>💰 Place a Bid</button>
          </div>
        ))}
      </div>

      {selectedItem && (
        <div className="bid-modal">
          <div className="bid-content">
            <h3>Bid on {selectedItem.name}</h3>
            <img src={selectedItem.image} alt={selectedItem.name} className="modal-img" />
            <input
              type="number"
              placeholder="Enter bid amount"
              value={bidAmount}
              onChange={(e) => setBidAmount(e.target.value)}
            />
            <div className="modal-actions">
              <button onClick={submitBid}>Submit Bid</button>
              <button className="cancel-btn" onClick={() => setSelectedItem(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TrendingItems;
