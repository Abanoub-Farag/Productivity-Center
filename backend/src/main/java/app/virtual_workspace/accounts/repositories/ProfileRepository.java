package app.virtual_workspace.accounts.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.accounts.models.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @EntityGraph(attributePaths = { "user" })
    Optional<Profile> findByUserIdWithUser(Long userId);

}
