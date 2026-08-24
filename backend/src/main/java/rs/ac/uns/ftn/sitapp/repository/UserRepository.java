package rs.ac.uns.ftn.sitapp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.sitapp.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id IN :userIds
            ORDER BY u.id
            """)
    List<User> findAllByIdForUpdate(@Param("userIds") Collection<Long> userIds);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:currentUserId IS NULL OR u.id <> :currentUserId)
              AND (LOWER(u.username) LIKE :pattern ESCAPE '!'
               OR LOWER(u.firstName) LIKE :pattern ESCAPE '!'
               OR LOWER(u.lastName) LIKE :pattern ESCAPE '!'
               OR LOWER(u.phoneNumber) LIKE :pattern ESCAPE '!')
            ORDER BY LOWER(u.username), u.id
            """)
    List<User> search(
            @Param("pattern") String pattern,
            @Param("currentUserId") Long currentUserId
    );
}
