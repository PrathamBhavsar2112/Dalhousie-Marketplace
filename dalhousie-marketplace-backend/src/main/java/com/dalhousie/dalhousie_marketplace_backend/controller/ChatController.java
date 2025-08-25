package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.MessageRequest;
import com.dalhousie.dalhousie_marketplace_backend.model.Message;
import com.dalhousie.dalhousie_marketplace_backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload MessageRequest messageRequest) {
        try {
            if (isMessageRequestInvalid(messageRequest)) {
                logInvalidRequest();
                return;
            }

            String conversationId = generateConversationId(messageRequest);

            Message savedMessage = sendAndSaveMessage(messageRequest, conversationId);

            sendMessageToUsers(savedMessage, conversationId, messageRequest);

        } catch (Exception e) {
            logErrorSendingMessage(e);
        }
    }

    private boolean isMessageRequestInvalid(MessageRequest messageRequest) {
        return messageRequest.getSenderId() == null || messageRequest.getReceiverId() == null || messageRequest.getListingId() == null;
    }

    private void logInvalidRequest() {
        System.err.println("Invalid message request: Missing sender, receiver, or listing ID.");
    }

    private String generateConversationId(MessageRequest messageRequest) {
        return messageService.generateConversationId(
                messageRequest.getSenderId(),
                messageRequest.getReceiverId(),
                messageRequest.getListingId()
        );
    }

    private Message sendAndSaveMessage(MessageRequest messageRequest, String conversationId) {
        return messageService.sendMessage(
                messageRequest.getSenderId(),
                messageRequest.getReceiverId(),
                messageRequest.getListingId(),
                messageRequest.getContent(),
                conversationId
        );
    }

    private void sendMessageToUsers(Message savedMessage, String conversationId, MessageRequest messageRequest) {
        messagingTemplate.convertAndSend("/topic/messages/" + conversationId, savedMessage);
        messagingTemplate.convertAndSend("/user/" + messageRequest.getReceiverId() + "/queue/messages", savedMessage);
        messagingTemplate.convertAndSend("/user/" + messageRequest.getSenderId() + "/queue/messages", savedMessage);
    }

    private void logErrorSendingMessage(Exception e) {
        System.err.println("Error sending message: " + e.getMessage());
    }
}
