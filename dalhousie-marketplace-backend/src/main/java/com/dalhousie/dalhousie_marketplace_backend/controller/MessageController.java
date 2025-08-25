package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.MessageRequest;
import com.dalhousie.dalhousie_marketplace_backend.DTO.ConversationDTO;
import com.dalhousie.dalhousie_marketplace_backend.model.Message;
import com.dalhousie.dalhousie_marketplace_backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a message from sender to receiver related to a listing.
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest messageRequest) {
        try {
            if (messageRequest.getSenderId() == null || messageRequest.getReceiverId() == null || messageRequest.getListingId() == null) {
                return ResponseEntity.badRequest().body("Sender, receiver, and listing IDs are required");
            }

            // Check if sender and receiver are the same
            if (messageRequest.getSenderId().equals(messageRequest.getReceiverId())) {
                return ResponseEntity.badRequest().body("You cannot send messages to yourself");
            }

            // Generate standardized conversation ID via MessageService
            String conversationId = messageService.generateConversationId(
                    messageRequest.getSenderId(),
                    messageRequest.getReceiverId(),
                    messageRequest.getListingId()
            );

            // Save message in DB
            Message message = messageService.sendMessage(
                    messageRequest.getSenderId(),
                    messageRequest.getReceiverId(),
                    messageRequest.getListingId(),
                    messageRequest.getContent(),
                    conversationId
            );

            // Broadcast message using WebSockets
            messagingTemplate.convertAndSend("/topic/messages/" + conversationId, message);

            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves all conversations for a listing where a user is involved.
     */
    @GetMapping("/listing/{listingId}/conversations/{userId}")
    public ResponseEntity<List<ConversationDTO>> getListingMessages(
            @PathVariable Long listingId, @PathVariable Long userId) {
        List<ConversationDTO> conversations = messageService.getMessagesForListing(listingId, userId);
        return conversations.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(conversations);
    }

    /**
     * Fetch chat history based on conversationId.
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<Message>> getMessagesByConversation(@PathVariable String conversationId) {
        List<Message> messages = messageService.getMessageHistory(conversationId);
        return messages.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(messages);
    }

    /**
     * Fetch all conversations associated with a seller.
     */
    @GetMapping("/conversations/seller/{sellerId}")
    public ResponseEntity<List<ConversationDTO>> getSellerConversations(@PathVariable Long sellerId) {
        List<ConversationDTO> conversationList = messageService.getSellerConversations(sellerId);
        return conversationList.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(conversationList);
    }

    /**
     * Fetch all conversations associated with a buyer.
     */
    @GetMapping("/conversations/buyer/{buyerId}")
    public ResponseEntity<List<ConversationDTO>> getBuyerConversations(@PathVariable Long buyerId) {
        List<ConversationDTO> conversationList = messageService.getBuyerConversations(buyerId);
        return conversationList.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(conversationList);
    }
}
