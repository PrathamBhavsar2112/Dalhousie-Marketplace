import React, { useState, useEffect } from "react";
import axios from "axios";
import ChatBox from "./ChatBox";
import "../css/SellerConversations.css";
import { BASE_URL } from "../constant_url";


const SellerConversations = ({ listingId, sellerId }) => {
  const [conversations, setConversations] = useState([]);
  const [selectedBuyer, setSelectedBuyer] = useState(null);
  const [isConversationsOpen, setIsConversationsOpen] = useState(false);

  useEffect(() => {
    const fetchConversations = async () => {
      try {
        const response = await axios.get(
          `${BASE_URL}/api/messages/listing/${listingId}/conversations/${sellerId}`,
          { withCredentials: true }
        );
        console.log("Conversations API Response:", response.data);
        
        if (Array.isArray(response.data) && response.data.length > 0) {
          setConversations(response.data);
        } else {
          console.warn("No conversations found for this listing.");
          setConversations([]);
        }
      } catch (error) {
        console.error("Error fetching conversations:", error);
        setConversations([]);
      }
    };

    if (listingId && sellerId) {
      fetchConversations();
    }
  }, [listingId, sellerId]);

  const handleOpenChat = (buyerId) => {
    console.log("🔹 Opening chat with buyerId:", buyerId);
    console.log("🔹 Current sellerId:", sellerId);

    if (buyerId && typeof buyerId === "number") {
      setSelectedBuyer(buyerId);
      setIsConversationsOpen(true);
    } else {
      console.error("Invalid buyerId received:", buyerId);
    }
  };

  return (
    <>
      {!isConversationsOpen && (
        <button
          className="conversations-toggle-button"
          onClick={() => setIsConversationsOpen(true)}
        >
          Messages
        </button>
      )}

      <div className={`seller-conversations ${isConversationsOpen ? "" : "hidden"}`}>
        <div className="conversations-header">
          <h3>Chats</h3>
          <button className="close-btn" onClick={() => setIsConversationsOpen(false)}>✕</button>
        </div>

        <div className="conversation-list">
          {conversations.length > 0 ? (
            conversations.map((conversation, index) => {
              console.log(`🔹 Conversation ${index + 1}:`, conversation);
              const buyerId = conversation.buyerId;
              const buyerName = conversation.buyerName || "Unknown Buyer";

              const formattedTime = new Date(conversation.timestamp).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit",
                hour12: true,
              });

              return (
                <div key={index} className="conversation" onClick={() => handleOpenChat(buyerId)}>
                  <div className="chat-avatar">
                    <span>{buyerName.charAt(0)}</span>
                  </div>

                  <div className="chat-info">
                    <div className="chat-header">
                      <b className="chat-name">{buyerName}</b>
                      <span className="chat-time">{formattedTime}</span>
                    </div>
                    <p className="chat-message-preview">{conversation.lastMessage}</p>
                  </div>
                </div>
              );
            })
          ) : (
            <p className="no-messages">No conversations yet.</p>
          )}
        </div>

        {selectedBuyer && (
          <div className="chatbox-wrapper">
            <ChatBox
              senderId={sellerId}
              receiverId={selectedBuyer}
              listingId={listingId}
              key={selectedBuyer}
            />
          </div>
        )}
      </div>
    </>
  );
};

export default SellerConversations;
