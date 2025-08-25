import React, { useState, useEffect } from "react";
import WebSocketService from "../services/WebSocketService";

const Notifications = ({ userId }) => {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    // Fetch unread notifications from backend
    fetch(`/api/notifications/${userId}`)
      .then((res) => res.json())
      .then((data) => setNotifications(data));

    // Connect to WebSocket for real-time updates
    WebSocketService.connect(userId, (newNotification) => {
      setNotifications((prev) => [newNotification, ...prev]);
    });

    return () => {
      WebSocketService.disconnect();
    };
  }, [userId]);

  const markAsRead = (id) => {
    fetch(`/api/notifications/mark-as-read/${id}`, { method: "POST" })
      .then(() => {
        setNotifications(notifications.filter((notif) => notif.id !== id));
      });
  };

  return (
    <div>
      <button>🔔 ({notifications.length})</button>
      <div className="dropdown">
        {notifications.length === 0 ? <p>No new notifications</p> : null}
        {notifications.map((notif) => (
          <div key={notif.id} className="notification-item">
            <p>{notif.message}</p>
            <button onClick={() => markAsRead(notif.id)}>Mark as Read</button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Notifications;

  