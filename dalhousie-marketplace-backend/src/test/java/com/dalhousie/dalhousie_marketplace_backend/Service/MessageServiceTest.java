package com.dalhousie.dalhousie_marketplace_backend.Service;

import com.dalhousie.dalhousie_marketplace_backend.DTO.ConversationDTO;
import com.dalhousie.dalhousie_marketplace_backend.model.*;
import com.dalhousie.dalhousie_marketplace_backend.repository.ListingRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.MessageRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.NotificationRepository;
import com.dalhousie.dalhousie_marketplace_backend.repository.UserRepository;
import com.dalhousie.dalhousie_marketplace_backend.service.MessageService;
import com.dalhousie.dalhousie_marketplace_backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;


    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;
    private Listing mockListing;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        sender = new User();
        sender.setUserId(91L);
        sender.setusername("sender");

        receiver = new User();
        receiver.setUserId(105L);
        receiver.setusername("receiver");

        mockListing = new Listing();
        mockListing.setId(1L);
        mockListing.setSeller(receiver); 

        when(userRepository.findById(91L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(105L)).thenReturn(Optional.of(receiver));
        when(listingRepository.findById(1L)).thenReturn(Optional.of(mockListing));
    }

//    @Test
//    public void testSendMessage_Success() {
//        Long senderId = 91L;
//        Long listingId = 1L;
//        String content = "Hello, this is a test message.";
//        String conversationId = "91_105_1";
//
//        Message mockMessage = new Message();
//        mockMessage.setContent(content);
//        mockMessage.setSender(sender);
//        mockMessage.setReceiver(receiver);
//        mockMessage.setListing(mockListing);
//        mockMessage.setConversationId(conversationId);
//
//        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
//
//        Message result = messageService.sendMessage(senderId, listingId, content);
//
//        assertNotNull(result);
//        assertEquals(content, result.getContent());
//        assertEquals(sender, result.getSender());
//        assertEquals(receiver, result.getReceiver());
//        assertEquals(mockListing, result.getListing());
//        assertEquals(conversationId, result.getConversationId());
//
//        verify(messageRepository, times(1)).save(any(Message.class));
//        verify(notificationService, times(1)).sendNotification(
//                eq(receiver),
//                eq(NotificationType.MESSAGE),
//                contains("Hello, this is a test message.")
//        );
//
//    }

    @Test
    public void testSendMessage_ReceiverNotFound() {
        Long senderId = 91L;
        Long listingId = 1L;
        String content = "Hello, is this available?";

        when(userRepository.findById(105L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, listingId, content);
        });

        assertEquals("Receiver not found", exception.getMessage());

        verify(messageRepository, never()).save(any(Message.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    public void testSendMessage_ListingNotFound() {
        Long senderId = 91L;
        Long invalidListingId = 999L;
        String content = "Hello, is this available?";

        when(listingRepository.findById(invalidListingId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, invalidListingId, content);
        });

        assertEquals("Listing not found", exception.getMessage());

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    public void testGenerateConversationId() {
        String conversationId = messageService.generateConversationId(91L, 105L, 1L);
        assertEquals("91_105_1", conversationId);
    }

//    @Test
//    public void testGetMessageHistory() {
//        String conversationId = "91_105_1";
//        Message message1 = new Message();
//        message1.setConversationId(conversationId);
//        message1.setContent("Message 1");
//
//        Message message2 = new Message();
//        message2.setConversationId(conversationId);
//        message2.setContent("Message 2");
//
//        when(messageRepository.findByConversationId(conversationId)).thenReturn(Arrays.asList(message1, message2));
//
//        List<Message> messages = messageService.getMessageHistory(conversationId);
//        assertEquals(2, messages.size());
//        assertEquals("Message 1", messages.get(0).getContent());
//        assertEquals("Message 2", messages.get(1).getContent());
//    }

//    @Test
//    public void testGetSellerConversations() {
//        String conversationId = "91_105_1";
//        Message lastMessage = new Message();
//        lastMessage.setConversationId(conversationId);
//        lastMessage.setSender(sender);
//        lastMessage.setReceiver(receiver);
//        lastMessage.setContent("Last message");
//        lastMessage.setTimestamp(new Date());
//        lastMessage.setListing(mockListing);
//
//        when(messageRepository.findDistinctConversationIdsBySeller(105L)).thenReturn(Collections.singletonList(conversationId));
//        when(messageRepository.findByConversationId(conversationId)).thenReturn(Collections.singletonList(lastMessage));
//
//        List<ConversationDTO> conversations = messageService.getSellerConversations(105L);
//        assertEquals(1, conversations.size());
//        assertEquals("Last message", conversations.get(0).getLastMessage());
//    }

//    @Test
//    public void testGetBuyerConversations() {
//        String conversationId = "91_105_1";
//        Message lastMessage = new Message();
//        lastMessage.setConversationId(conversationId);
//        lastMessage.setSender(sender);
//        lastMessage.setReceiver(receiver);
//        lastMessage.setContent("Buyer message");
//        lastMessage.setTimestamp(new Date());
//        lastMessage.setListing(mockListing);
//
//        when(messageRepository.findDistinctConversationIdsByBuyer(91L)).thenReturn(Collections.singletonList(conversationId));
//        when(messageRepository.findByConversationId(conversationId)).thenReturn(Collections.singletonList(lastMessage));
//
//        List<ConversationDTO> conversations = messageService.getBuyerConversations(91L);
//        assertEquals(1, conversations.size());
//        assertEquals("Buyer message", conversations.get(0).getLastMessage());
//    }

    // sendMessage(Long senderId, Long listingId, String content)
    @Test
    public void sendMessageOverloaded_ReturnsNonNull() {
        Long senderId = 91L;
        Long listingId = 1L;
        String content = "Hello";
        String conversationId = "91_105_1";
        Message mockMessage = new Message();
        mockMessage.setConversationId(conversationId);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);

        Message result = messageService.sendMessage(senderId, listingId, content);

        assertNotNull(result);
    }

    @Test
    public void sendMessageOverloaded_ListingNotFound() {
        Long senderId = 91L;
        Long invalidListingId = 999L;
        when(listingRepository.findById(invalidListingId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, invalidListingId, "Hello");
        });

        assertEquals("Listing not found", exception.getMessage());
    }

    // sendMessage(Long senderId, Long receiverId, Long listingId, String content, String conversationId)
    @Test
    public void sendMessage_ReturnsNonNull() {
        Long senderId = 91L;
        Long receiverId = 105L;
        Long listingId = 1L;
        String content = "Hi there";
        String conversationId = "91_105_1";
        Message mockMessage = new Message();
        mockMessage.setConversationId(conversationId);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);

        Message result = messageService.sendMessage(senderId, receiverId, listingId, content, conversationId);

        assertNotNull(result);
    }

    @Test
    public void sendMessage_SelfMessagingThrowsException() {
        Long userId = 91L;
        String conversationId = "91_91_1";

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(userId, userId, 1L, "Hi me", conversationId);
        });

        assertEquals("You cannot message yourself.", exception.getMessage());
    }

    @Test
    public void sendMessage_SenderNotFound() {
        Long senderId = 999L;
        Long receiverId = 105L;
        String conversationId = "999_105_1";
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, receiverId, 1L, "Hello", conversationId);
        });

        assertEquals("Sender not found", exception.getMessage());
    }

    @Test
    public void sendMessage_ReceiverNotFound() {
        Long senderId = 91L;
        Long receiverId = 999L;
        String conversationId = "91_999_1";
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, receiverId, 1L, "Hello", conversationId);
        });

        assertEquals("Receiver not found", exception.getMessage());
    }

    @Test
    public void sendMessage_ListingNotFoundInFullMethod() {
        Long senderId = 91L;
        Long receiverId = 105L;
        Long invalidListingId = 999L;
        String conversationId = "91_105_999";
        when(listingRepository.findById(invalidListingId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage(senderId, receiverId, invalidListingId, "Hello", conversationId);
        });

        assertEquals("Listing not found", exception.getMessage());
    }

    // generateConversationId
    @Test
    public void generateConversationId_SenderLessThanReceiver() {
        String result = messageService.generateConversationId(91L, 105L, 1L);

        assertEquals("91_105_1", result);
    }

    @Test
    public void generateConversationId_ReceiverLessThanSender() {
        String result = messageService.generateConversationId(105L, 91L, 1L);

        assertEquals("91_105_1", result);
    }

    // @Test
    // public void generateConversationId_NullSenderThrowsException() {
    //     Exception exception = assertThrows(IllegalArgumentException.class, () -> {
    //         messageService.generateConversationId(null, 105L, 1L);
    //     });

    //     assertEquals("Sender, receiver, and listing IDs cannot be null", exception.getMessage());
    // }

    // @Test
    // public void generateConversationId_NullReceiverThrowsException() {
    //     Exception exception = assertThrows(IllegalArgumentException.class, () -> {
    //         messageService.generateConversationId(91L, null, 1L);
    //     });

    //     assertEquals("Sender, receiver, and listing IDs cannot be null", exception.getMessage());
    // }

    // @Test
    // public void generateConversationId_NullListingThrowsException() {
    //     Exception exception = assertThrows(IllegalArgumentException.class, () -> {
    //         messageService.generateConversationId(91L, 105L, null);
    //     });

    //     assertEquals("Sender, receiver, and listing IDs cannot be null", exception.getMessage());
    // }

    // getMessageHistory
    @Test
    public void getMessageHistory_ReturnsCorrectSize() {
        String conversationId = "91_105_1";
        Message message1 = new Message();
        message1.setConversationId(conversationId);
        message1.setContent("Message 1");
        List<Message> mockMessages = Collections.singletonList(message1);
        when(messageRepository.findByConversationId(conversationId)).thenReturn(mockMessages);

        List<Message> result = messageService.getMessageHistory(conversationId);

        assertEquals(1, result.size());
    }

    // getSellerConversations
    @Test
    public void getSellerConversations_ReturnsCorrectSize() {
        String conversationId = "91_105_1";
        Message lastMessage = new Message();
        lastMessage.setConversationId(conversationId);
        lastMessage.setSender(sender);
        lastMessage.setReceiver(receiver);
        lastMessage.setContent("Last message");
        lastMessage.setTimestamp(new Date());
        lastMessage.setListing(mockListing);
        when(messageRepository.findDistinctConversationIdsBySeller(105L)).thenReturn(Collections.singletonList(conversationId));
        when(messageRepository.findByConversationId(conversationId)).thenReturn(Collections.singletonList(lastMessage));

        List<ConversationDTO> result = messageService.getSellerConversations(105L);

        assertEquals(1, result.size());
    }

    @Test
    public void getSellerConversations_EmptyWhenNoMessages() {
        when(messageRepository.findDistinctConversationIdsBySeller(105L)).thenReturn(Collections.emptyList());

        List<ConversationDTO> result = messageService.getSellerConversations(105L);

        assertTrue(result.isEmpty());
    }

    // getMessagesForListing
    @Test
    public void getMessagesForListing_ReturnsCorrectSize() {
        Long listingId = 1L;
        Long userId = 91L;
        Message message = new Message();
        message.setConversationId("91_105_1");
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("Hi");
        message.setTimestamp(new Date());
        message.setListing(mockListing);
        when(messageRepository.findByListingAndUser(listingId, userId)).thenReturn(Collections.singletonList(message));

        List<ConversationDTO> result = messageService.getMessagesForListing(listingId, userId);

        assertEquals(1, result.size());
    }

    @Test
    public void getMessagesForListing_EmptyWhenNoMessages() {
        Long listingId = 1L;
        Long userId = 91L;
        when(messageRepository.findByListingAndUser(listingId, userId)).thenReturn(Collections.emptyList());

        List<ConversationDTO> result = messageService.getMessagesForListing(listingId, userId);

        assertTrue(result.isEmpty());
    }

    // getBuyerConversations
    @Test
    public void getBuyerConversations_ReturnsCorrectSize() {
        String conversationId = "91_105_1";
        Message lastMessage = new Message();
        lastMessage.setConversationId(conversationId);
        lastMessage.setSender(sender);
        lastMessage.setReceiver(receiver);
        lastMessage.setContent("Buyer message");
        lastMessage.setTimestamp(new Date());
        lastMessage.setListing(mockListing);
        when(messageRepository.findDistinctConversationIdsByBuyer(91L)).thenReturn(Collections.singletonList(conversationId));
        when(messageRepository.findByConversationId(conversationId)).thenReturn(Collections.singletonList(lastMessage));

        List<ConversationDTO> result = messageService.getBuyerConversations(91L);

        assertEquals(1, result.size());
    }

    @Test
    public void getBuyerConversations_EmptyWhenNoMessages() {
        when(messageRepository.findDistinctConversationIdsByBuyer(91L)).thenReturn(Collections.emptyList());

        List<ConversationDTO> result = messageService.getBuyerConversations(91L);

        assertTrue(result.isEmpty());
    }
}
