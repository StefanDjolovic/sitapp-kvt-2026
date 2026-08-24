package rs.ac.uns.ftn.sitapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.sitapp.domain.Conversation;
import rs.ac.uns.ftn.sitapp.domain.ConversationType;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndType(Long id, ConversationType type);

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
