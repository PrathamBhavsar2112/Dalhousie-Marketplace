package com.dalhousie.dalhousie_marketplace_backend.repository;

import com.dalhousie.dalhousie_marketplace_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Retrieves all messages in a specific conversation.
     */
    List<Message> findByConversationId(String conversationId);

    /**
     * Retrieves distinct conversation IDs where the seller is involved.
     */
    @Query("SELECT DISTINCT m.conversationId FROM Message m " +
           "WHERE m.receiver.userId = :sellerId OR m.sender.userId = :sellerId")
    List<String> findDistinctConversationIdsBySeller(@Param("sellerId") Long sellerId);

    /**
     * Retrieves distinct conversation IDs where the buyer is involved.
     */
    @Query("SELECT DISTINCT m.conversationId FROM Message m " +
           "WHERE (m.receiver.userId = :buyerId OR m.sender.userId = :buyerId) " +
           "AND m.receiver.userId <> m.listing.seller.userId")
    List<String> findDistinctConversationIdsByBuyer(@Param("buyerId") Long buyerId);

    /**
     * Retrieves messages related to a specific listing and user.
     */
    @Query("SELECT m FROM Message m " +
           "WHERE m.listing.id = :listingId " +
           "AND (m.sender.userId = :userId OR m.receiver.userId = :userId) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findByListingAndUser(@Param("listingId") Long listingId, 
                                       @Param("userId") Long userId);
                                       
    /**
     * Retrieves the latest message for each conversation involving a specific user.
     */
    @Query("SELECT m FROM Message m " +
           "WHERE m.conversationId IN " +
           "(SELECT DISTINCT m2.conversationId FROM Message m2 WHERE m2.sender.userId = :userId OR m2.receiver.userId = :userId) " +
           "AND m.timestamp = " +
           "(SELECT MAX(m3.timestamp) FROM Message m3 WHERE m3.conversationId = m.conversationId) " +
           "ORDER BY m.timestamp DESC")
    List<Message> findLatestMessagesByUser(@Param("userId") Long userId);
}