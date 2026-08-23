package rs.ac.uns.ftn.sitapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.sitapp.domain.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
