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

import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.dtos.UpdateTaskDto;
import app.virtual_workspace.tasks.mappers.TaskMapper;
import app.virtual_workspace.tasks.models.Task;
import app.virtual_workspace.tasks.repositories.TaskRepository;

import app.virtual_workspace.accounts.services.UserReferenceProvider;
import app.virtual_workspace.accounts.models.User;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private UserReferenceProvider userReferenceProvider;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private TaskResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(userReferenceProvider.getReference(any())).thenReturn(User.builder().id(1L).build());
        
        sampleTask = Task.builder()
                .id(10L)
                .title("Initial Task")
                .isCompleted(false)
                .userId(1L)
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

            when(taskRepository.findTasksByUserId(1L, pageable)).thenReturn(taskSlice);
            when(taskMapper.toAllTasksResponseDto(sampleTask)).thenReturn(sampleResponseDto);

            Slice<TaskResponseDto> result = taskService.getAllTasks(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(10L);
            assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Initial Task");

            verify(taskRepository, times(1)).findTasksByUserId(1L, pageable);
            verify(taskMapper, times(1)).toAllTasksResponseDto(sampleTask);
        }

        @Test
        @DisplayName("Should return empty slice when user has no tasks")
        void getAllTasks_shouldReturnEmptySlice_whenNoTasks() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<Task> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);

            when(taskRepository.findTasksByUserId(1L, pageable)).thenReturn(emptySlice);

            Slice<TaskResponseDto> result = taskService.getAllTasks(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(taskRepository, times(1)).findTasksByUserId(1L, pageable);
            verify(taskMapper, never()).toAllTasksResponseDto(any());
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository fails downstream")
        void getAllTasks_shouldPropagateException_whenRepositoryThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(taskRepository.findTasksByUserId(1L, pageable))
                    .thenThrow(new RuntimeException("Database query failed"));

            assertThatThrownBy(() -> taskService.getAllTasks(1L, pageable))
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

            when(taskMapper.toModel(request)).thenReturn(mappedTask);
            when(taskRepository.save(mappedTask)).thenReturn(mappedTask);
            when(taskMapper.taskResponseDto(mappedTask)).thenReturn(sampleResponseDto);

            TaskResponseDto result = taskService.createTask(1L, request);

            verify(taskMapper, times(1)).toModel(request);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());

            Task savedTask = taskCaptor.getValue();
            assertThat(savedTask).isNotNull();
            assertThat(savedTask.getUser().getId()).isEqualTo(1L);
            assertThat(savedTask.getTitle()).isEqualTo("New Task");
            assertThat(savedTask.isCompleted()).isFalse();
            assertThat(result).isEqualTo(sampleResponseDto);
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

            when(taskMapper.toModel(request)).thenReturn(mappedTask);
            when(taskRepository.save(mappedTask)).thenReturn(mappedTask);
            when(taskMapper.taskResponseDto(mappedTask)).thenReturn(sampleResponseDto);

            TaskResponseDto result = taskService.createTask(1L, request);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().isCompleted()).isTrue();
            assertThat(result).isEqualTo(sampleResponseDto);
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository.save fails")
        void createTask_shouldPropagateException_whenRepositoryThrows() {
            CreateTaskDto request = CreateTaskDto.builder().title("Task").build();
            Task mappedTask = new Task();

            when(taskMapper.toModel(request)).thenReturn(mappedTask);
            when(taskRepository.save(mappedTask)).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> taskService.createTask(1L, request))
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

            when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(1L, 10L, updateDto);

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

            when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(1L, 10L, updateDto);

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

            when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(1L, 10L, updateDto);

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

            when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleTask));

            taskService.updateTask(1L, 10L, updateDto);

            assertThat(sampleTask.getTitle()).isEqualTo("Initial Task");
            assertThat(sampleTask.isCompleted()).isFalse();
            verify(taskRepository, times(1)).save(sampleTask);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task does not exist")
        void updateTask_shouldThrowResourceNotFoundException_whenTaskNotFound() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Title").build();
            when(taskRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(1L, 999L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task With Id: 999 Not Found");

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle boundary task IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void updateTask_shouldHandleBoundaryTaskIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Boundary").build();

            for (long id : boundaryIds) {
                Task task = Task.builder().id(id).title("Old").userId(1L).build();
                when(taskRepository.findByIdAndUserId(id, 1L)).thenReturn(Optional.of(task));

                taskService.updateTask(1L, id, updateDto);

                assertThat(task.getTitle()).isEqualTo("Boundary");
                verify(taskRepository, times(1)).save(task);
            }
        }

        @Test
        @DisplayName("Should propagate exception when taskRepository.save fails")
        void updateTask_shouldPropagateException_whenSaveThrows() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Title").build();
            when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(sampleTask)).thenThrow(new RuntimeException("Save failed"));

            assertThatThrownBy(() -> taskService.updateTask(1L, 10L, updateDto))
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
            when(taskRepository.deleteByIdAndUserId(10L, 1L)).thenReturn(1L);

            taskService.deleteTask(1L, 10L);

            verify(taskRepository, times(1)).deleteByIdAndUserId(10L, 1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task does not exist")
        void deleteTask_shouldThrowResourceNotFoundException_whenTaskDoesNotExist() {
            when(taskRepository.deleteByIdAndUserId(999L, 1L)).thenReturn(0L);

            assertThatThrownBy(() -> taskService.deleteTask(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task with id: 999 not found");

            verify(taskRepository, times(1)).deleteByIdAndUserId(999L, 1L);
        }

        @Test
        @DisplayName("Should handle boundary task IDs (0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)")
        void deleteTask_shouldHandleBoundaryTaskIds() {
            long[] boundaryIds = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE};

            for (long id : boundaryIds) {
                when(taskRepository.deleteByIdAndUserId(id, 1L)).thenReturn(1L);

                taskService.deleteTask(1L, id);

                verify(taskRepository, times(1)).deleteByIdAndUserId(id, 1L);
            }
        }

        @Test
        @DisplayName("Should propagate exception when deleteByIdAndUserId throws")
        void deleteTask_shouldPropagateException_whenDeleteByIdAndUserIdThrows() {
            when(taskRepository.deleteByIdAndUserId(10L, 1L)).thenThrow(new RuntimeException("Delete error"));

            assertThatThrownBy(() -> taskService.deleteTask(1L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete error");

            verify(taskRepository, times(1)).deleteByIdAndUserId(10L, 1L);
        }
    }
}
