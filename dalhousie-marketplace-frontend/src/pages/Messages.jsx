import { useState } from "react";
import "../css/Messages.css";

const Messages = () => {
  const [messages, setMessages] = useState([
   
  ]);

  const [newMessage, setNewMessage] = useState("");

  const sendMessage = () => {
    if (newMessage.trim() !== "") {
      setMessages([...messages, { id: messages.length + 1, sender: "You", text: newMessage }]);
      setNewMessage("");
    }
  };

  return (
    <div className="messages-container">
      <h2>💬 Messages</h2>
      <div className="message-list">
        {messages.map((msg) => (
          <div key={msg.id} className="message">
            <strong>{msg.sender}:</strong> {msg.text}
          </div>
        ))}
      </div>
      <div className="message-input">
        <input
          type="text"
          placeholder="Type a message..."
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
        />
        <button onClick={sendMessage}>Send</button>
      </div>
    </div>
  );
};

export default Messages;
