package app.virtual_workspace.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.tasks.models.Task;
import app.virtual_workspace.tasks.repositories.TaskRepository;

@ExtendWith(MockitoExtension.class)
public class TaskAuthServiceTest {

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskAuthService taskAuthService;

    private User authUser;
    private User otherUser;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        authUser = User.builder()
                .id(1L)
                .email("auth@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        sampleTask = Task.builder()
                .id(10L)
                .title("Sample Task")
                .user(authUser)
                .build();
    }

    @Nested
    @DisplayName("isOwner() tests")
    class IsOwnerTests {

        @Test
        @DisplayName("Should return true when authenticated user is the owner of the task")
        void isOwner_shouldReturnTrue_whenUserIsOwner() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(taskRepository.findTasksById(10L)).thenReturn(sampleTask);

            boolean result = taskAuthService.isOwner(10L);

            assertThat(result).isTrue();
            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(taskRepository, times(1)).findTasksById(10L);
        }

        @Test
        @DisplayName("Should return false when authenticated user is not the owner of the task")
        void isOwner_shouldReturnFalse_whenUserIsNotOwner() {
            sampleTask.setUser(otherUser);
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(taskRepository.findTasksById(10L)).thenReturn(sampleTask);

            boolean result = taskAuthService.isOwner(10L);

            assertThat(result).isFalse();
            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(taskRepository, times(1)).findTasksById(10L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task does not exist (null)")
        void isOwner_shouldThrowResourceNotFoundException_whenTaskNotFound() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(taskRepository.findTasksById(999L)).thenReturn(null);

            assertThatThrownBy(() -> taskAuthService.isOwner(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No task found with id: 999");

            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(taskRepository, times(1)).findTasksById(999L);
        }

        @Test
        @DisplayName("Should handle boundary task IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void isOwner_shouldHandleBoundaryTaskIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                Task boundaryTask = Task.builder()
                        .id(id)
                        .title("Boundary Task")
                        .user(authUser)
                        .build();

                when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
                when(taskRepository.findTasksById(id)).thenReturn(boundaryTask);

                boolean result = taskAuthService.isOwner(id);

                assertThat(result).isTrue();
                verify(taskRepository, times(1)).findTasksById(id);
            }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when taskId is null and task is not found")
        void isOwner_shouldThrowResourceNotFoundException_whenTaskIdIsNull() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(taskRepository.findTasksById(null)).thenReturn(null);

            assertThatThrownBy(() -> taskAuthService.isOwner(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No task found with id: null");

            verify(taskRepository, times(1)).findTasksById(null);
        }

        @Test
        @DisplayName("Should propagate exception when userAuthService.getAuthenticatedUser fails")
        void isOwner_shouldPropagateException_whenUserAuthServiceThrows() {
            when(userAuthService.getAuthenticatedUser())
                    .thenThrow(new ResourceNotFoundException("No authenticated user"));

            assertThatThrownBy(() -> taskAuthService.isOwner(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No authenticated user");

            verify(taskRepository, times(0)).findTasksById(any());
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository.findTasksById fails downstream")
        void isOwner_shouldPropagateException_whenTaskRepositoryThrows() {
            when(userAuthService.getAuthenticatedUser()).thenReturn(authUser);
            when(taskRepository.findTasksById(10L))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> taskAuthService.isOwner(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(taskRepository, times(1)).findTasksById(10L);
        }
    }
}
