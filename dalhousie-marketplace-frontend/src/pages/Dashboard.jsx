import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../css/Dashboard.css";
// import TrendingItems from "./TrendingItems";
// import Announcements from "./Announcements";
import { useDarkMode } from "../pages/DarkModeContext";
import { BASE_URL } from "../constant_url";
import {
  DollarSign,
  ShoppingBag,
  PackageCheck,
  Tag,
  PieChart,
  Gavel,
} from "lucide-react";

// Stats Card Component
const StatsCard = ({ title, value, icon, color, subtitle, onClick }) => {
  const { isDarkMode } = useDarkMode();

  return (
    <div
      className={`stats-card ${isDarkMode ? "dark" : ""}`}
      onClick={onClick}
      style={{ cursor: onClick ? "pointer" : "default" }}
    >
      <div className="stats-card-icon" style={{ backgroundColor: color }}>
        {icon}
      </div>
      <div className="stats-card-content">
        <h3>{title}</h3>
        <div className="stats-card-value">{value}</div>
        {subtitle && <div className="stats-card-subtitle">{subtitle}</div>}
      </div>
      {/* {onClick && (
        <div className="stats-card-arrow">
          <ArrowRight size={20} />
        </div>
      )} */}
    </div>
  );
};

// Dashboard Component
const Dashboard = () => {
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();
  const [sellerStats, setSellerStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    // Fetch seller stats
    const fetchSellerStats = async () => {
      try {
        const response = await fetch(`${BASE_URL}/api/profile/stats/seller`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          if (response.status === 401) {
            navigate("/login");
            return;
          }
          throw new Error("Failed to fetch seller stats");
        }

        const data = await response.json();
        setSellerStats(data);
      } catch (err) {
        console.error("Error fetching seller stats:", err);
        setError(err.message || "Failed to load your dashboard data");
      } finally {
        setLoading(false);
      }
    };

    fetchSellerStats();
  }, [navigate]);

  // Format currency
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
    }).format(amount);
  };

  return (
    <div className={`dashboard-content ${isDarkMode ? "dark" : ""}`}>
      <div className="dashboard-welcome-section">
        <h1>Welcome to Dalhousie Marketplace 🎉</h1>
        <p>Find great deals or sell your items easily!</p>
      </div>

      {loading ? (
        <div className="dashboard-loading">
          <div className="dashboard-loading-spinner"></div>
          <p>Loading your dashboard data...</p>
        </div>
      ) : error ? (
        <div className="dashboard-error">
          <p>{error}</p>
        </div>
      ) : sellerStats ? (
        <div className="dashboard-stats-section">
          <h2>Your Marketplace Stats</h2>

          <div className="dashboard-stats-grid">
            {/* Sales Stats */}
            <StatsCard
              title="Total Sales"
              value={formatCurrency(sellerStats.salesActivity.totalSales)}
              icon={<DollarSign size={24} />}
              color="#10b981"
              subtitle={`${sellerStats.salesActivity.itemsSold} items sold`}
              // onClick={() => navigate("/selling")}
            />

            {/* Listings Stats */}
            <StatsCard
              title="Active Listings"
              value={sellerStats.listingActivity.activeListings}
              icon={<Tag size={24} />}
              color="#6366f1"
              subtitle={`${sellerStats.listingActivity.totalListings} total listings`}
              // onClick={() => navigate("/selling")}
            />

            {/* Sold Items Stats */}
            <StatsCard
              title="Sold Items"
              value={sellerStats.listingActivity.soldListings}
              icon={<PackageCheck size={24} />}
              color="#f59e0b"
              subtitle="Successfully completed sales"
            />

            {/* Bid Stats */}
            <StatsCard
              title="Bid Sales"
              value={formatCurrency(sellerStats.salesActivity.bidSales)}
              icon={<Gavel size={24} />}
              color="#ec4899"
              subtitle={`${sellerStats.bidActivity.totalBidsReceived} total bids received`}
              // onClick={() => navigate("/mybids")}
            />
          </div>

          <div className="dashboard-stats-summary">
            <div className={`summary-card ${isDarkMode ? "dark" : ""}`}>
              <h3>Sales Breakdown</h3>
              <div className="summary-stats">
                <div className="summary-stat">
                  <span className="stat-label">Regular Sales:</span>
                  <span className="stat-value">
                    {formatCurrency(sellerStats.salesActivity.regularSales)}
                  </span>
                </div>
                <div className="summary-stat">
                  <span className="stat-label">Bid-based Sales:</span>
                  <span className="stat-value">
                    {formatCurrency(sellerStats.salesActivity.bidSales)}
                  </span>
                </div>
                <div className="summary-stat total">
                  <span className="stat-label">Total Revenue:</span>
                  <span className="stat-value">
                    {formatCurrency(sellerStats.salesActivity.totalSales)}
                  </span>
                </div>
              </div>
            </div>

            <div className={`summary-card ${isDarkMode ? "dark" : ""}`}>
              <h3>Activity Overview</h3>
              <div className="summary-stats">
                <div className="summary-stat">
                  <span className="stat-label">Active Bids:</span>
                  <span className="stat-value">
                    {sellerStats.bidActivity.activeBidsReceived}
                  </span>
                </div>
                <div className="summary-stat">
                  <span className="stat-label">Active Listings:</span>
                  <span className="stat-value">
                    {sellerStats.listingActivity.activeListings}
                  </span>
                </div>
                <div className="summary-stat">
                  <span className="stat-label">Sold Listings:</span>
                  <span className="stat-value">
                    {sellerStats.listingActivity.soldListings}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {/* <Announcements />

      <div className="dashboard-trending-section">
        <TrendingItems />
      </div> */}

      <div className="dashboard-actions">
        <button
          className="dashboard-action-button sell"
          onClick={() => navigate("/selling")}
        >
          <ShoppingBag size={24} />
          <span>Sell an Item</span>
        </button>

        <button
          className="dashboard-action-button browse"
          onClick={() => navigate("/buying")}
        >
          <PieChart size={24} />
          <span>Browse Marketplace</span>
        </button>

        <button
          className="dashboard-action-button bids"
          onClick={() => navigate("/bids")}
        >
          <Gavel size={24} />
          <span>Manage Bid Listings</span>
        </button>
      </div>
    </div>
  );
};

export default Dashboard;
