package com.dalhousie.dalhousie_marketplace_backend.controller;

import com.dalhousie.dalhousie_marketplace_backend.DTO.MessageRequest;
import com.dalhousie.dalhousie_marketplace_backend.model.Message;
import com.dalhousie.dalhousie_marketplace_backend.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

public class ChatControllerTest {

    private ChatController chatController;
    private MessageService messageService;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        chatController = new ChatController();

        // Create mocks
        messageService = mock(MessageService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        // Inject mocks into private fields using reflection
        setPrivateField(chatController, "messageService", messageService);
        setPrivateField(chatController, "messagingTemplate", messagingTemplate);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testSendMessage() {
        // Arrange
        MessageRequest request = new MessageRequest();
        request.setSenderId(1L);
        request.setReceiverId(2L);
        request.setListingId(3L);
        request.setContent("Hello!");

        String conversationId = "1-2-3";
        Message savedMessage = new Message();
        savedMessage.setContent("Hello!");

        when(messageService.generateConversationId(1L, 2L, 3L)).thenReturn(conversationId);
        when(messageService.sendMessage(1L, 2L, 3L, "Hello!", conversationId)).thenReturn(savedMessage);

        // Act
        chatController.sendMessage(request);

        // Assert
        verify(messagingTemplate).convertAndSend("/topic/messages/" + conversationId, savedMessage);
        verify(messagingTemplate).convertAndSend("/user/2/queue/messages", savedMessage);
        verify(messagingTemplate).convertAndSend("/user/1/queue/messages", savedMessage);
    }
}
