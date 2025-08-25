import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { BASE_URL } from "../constant_url"; // Ensure BASE_URL is correctly imported

const SOCKET_URL = `${BASE_URL}/ws`;

class WebSocketService {
  constructor() {
    this.stompClient = null;
  }

  connect(userId, onMessageReceived) {
    const socket = new SockJS(SOCKET_URL);
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        console.log("Connected to WebSocket");

        // Subscribe to the notifications queue
        this.stompClient.subscribe(`/queue/notifications/${userId}`, (message) => {
          const notification = JSON.parse(message.body);
          onMessageReceived(notification);
        });
        
        
      },
      onStompError: (error) => {
        console.error("WebSocket error:", error);
      },
    });

    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }
}

const webSocketServiceInstance = new WebSocketService();
export default webSocketServiceInstance;
