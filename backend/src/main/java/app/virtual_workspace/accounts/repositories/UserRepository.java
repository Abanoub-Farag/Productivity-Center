package app.virtual_workspace.accounts.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.models.User;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
                SELECT new app.virtual_workspace.accounts.dtos.data.UserDataDto(u.id,
                        u.email,
                        u.firstName,
                        u.lastName,
                        u.createdAt,
                        u.updatedAt,
                        p.bio,
                        p.gender,
                        p.dateOfBirth,
                        r.id
                    )
                FROM User u
                LEFT JOIN Profile p ON p.user = u
                LEFT JOIN Room r ON r.user = u
                WHERE u.id = :userId
            """)
    Optional<UserDataDto> findUserByIdWithProfileAndRoom(@Param("userId") Long userId);

    Optional<User> findUserByEmail(String email);

    Optional<User> findUserById(Long id);

    User getUserByEmail(String email);

    User getUserById(Long id);

    boolean existsByEmail(String email);
}
