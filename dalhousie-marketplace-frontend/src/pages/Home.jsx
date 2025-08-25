import React from "react";
import "../css/Home.css";
import { Link } from "react-router-dom";

function Home() {
  return (
    <main className="home-container">
      <header className="navbar">
        <h1 className="logo">Dalhousie Marketplace</h1>
      </header>

      <section className="content">
        <h2 className="tagline">Buy & Sell with Dalhousie Students</h2>
        <p className="description">
          Your trusted marketplace for trading books, electronics, furniture, and more.
        </p>
        <div className="cta-buttons">
          <Link to="/signup" className="button shop-button" aria-label="Start shopping">
            Start Shopping
          </Link>
          <Link to="/login" className="button sell-button" aria-label="Sell an item">
            Sell an Item
          </Link>
        </div>
      </section>

      
    </main>
  );
}

export default Home;
