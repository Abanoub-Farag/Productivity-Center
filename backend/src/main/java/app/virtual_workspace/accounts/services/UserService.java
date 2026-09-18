package app.virtual_workspace.accounts.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.mappers.UserMapper;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserDataDto userData(Long userId) {
        return userRepository.findUserByIdWithProfileAndRoom(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
    }

    @Transactional
    public UpdateUserDataDto updateData(Long userId, String firstName, String lastName) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + userId + " is not found"));
        user.setFirstName(firstName);
        user.setLastName(lastName);

        userRepository.save(user);

        return userMapper.toUserUpdateDataDto(user);

    }

}
