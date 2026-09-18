package app.virtual_workspace.accounts.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.mappers.UserMapper;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.accounts.repositories.UserRepository;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encoded_pwd")
                .role(Role.ROLE_USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findUserById() tests")
    class FindUserByIdTests {

        @Test
        @DisplayName("Should return User when user exists for ID")
        void findUserById_shouldReturnUser_whenUserExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            User result = userService.findUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(userRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void findUserById_shouldThrowResourceNotFoundException_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id 999 not found");

            verify(userRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for boundary values: 0L, negative, and extreme IDs")
        void findUserById_shouldThrowResourceNotFoundException_forBoundaryIds() {
            when(userRepository.findById(0L)).thenReturn(Optional.empty());
            when(userRepository.findById(-1L)).thenReturn(Optional.empty());
            when(userRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(0L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id 0 not found");

            assertThatThrownBy(() -> userService.findUserById(-1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id -1 not found");

            assertThatThrownBy(() -> userService.findUserById(Long.MAX_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id " + Long.MAX_VALUE + " not found");

            when(userRepository.findById(Long.MIN_VALUE)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.findUserById(Long.MIN_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id " + Long.MIN_VALUE + " not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when userId is null (null boundary)")
        void findUserById_shouldThrowResourceNotFoundException_whenUserIdIsNull() {
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id null not found");

            verify(userRepository, times(1)).findById(null);
        }

        @Test
        @DisplayName("Should propagate downstream exception when userRepository.findById fails")
        void findUserById_shouldPropagateException_whenRepositoryThrows() {
            when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> userService.findUserById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("saveUser() tests")
    class SaveUserTests {

        @Test
        @DisplayName("Should return saved User when user is saved successfully")
        void saveUser_shouldReturnSavedUser_whenSuccessful() {
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);

            User result = userService.saveUser(sampleUser);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should propagate exception when userRepository.save fails downstream")
        void saveUser_shouldPropagateException_whenRepositoryThrows() {
            when(userRepository.save(sampleUser)).thenThrow(new RuntimeException("Persistence error"));

            assertThatThrownBy(() -> userService.saveUser(sampleUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Persistence error");

            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should pass null user to repository when null is provided")
        void saveUser_shouldPassNullToRepository_whenUserIsNull() {
            when(userRepository.save(null)).thenThrow(new IllegalArgumentException("Entity must not be null"));

            assertThatThrownBy(() -> userService.saveUser(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Entity must not be null");

            verify(userRepository, times(1)).save(null);
        }
    }

    @Nested
    @DisplayName("userData() tests")
    class UserDataTests {

        @Test
        @DisplayName("Should return UserDataDto when user data is found with profile and room")
        void userData_shouldReturnUserDataDto_whenFound() {
            UserDataDto mockData = UserDataDto.builder()
                    .id(1L)
                    .email("john.doe@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .bio("Developer bio")
                    .roomsId(10L)
                    .build();

            when(userRepository.findUserByIdWithProfileAndRoom(1L)).thenReturn(Optional.of(mockData));

            UserDataDto result = userService.userData(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getBio()).isEqualTo("Developer bio");
            assertThat(result.getRoomsId()).isEqualTo(10L);

            verify(userRepository, times(1)).findUserByIdWithProfileAndRoom(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user data is not found")
        void userData_shouldThrowResourceNotFoundException_whenNotFound() {
            when(userRepository.findUserByIdWithProfileAndRoom(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.userData(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id 999 not found");

            verify(userRepository, times(1)).findUserByIdWithProfileAndRoom(999L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for boundary values: 0L, negative, and extreme IDs")
        void userData_shouldThrowResourceNotFoundException_forBoundaryIds() {
            when(userRepository.findUserByIdWithProfileAndRoom(0L)).thenReturn(Optional.empty());
            when(userRepository.findUserByIdWithProfileAndRoom(-1L)).thenReturn(Optional.empty());
            when(userRepository.findUserByIdWithProfileAndRoom(Long.MAX_VALUE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.userData(0L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id 0 not found");

            assertThatThrownBy(() -> userService.userData(-1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id -1 not found");

            assertThatThrownBy(() -> userService.userData(Long.MAX_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id " + Long.MAX_VALUE + " not found");

            when(userRepository.findUserByIdWithProfileAndRoom(Long.MIN_VALUE)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.userData(Long.MIN_VALUE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id " + Long.MIN_VALUE + " not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when userId is null (null boundary)")
        void userData_shouldThrowResourceNotFoundException_whenUserIdIsNull() {
            when(userRepository.findUserByIdWithProfileAndRoom(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.userData(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id null not found");

            verify(userRepository, times(1)).findUserByIdWithProfileAndRoom(null);
        }

        @Test
        @DisplayName("Should propagate exception when repository query fails downstream")
        void userData_shouldPropagateException_whenRepositoryThrows() {
            when(userRepository.findUserByIdWithProfileAndRoom(1L))
                    .thenThrow(new RuntimeException("Query error"));

            assertThatThrownBy(() -> userService.userData(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Query error");
        }
    }

    @Nested
    @DisplayName("updateData() tests")
    class UpdateDataTests {

        @Test
        @DisplayName("Should update first and last name, persist user, and return mapped DTO")
        void updateData_shouldUpdateUserAndReturnDto_whenUserExists() {
            UpdateUserDataDto expectedDto = UpdateUserDataDto.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("john.doe@example.com")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenReturn(expectedDto);

            UpdateUserDataDto result = userService.updateData(1L, "Jane", "Smith");

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(sampleUser.getFirstName()).isEqualTo("Jane");
            assertThat(sampleUser.getLastName()).isEqualTo("Smith");

            verify(userRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).save(sampleUser);
            verify(userMapper, times(1)).toUserUpdateDataDto(sampleUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void updateData_shouldThrowResourceNotFoundException_whenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateData(999L, "Jane", "Smith"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id: 999 is not found");

            verify(userRepository, times(1)).findById(999L);
            verify(userRepository, never()).save(any());
            verify(userMapper, never()).toUserUpdateDataDto(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when userId is null (null boundary)")
        void updateData_shouldThrowResourceNotFoundException_whenUserIdIsNull() {
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateData(null, "Jane", "Smith"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id: null is not found");

            verify(userRepository, times(1)).findById(null);
            verify(userRepository, never()).save(any());
            verify(userMapper, never()).toUserUpdateDataDto(any());
        }

        @Test
        @DisplayName("Should allow setting null and empty strings for first and last names (boundary values)")
        void updateData_shouldAllowNullAndEmptyNames() {
            UpdateUserDataDto nullNamesDto = UpdateUserDataDto.builder().firstName(null).lastName(null).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenReturn(nullNamesDto);

            UpdateUserDataDto result = userService.updateData(1L, null, null);

            assertThat(result).isNotNull();
            assertThat(sampleUser.getFirstName()).isNull();
            assertThat(sampleUser.getLastName()).isNull();

            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should handle empty and whitespace-only strings for names (boundary values)")
        void updateData_shouldHandleEmptyAndWhitespaceNames() {
            UpdateUserDataDto whitespaceDto = UpdateUserDataDto.builder().firstName("").lastName("   ").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenReturn(whitespaceDto);

            UpdateUserDataDto result = userService.updateData(1L, "", "   ");

            assertThat(result).isNotNull();
            assertThat(sampleUser.getFirstName()).isEqualTo("");
            assertThat(sampleUser.getLastName()).isEqualTo("   ");

            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should handle partial null inputs (one name null, other populated)")
        void updateData_shouldHandlePartialNullNames() {
            UpdateUserDataDto partialDto = UpdateUserDataDto.builder().firstName("Jane").lastName(null).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenReturn(partialDto);

            UpdateUserDataDto result = userService.updateData(1L, "Jane", null);

            assertThat(result).isNotNull();
            assertThat(sampleUser.getFirstName()).isEqualTo("Jane");
            assertThat(sampleUser.getLastName()).isNull();

            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should handle large string values up to boundary length")
        void updateData_shouldHandleLargeStringBoundaryValues() {
            String maxFirstName = "A".repeat(255);
            String maxLastName = "B".repeat(255);
            UpdateUserDataDto maxNamesDto = UpdateUserDataDto.builder()
                    .firstName(maxFirstName)
                    .lastName(maxLastName)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenReturn(maxNamesDto);

            UpdateUserDataDto result = userService.updateData(1L, maxFirstName, maxLastName);

            assertThat(result).isNotNull();
            assertThat(sampleUser.getFirstName()).isEqualTo(maxFirstName);
            assertThat(sampleUser.getLastName()).isEqualTo(maxLastName);

            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("Should propagate exception when userRepository.save fails downstream")
        void updateData_shouldPropagateException_whenRepositorySaveThrows() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenThrow(new RuntimeException("DB update constraint failure"));

            assertThatThrownBy(() -> userService.updateData(1L, "Jane", "Smith"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB update constraint failure");

            verify(userMapper, never()).toUserUpdateDataDto(any());
        }

        @Test
        @DisplayName("Should propagate exception when userMapper fails downstream")
        void updateData_shouldPropagateException_whenMapperThrows() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toUserUpdateDataDto(sampleUser)).thenThrow(new RuntimeException("Mapping error"));

            assertThatThrownBy(() -> userService.updateData(1L, "Jane", "Smith"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping error");
        }
    }
}
