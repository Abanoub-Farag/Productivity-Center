package app.virtual_workspace.accounts.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.accounts.models.Profile;
import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<Profile> findProfileAndUserByUserId(@Param("userId") Long userId);

    Optional<Profile> findByUserId(Long userId);

}
