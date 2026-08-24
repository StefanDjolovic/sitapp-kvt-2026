package rs.ac.uns.ftn.sitapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.sitapp.domain.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    boolean existsByConversationIdAndSenderIdAndContent(
            Long conversationId,
            Long senderId,
            String content
    );

    Page<Message> findByConversationIdOrderBySentAtDescIdDesc(
            Long conversationId,
            Pageable pageable
    );

    List<Message> findByConversationIdOrderBySentAtAscIdAsc(Long conversationId);

    Optional<Message> findFirstByConversationIdOrderBySentAtDescIdDesc(Long conversationId);

    Optional<Message> findByIdAndConversationId(Long id, Long conversationId);

    @Query("""
            SELECT COUNT(message.id)
            FROM Message message
            JOIN ConversationParticipant participant
              ON participant.conversation = message.conversation
            WHERE message.conversation.id = :conversationId
              AND participant.user.id = :userId
              AND message.sender.id <> :userId
              AND (participant.lastReadMessageId IS NULL
                   OR message.id > participant.lastReadMessageId)
            """)
    long countUnreadMessages(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );
}
