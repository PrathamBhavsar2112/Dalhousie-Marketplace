import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Layout from "./pages/Layout"; 
import Home from "./pages/Home";
import Signup from "./pages/Signup";
import Login from "./pages/Login";
import Items from "./pages/Items";
import ResetPassword from './pages/ResetPassword';
import Dashboard from "./pages/Dashboard";
import SellingPage from "./pages/SellingPage";
import BuyingPage from "./pages/BuyingPage";
import ProfilePage from "./pages/ProfilePage";
import Messages from "./pages/Messages";
import Wishlist from "./pages/Wishlist";
// import Notifications from "./pages/Notifications";
import MarketplaceListing from "./pages/MarketplaceListing";
import Cart from "./pages/Cart";
import Orders from "./pages/Orders";
import PaymentSuccess from "./pages/PaymentSuccess";
import PaymentCancelled from "./pages/PaymentCancelled";
import { DarkModeProvider } from "./pages/DarkModeContext";
import NotificationSettings from "./pages/NotificationSettings";
import Bids from "./pages/Bids";



function App() {
  return (
    <DarkModeProvider>

    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/login" element={<Login />} />
        <Route path="/sellItems" element={<Items />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        <Route element={<Layout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/selling" element={<SellingPage />} />
        <Route path="/buying" element={<BuyingPage />} />
        <Route path="/profilepage" element={<ProfilePage />} />
        <Route path="/messages" element={<Messages />} />
        <Route path="/wishlist" element={<Wishlist />} />
        <Route path="/settings/notifications" element={<NotificationSettings />} /> 
        <Route path="/listing/:id" element={<MarketplaceListing />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/orders" element={<Orders />} />
        <Route path="/payment/success" element={<PaymentSuccess />} />
        <Route path="/payment/cancel" element={<PaymentCancelled />} />
        <Route path="/bids" element={<Bids/>} />
        </Route>
      </Routes>
    </Router>
    </DarkModeProvider>
  );
}

export default App;