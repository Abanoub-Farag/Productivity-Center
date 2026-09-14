package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("should return user if found successfully")
  void shouldReturnUserIfFoundSuccessfully() {

    Long userId = 1L;
    User mockUser = User.builder()
        .email("user@example.com")
        .firstName("user")
        .lastName("user")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

    User actualUser = userService.findUserById(userId);

    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getId()).isEqualTo(mockUser.getId());
    assertThat(actualUser.getFirstName()).isEqualTo(mockUser.getFirstName());

    verify(userRepository, times(1)).findById(userId);

  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user doesn't exist")
  void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {

    Long userId = 100L;
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.findUserById(userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User with id " + userId + " not found");

    verify(userRepository, times(1)).findById(userId);

  }

  @Test
  @DisplayName("should return user when user is saved successfully")
  void shouldReturnUserWhenUserIsSavedSuccessfully() {
    Long userId = 100L;

    User inputUser = User.builder()
        .id(userId)
        .email("user@example.com")
        .firstName("user")
        .lastName("user")
        .build();

    User savedUser = User.builder()
        .id(userId)
        .email("user@example.com")
        .firstName("user")
        .lastName("user")
        .build();

    when(userService.saveUser(inputUser)).thenReturn(savedUser);

    User actualUser = userService.saveUser(inputUser);

    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getId()).isEqualTo(userId);
    assertThat(actualUser.getEmail()).isEqualTo(savedUser.getEmail());

    verify(userRepository, times(1)).save(inputUser);
  }

  @Test
  @DisplayName("should return user data when user found successfully")
  void shouldReturnUserDataWhenUserFoundSuccefully() {

    Long userId = 98L;
    User inputUser = User.builder()
        .id(userId)
        .email("user@example.com")
        .firstName("firstName")
        .lastName("lastName")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(inputUser));

    UserDataDto userDataDto = userService.userData(userId);

    assertThat(inputUser.getId()).isEqualTo(userDataDto.getId());
    assertThat(inputUser.getEmail()).isEqualTo(userDataDto.getEmail());

    verify(userRepository, times(1)).findById(userId);
    verify(userService, times(1)).userData(userId);

  }

}
