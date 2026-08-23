package rs.ac.uns.ftn.sitapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.sitapp.domain.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderBySentAtDescIdDesc(
            Long conversationId,
            Pageable pageable
    );
}
