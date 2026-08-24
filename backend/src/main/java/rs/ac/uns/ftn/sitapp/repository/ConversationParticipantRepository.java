package rs.ac.uns.ftn.sitapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
