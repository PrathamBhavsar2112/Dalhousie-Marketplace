import React, { useState, useEffect } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import {
  // LayoutGrid,
  FileText,
  Heart,
  User,
  ShoppingBag,
  Settings,
  LogOut,
  Bell,
  Moon,
  Sun,
  Search,
  Receipt,
  Gavel,
} from "lucide-react";
import "../css/Dashboard.css";
import "../css/Notification.css";
import DalLogo from "../assets/Dalhousie Logo.svg";
// import smillingWoman from "../assets/smillingWoman.jpg";
import CartCounter from "../component/CartCounter";
import WebSocketService from "../services/WebSocketService";
import { BASE_URL } from "../constant_url";
import { useDarkMode } from "./DarkModeContext";

const IconButton = ({ Icon, onClick, notificationCount = 0 }) => {
  const { isDarkMode } = useDarkMode();

  return (
    <button
      onClick={onClick}
      className={`dashboard-icon-button ${isDarkMode ? "dark" : ""}`}
    >
      <Icon className="dashboard-icon" />
      {notificationCount > 0 && (
        <span className="notification-badge">{notificationCount}</span>
      )}
    </button>
  );
};


const Layout = () => {
  const navigate = useNavigate();
  const { isDarkMode, setIsDarkMode } = useDarkMode();
  const [searchQuery, setSearchQuery] = useState("");
  const [notifications, setNotifications] = useState([]);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const userId = localStorage.getItem("userId");
  const username = localStorage.getItem("username");
  console.log("username:",username);

  useEffect(() => {
    document.body.classList.toggle("dark", isDarkMode);
  }, [isDarkMode]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }
    //setIsDarkMode(window.matchMedia("(prefers-color-scheme: dark)").matches);
    // Fetch existing unread notifications
    console.log("BASE_URL:", BASE_URL);
    console.log("userId:", userId);

    fetch(`${BASE_URL}/api/notifications/${userId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Fetched Notifications:", data);
        if (Array.isArray(data)) {
          setNotifications(data);
          const unreadCount = data.filter((notif) => !notif.read).length;
          setUnreadNotifications(unreadCount);
        } else {
          console.error("Notifications API returned non-array data:", data);
          setNotifications([]); // Set empty array to prevent errors
        }
      });

    // Connect WebSocket for real-time updates
    WebSocketService.connect(userId, (newNotification) => {
      setNotifications((prev) => [newNotification, ...prev]);
      if (!newNotification.read) {
        setUnreadNotifications((prev) => prev + 1);
      }
    });

    return () => {
      WebSocketService.disconnect();
    };
  }, [navigate, userId]);

  useEffect(() => {
    const appContainer = document.querySelector(".dashboard-app-container");
    if (isDarkMode) {
      appContainer?.classList.add("dark");
    } else {
      appContainer?.classList.remove("dark");
    }
    //localStorage.setItem("darkMode", JSON.stringify(isDarkMode));
  }, [isDarkMode]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    navigate("/login");
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const handleSearchSubmit = (e) => {
    if (e.key === "Enter") {
      const currentPath = window.location.pathname; // Get current URL path
      const encodedQuery = encodeURIComponent(searchQuery);
      if (currentPath.includes("/orders")) {
        navigate(`/orders?search=${encodedQuery}`);
      } else {
        navigate(`/buying?keyword=${encodedQuery}`);
      }

      //navigate(`/buying?keyword=${encodeURIComponent(searchQuery)}`);
    }
  };

  const toggleDropdown = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  const markAsRead = (id) => {
    fetch(`${BASE_URL}/api/notifications/mark-as-read/${id}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    }).then(() => {
      setNotifications((prevNotifs) => {
        const updated = prevNotifs.filter((notif) => notif.id !== id);
        setUnreadNotifications(updated.length); // 👈 update unread count
        return updated;
      });
    });
  };


  return (
    <div className="dashboard-app-container">
      {/* Sidebar */}
      <div className="dashboard-sidebar">
        <img
          src={DalLogo}
          alt="Logo"
          className="dashboard-logo"
          onClick={() => navigate("/dashboard")}
        />
        <div className="dashboard-sidebar-icons">
          {/* <IconButton
            Icon={LayoutGrid}
            onClick={() => navigate("/dashboard")}
          /> */}
          <IconButton Icon={ShoppingBag} onClick={() => navigate("/buying")} />
          <IconButton Icon={FileText} onClick={() => navigate("/selling")} />
          <IconButton Icon={Heart} onClick={() => navigate("/wishlist")} />
          <IconButton Icon={User} onClick={() => navigate("/profilepage")} />
          <IconButton
            Icon={Settings}
            onClick={() => navigate("/settings/notifications")}
          />
          <IconButton
            Icon={Gavel}
            onClick={() => navigate("/bids")}
            title="My Bids"
          />
          <IconButton
            className="dashboard-logout-icon"
            Icon={LogOut}
            onClick={handleLogout}
          />
        </div>
      </div>

      {/* Main Content */}
      <div className="dashboard-main-content">
        {/* Top Bar */}
        <div className="dashboard-top-bar">
          <div className="dashboard-search-container">
            <input
              type="text"
              placeholder="Search Here"
              className="dashboard-search-input"
              value={searchQuery}
              onChange={handleSearchChange}
              onKeyDown={handleSearchSubmit}
            />
            <Search className="dashboard-search-icon" />
          </div>

          <div className="dashboard-top-bar-icons">
            {/* <CartCounter onClick={() => navigate("/cart")} /> */}
            <CartCounter
              onClick={() => navigate("/cart")}
              onCartUpdate={(count) => console.log("Cart updated:", count)}
            />
            {/* <IconButton Icon={Bell} onClick={() => navigate("/notifications")} /> */}
            {/* Notifications Bell Icon with Dropdown */}
            <IconButton
              Icon={Receipt}
              onClick={() => navigate("/orders")}
              title="Orders & Receipts"
            />
            <div className="notification-container">
              <IconButton
                Icon={Bell}
                onClick={toggleDropdown}
                notificationCount={unreadNotifications}
              />
              {isDropdownOpen && (
                <div className="notification-dropdown">
                  <button
                    className="notification-close-btn"
                    onClick={() => setIsDropdownOpen(false)}
                  >
                    ✖
                  </button>
                  {notifications.length === 0 ? (
                    <p>No new notifications</p>
                  ) : (
                    notifications.map((notif) => (
                      <div key={notif.id} className="notification-item">
                        <p>{notif.message}</p>
                        <button onClick={() => markAsRead(notif.id)}>
                          Read
                        </button>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
            <IconButton
              Icon={isDarkMode ? Sun : Moon}
              onClick={() => setIsDarkMode(!isDarkMode)}
              darkModeIcon={true}
            />
            <div className="seller-image-placeholder"onClick={() => navigate("/profilepage")}
                                                                   style={{ cursor: "pointer" }}>
                {username ? username.charAt(0).toUpperCase() : "?"}

            </div>
          </div>
        </div>

        <div className="dashboard-content-wrapper">
          <Outlet /> {/* page content will be rendered here*/}
        </div>
      </div>
    </div>
  );
};

export default Layout;
