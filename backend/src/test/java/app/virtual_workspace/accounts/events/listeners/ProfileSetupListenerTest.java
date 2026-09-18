package app.virtual_workspace.accounts.events.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.events.UserRegisteredEvent;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.repositories.ProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class ProfileSetupListenerTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProfileSetupListener profileSetupListener;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("onUserRegistered() tests")
    class OnUserRegisteredTests {

        @Test
        @DisplayName("Should create and save profile with referenced user when event is received")
        void onUserRegistered_shouldCreateAndSaveProfile_whenEventReceived() {
            UserRegisteredEvent event = new UserRegisteredEvent(1L);
            when(entityManager.getReference(User.class, 1L)).thenReturn(sampleUser);

            profileSetupListener.onUserRegistered(event);

            verify(entityManager, times(1)).getReference(User.class, 1L);
            ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository, times(1)).save(profileCaptor.capture());

            Profile savedProfile = profileCaptor.getValue();
            assertThat(savedProfile).isNotNull();
            assertThat(savedProfile.getUser()).isEqualTo(sampleUser);
        }

        @Test
        @DisplayName("Should handle boundary user IDs: 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE")
        void onUserRegistered_shouldHandleBoundaryUserIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                User user = User.builder().id(id).build();
                when(entityManager.getReference(User.class, id)).thenReturn(user);

                profileSetupListener.onUserRegistered(new UserRegisteredEvent(id));

                verify(entityManager, times(1)).getReference(User.class, id);
            }
            verify(profileRepository, times(boundaryIds.length)).save(any(Profile.class));
        }

        @Test
        @DisplayName("Should handle null user ID in event")
        void onUserRegistered_shouldHandleNullUserId() {
            UserRegisteredEvent event = new UserRegisteredEvent(null);
            when(entityManager.getReference(User.class, null)).thenReturn(null);

            profileSetupListener.onUserRegistered(event);

            verify(entityManager, times(1)).getReference(User.class, null);
            ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository, times(1)).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getUser()).isNull();
        }

        @Test
        @DisplayName("Should propagate exception when entityManager throws EntityNotFoundException")
        void onUserRegistered_shouldPropagateException_whenEntityManagerThrows() {
            UserRegisteredEvent event = new UserRegisteredEvent(999L);
            when(entityManager.getReference(User.class, 999L))
                    .thenThrow(new EntityNotFoundException("User entity not found"));

            assertThatThrownBy(() -> profileSetupListener.onUserRegistered(event))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User entity not found");

            verify(profileRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("Should propagate exception when profileRepository.save throws downstream")
        void onUserRegistered_shouldPropagateException_whenRepositoryThrows() {
            UserRegisteredEvent event = new UserRegisteredEvent(1L);
            when(entityManager.getReference(User.class, 1L)).thenReturn(sampleUser);
            when(profileRepository.save(any(Profile.class)))
                    .thenThrow(new RuntimeException("Database error saving profile"));

            assertThatThrownBy(() -> profileSetupListener.onUserRegistered(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error saving profile");

            verify(entityManager, times(1)).getReference(User.class, 1L);
            verify(profileRepository, times(1)).save(any(Profile.class));
        }
    }
}
