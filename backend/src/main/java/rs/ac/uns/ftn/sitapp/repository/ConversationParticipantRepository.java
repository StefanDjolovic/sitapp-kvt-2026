package rs.ac.uns.ftn.sitapp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.sitapp.domain.ConversationParticipant;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByUserId(Long userId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(
            Long conversationId,
            Long userId
    );

    List<ConversationParticipant> findByConversationIdOrderById(Long conversationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT participant
            FROM ConversationParticipant participant
            WHERE participant.conversation.id = :conversationId
              AND participant.user.id = :userId
            """)
    Optional<ConversationParticipant> findForUpdate(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );
}
