package app.virtual_workspace.accounts.services;

import org.springframework.stereotype.Service;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.UserRepository;

@Service
public class UserReferenceProvider {

    private final UserRepository userRepository;

    public UserReferenceProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getReference(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
