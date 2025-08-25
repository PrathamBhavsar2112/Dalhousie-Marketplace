import React, { useState, useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import axios from "axios";
import "../css/ChatBox.css";
import { BASE_URL } from "../constant_url";

const ChatBox = ({ senderId, listingId, receiverId }) => {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);

  const fixedSenderId = Number(senderId);
  const fixedReceiverId = Number(receiverId);
  const fixedListingId = Number(listingId);

  useEffect(() => {
    if (fixedSenderId === fixedReceiverId) {
      setError("You cannot message yourself");
    } else {
      setError(null);
    }
  }, [fixedSenderId, fixedReceiverId]);

  const conversationId = `${Math.min(fixedSenderId, fixedReceiverId)}_${Math.max(fixedSenderId, fixedReceiverId)}_${fixedListingId}`;

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchMessageHistory = useCallback(async () => {
    if (!senderId || !receiverId || !listingId || error) return;

    setIsLoading(true);
    try {
      const response = await axios.get(`${BASE_URL}/api/messages/history/${conversationId}`, {
        withCredentials: true,
      });

      if (response.status === 204 || response.data === "") {
        setMessages([]);
        return;
      }

      setMessages(response.data);
    } catch (error) {
      console.error("Error fetching messages:", error);
      setMessages([]);
    } finally {
      setIsLoading(false);
    }
  }, [conversationId, senderId, receiverId, listingId, error]);

  const connectWebSocket = useCallback(() => {
    if (!senderId || !receiverId || !listingId || error || stompClientRef.current) return;

    const socket = new SockJS(`${BASE_URL}/ws`, null, { withCredentials: true });
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP Debug:", str),
      onConnect: () => {
        setIsConnected(true);
        stompClient.subscribe(`/topic/messages/${conversationId}`, (message) => {
          try {
            const receivedMessage = JSON.parse(message.body);

            setMessages((prev) =>
              prev.some((m) => m.id === receivedMessage.id) ? prev : [...prev.filter(m => !m.temporary), receivedMessage]
            );
          } catch (error) {
            console.error("Error processing WebSocket message:", error);
          }
        });
      },
      onStompError: () => {
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
      },
    });

    stompClient.activate();
    stompClientRef.current = stompClient;
  }, [conversationId, senderId, receiverId, listingId, error]);

  useEffect(() => {
    if (isChatOpen) {
      fetchMessageHistory();
      connectWebSocket();
    }

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
        setIsConnected(false);
      }
    };
  }, [isChatOpen, fetchMessageHistory, connectWebSocket]);

  const sendMessage = async () => {
    if (!newMessage.trim() || !senderId || !receiverId || !listingId) return;

    if (fixedSenderId === fixedReceiverId) {
      alert("You cannot message yourself.");
      return;
    }

    try {
      const tempMessage = {
        id: `temp_${Date.now()}`,
        content: newMessage,
        timestamp: new Date().toISOString(),
        sender: { userId: senderId },
        temporary: true,
      };

      setMessages((prev) => [...prev, tempMessage]);
      setNewMessage("");

      await axios.post(`${BASE_URL}/api/messages/send`, {
        senderId,
        receiverId,
        listingId,
        content: newMessage,
      }, { withCredentials: true });

      console.log("Message sent successfully");

      setMessages((prev) => prev.filter((msg) => msg.id !== tempMessage.id));

    } catch (error) {
      console.error("Error sending message:", error);
      alert("Failed to send message. Please try again.");
    }
  };

  const handleCloseChat = () => {
    setIsChatOpen(false);
  };

  const formatDate = (timestamp) => {
    return new Date(timestamp).toLocaleDateString(undefined, {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });
  };

  return (
    <>
      {!isChatOpen && (
        <button className="chat-button" onClick={() => setIsChatOpen(true)}>
          Chat
        </button>
      )}

      {isChatOpen && (
        <div className="chatbox">
          <div className="chat-header">
            <span>Chat {isConnected ? "✓" : ""}</span>
            <button className="close-btn" onClick={handleCloseChat}>✖</button>
          </div>

          <div className="chat-messages">
            {isLoading ? (
              <p>Loading messages...</p>
            ) : messages.length > 0 ? (
              messages.reduce((acc, msg, index, array) => {
                const messageDate = formatDate(msg.timestamp);
                const prevDate = index > 0 ? formatDate(array[index - 1].timestamp) : null;

                if (messageDate !== prevDate) {
                  acc.push(
                    <div key={messageDate} className="date-divider">
                      <p>{messageDate}</p>
                    </div>
                  );
                }

                acc.push(
                  <div
                    key={`${msg.id || index}-${msg.timestamp}`}
                    className={`message ${Number(msg.sender?.userId) === Number(senderId) ? "sent" : "received"}`}
                  >
                    <p>{msg.content}</p>
                    <span>{formatTime(msg.timestamp)}</span>
                  </div>
                );

                return acc;
              }, [])
            ) : (
              <p>No messages yet.</p>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="chat-input">
            <input type="text" value={newMessage} onChange={(e) => setNewMessage(e.target.value)} />
            <button className="send-btn" onClick={sendMessage} disabled={!isConnected || !newMessage.trim()}>
              Send
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export default ChatBox;
