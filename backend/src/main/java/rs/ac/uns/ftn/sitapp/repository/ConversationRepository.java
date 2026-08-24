package rs.ac.uns.ftn.sitapp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findFirstByTypeAndTitle(
            ConversationType type,
            String title
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT conversation FROM Conversation conversation WHERE conversation.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT c
            FROM Conversation c
            JOIN ConversationParticipant participant
              ON participant.conversation = c
            LEFT JOIN Message message
              ON message.conversation = c
            WHERE participant.user.id = :userId
            GROUP BY c
            ORDER BY COALESCE(MAX(message.sentAt), c.createdAt) DESC, c.id DESC
            """)
    List<Conversation> findAllForUserOrderByActivityDesc(@Param("userId") Long userId);

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE c.type = :type
              AND (SELECT COUNT(p.id)
                   FROM ConversationParticipant p
                   WHERE p.conversation = c) = 2
              AND EXISTS (SELECT firstParticipant.id
                          FROM ConversationParticipant firstParticipant
                          WHERE firstParticipant.conversation = c
                            AND firstParticipant.user.id = :firstUserId)
              AND EXISTS (SELECT secondParticipant.id
                          FROM ConversationParticipant secondParticipant
                          WHERE secondParticipant.conversation = c
                            AND secondParticipant.user.id = :secondUserId)
            """)
    Optional<Conversation> findBetweenUsers(
            @Param("type") ConversationType type,
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId
    );
}
