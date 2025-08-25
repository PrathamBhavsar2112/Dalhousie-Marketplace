import { useState, useEffect, useCallback } from "react";
import "../css/Announcements.css";

const Announcements = () => {
  const messages = [
    "🔥 New Categories Added! Click for details.",
    "🚀 Get Featured Listings for Free! Limited time offer.",
    "🎉 Invite Friends & Earn Rewards! Click to learn more.",
  ];

  const [currentMessageIndex, setCurrentMessageIndex] = useState(0);
  const [showFullMessage, setShowFullMessage] = useState(false);

  const rotateMessage = useCallback(() => {
    setCurrentMessageIndex((prevIndex) => (prevIndex + 1) % messages.length);
  }, [messages.length]);

  useEffect(() => {
    const interval = setInterval(rotateMessage, 3000);
    return () => clearInterval(interval);
  }, [rotateMessage]);

  return (
    <div className="announcements">
      <h3>📢 Marketplace Announcements</h3>
      <div 
        className="announcement-message" 
        onClick={() => setShowFullMessage(!showFullMessage)}
      >
        {showFullMessage ? messages[currentMessageIndex] : messages[currentMessageIndex].split("!")[0] + "!"}
      </div>
    </div>
  );
};

export default Announcements;