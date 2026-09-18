package app.virtual_workspace.accounts.events.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.events.UserRegisteredEvent;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfileSetupListener {

    private final ProfileRepository profileRepository;
    private final EntityManager entityManager;

    @Transactional
    @EventListener
    void onUserRegistered(UserRegisteredEvent event) {
        User user = entityManager.getReference(User.class, event.userId());
        Profile profile = new Profile();
        profile.setUser(user);
        profileRepository.save(profile);
    }

}
