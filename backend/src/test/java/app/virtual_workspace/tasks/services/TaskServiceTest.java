package app.virtual_workspace.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.accounts.services.UserAuthService;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.dtos.UpdateTaskDto;
import app.virtual_workspace.tasks.mappers.TaskMapper;
import app.virtual_workspace.tasks.models.Task;
import app.virtual_workspace.tasks.repositories.TaskRepository;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private UserAuthService userAuthService;

    @InjectMocks
    private TaskService taskService;

    private User sampleUser;
    private Task sampleTask;
    private TaskResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        sampleTask = Task.builder()
                .id(10L)
                .title("Initial Task")
                .isCompleted(false)
                .user(sampleUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponseDto = TaskResponseDto.builder()
                .id(10L)
                .title("Initial Task")
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllTasks() tests")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return mapped slice of tasks when user has tasks")
        void getAllTasks_shouldReturnMappedSlice_whenTasksExist() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<Task> taskSlice = new SliceImpl<>(List.of(sampleTask), pageable, false);

            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskRepository.findTasksByUserId(1L, pageable)).thenReturn(taskSlice);
            when(taskMapper.toAllTasksResponseDto(sampleTask)).thenReturn(sampleResponseDto);

            Slice<TaskResponseDto> result = taskService.getAllTasks(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(10L);
            assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Initial Task");

            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(taskRepository, times(1)).findTasksByUserId(1L, pageable);
            verify(taskMapper, times(1)).toAllTasksResponseDto(sampleTask);
        }

        @Test
        @DisplayName("Should return empty slice when user has no tasks")
        void getAllTasks_shouldReturnEmptySlice_whenNoTasks() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<Task> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskRepository.findTasksByUserId(1L, pageable)).thenReturn(emptySlice);

            Slice<TaskResponseDto> result = taskService.getAllTasks(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(taskRepository, times(1)).findTasksByUserId(1L, pageable);
            verify(taskMapper, never()).toAllTasksResponseDto(any());
        }

        @Test
        @DisplayName("Should propagate exception when userAuthService fails")
        void getAllTasks_shouldPropagateException_whenUserAuthServiceThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userAuthService.getAuthenticatedUser())
                    .thenThrow(new ResourceNotFoundException("No authenticated user"));

            assertThatThrownBy(() -> taskService.getAllTasks(pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("No authenticated user");

            verify(taskRepository, never()).findTasksByUserId(any(), any());
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository fails downstream")
        void getAllTasks_shouldPropagateException_whenRepositoryThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskRepository.findTasksByUserId(1L, pageable))
                    .thenThrow(new RuntimeException("Database query failed"));

            assertThatThrownBy(() -> taskService.getAllTasks(pageable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database query failed");
        }
    }

    @Nested
    @DisplayName("createTask() tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task and associate with authenticated user")
        void createTask_shouldSaveTaskWithAuthenticatedUser() {
            CreateTaskDto request = CreateTaskDto.builder()
                    .title("New Task")
                    .isCompleted(false)
                    .build();

            Task mappedTask = Task.builder()
                    .title("New Task")
                    .isCompleted(false)
                    .build();

            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskMapper.toModel(request)).thenReturn(mappedTask);

            taskService.createTask(request);

            verify(userAuthService, times(1)).getAuthenticatedUser();
            verify(taskMapper, times(1)).toModel(request);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());

            Task savedTask = taskCaptor.getValue();
            assertThat(savedTask).isNotNull();
            assertThat(savedTask.getUser()).isEqualTo(sampleUser);
            assertThat(savedTask.getTitle()).isEqualTo("New Task");
            assertThat(savedTask.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("Should save task when isCompleted is true")
        void createTask_shouldSaveTask_whenCompletedIsTrue() {
            CreateTaskDto request = CreateTaskDto.builder()
                    .title("Completed Task")
                    .isCompleted(true)
                    .build();

            Task mappedTask = Task.builder()
                    .title("Completed Task")
                    .isCompleted(true)
                    .build();

            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskMapper.toModel(request)).thenReturn(mappedTask);

            taskService.createTask(request);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should propagate exception when userAuthService fails during creation")
        void createTask_shouldPropagateException_whenUserAuthServiceThrows() {
            CreateTaskDto request = CreateTaskDto.builder().title("Task").build();
            when(userAuthService.getAuthenticatedUser())
                    .thenThrow(new ResourceNotFoundException("No authenticated user"));

            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository.save fails")
        void createTask_shouldPropagateException_whenRepositoryThrows() {
            CreateTaskDto request = CreateTaskDto.builder().title("Task").build();
            Task mappedTask = new Task();

            when(userAuthService.getAuthenticatedUser()).thenReturn(sampleUser);
            when(taskMapper.toModel(request)).thenReturn(mappedTask);
            when(taskRepository.save(mappedTask)).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }

    @Nested
    @DisplayName("updateTask() tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update both title and isCompleted when both are provided")
        void updateTask_shouldUpdateBothTitleAndCompleted_whenBothNonNull() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder()
                    .title("Updated Title")
                    .isCompleted(true)
                    .build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(10L, updateDto);

            assertThat(sampleTask.getTitle()).isEqualTo("Updated Title");
            assertThat(sampleTask.isCompleted()).isTrue();
            verify(taskRepository, times(1)).save(sampleTask);
        }

        @Test
        @DisplayName("Should update only title when isCompleted is null")
        void updateTask_shouldUpdateOnlyTitle_whenCompletedIsNull() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder()
                    .title("Updated Title Only")
                    .isCompleted(null)
                    .build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(10L, updateDto);

            assertThat(sampleTask.getTitle()).isEqualTo("Updated Title Only");
            assertThat(sampleTask.isCompleted()).isFalse(); // remains original false
            verify(taskRepository, times(1)).save(sampleTask);
        }

        @Test
        @DisplayName("Should update only isCompleted when title is null")
        void updateTask_shouldUpdateOnlyCompleted_whenTitleIsNull() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder()
                    .title(null)
                    .isCompleted(true)
                    .build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(10L, updateDto);

            assertThat(sampleTask.getTitle()).isEqualTo("Initial Task"); // remains original
            assertThat(sampleTask.isCompleted()).isTrue();
            verify(taskRepository, times(1)).save(sampleTask);
        }

        @Test
        @DisplayName("Should leave fields unchanged when both title and isCompleted are null")
        void updateTask_shouldLeaveFieldsUnchanged_whenBothNull() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder()
                    .title(null)
                    .isCompleted(null)
                    .build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(10L, updateDto);

            assertThat(sampleTask.getTitle()).isEqualTo("Initial Task");
            assertThat(sampleTask.isCompleted()).isFalse();
            verify(taskRepository, times(1)).save(sampleTask);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task does not exist")
        void updateTask_shouldThrowResourceNotFoundException_whenTaskNotFound() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Title").build();
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(999L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task Not Found");

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle boundary task IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void updateTask_shouldHandleBoundaryTaskIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Boundary").build();

            for (long id : boundaryIds) {
                Task task = Task.builder().id(id).title("Old").build();
                when(taskRepository.findById(id)).thenReturn(Optional.of(task));

                taskService.updateTask(id, updateDto);

                assertThat(task.getTitle()).isEqualTo("Boundary");
                verify(taskRepository, times(1)).save(task);
            }
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository.save fails")
        void updateTask_shouldPropagateException_whenSaveThrows() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Title").build();
            when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(sampleTask)).thenThrow(new RuntimeException("Save failed"));

            assertThatThrownBy(() -> taskService.updateTask(10L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Save failed");
        }
    }

    @Nested
    @DisplayName("deleteTask() tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task when task exists")
        void deleteTask_shouldDeleteTask_whenTaskExists() {
            when(taskRepository.existsById(10L)).thenReturn(true);

            taskService.deleteTask(10L);

            verify(taskRepository, times(1)).existsById(10L);
            verify(taskRepository, times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("Should not delete task when task does not exist")
        void deleteTask_shouldNotCallDelete_whenTaskDoesNotExist() {
            when(taskRepository.existsById(999L)).thenReturn(false);

            taskService.deleteTask(999L);

            verify(taskRepository, times(1)).existsById(999L);
            verify(taskRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should handle boundary task IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void deleteTask_shouldHandleBoundaryTaskIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                when(taskRepository.existsById(id)).thenReturn(true);

                taskService.deleteTask(id);

                verify(taskRepository, times(1)).deleteById(id);
            }
        }

        @Test
        @DisplayName("Should propagate exception when existsById throws")
        void deleteTask_shouldPropagateException_whenExistsByIdThrows() {
            when(taskRepository.existsById(10L)).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> taskService.deleteTask(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(taskRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should propagate exception when deleteById throws")
        void deleteTask_shouldPropagateException_whenDeleteByIdThrows() {
            when(taskRepository.existsById(10L)).thenReturn(true);
            org.mockito.Mockito.doThrow(new RuntimeException("Delete error"))
                    .when(taskRepository).deleteById(10L);

            assertThatThrownBy(() -> taskService.deleteTask(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete error");

            verify(taskRepository, times(1)).deleteById(10L);
        }
    }
}
