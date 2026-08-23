package rs.ac.uns.ftn.sitapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Zajednicki model za direktne i grupne razgovore.
 *
 * <p>Ogranicenje u tabeli {@code conversation_participants} sprecava da isti
 * korisnik bude dodat dva puta u jedan razgovor. Servisni sloj mora dodatno
 * garantovati da DIRECT razgovor ima tacno dva razlicita ucesnika i da za isti
 * par korisnika ne postoji vise DIRECT razgovora.</p>
 */
@Entity
@Table(
        name = "conversations",
        indexes = @Index(name = "idx_conversation_type", columnList = "type")
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    @Size(max = 100)
    @Column(length = 100)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    public Conversation(ConversationType type, String title) {
        this.type = type;
        this.title = title;
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
