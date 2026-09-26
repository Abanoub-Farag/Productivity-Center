package app.virtual_workspace.tasks.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.accounts.models.enums.Role;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.shared.dtos.ApiResponse;
import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.dtos.UpdateTaskDto;
import app.virtual_workspace.tasks.services.TaskService;

@ExtendWith(MockitoExtension.class)
public class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private UserPrincipal userPrincipal;
    private TaskResponseDto sampleTaskResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("user@example.com")
                .password("password")
                .active(true)
                .role(Role.ROLE_USER)
                .build();

        sampleTaskResponse = TaskResponseDto.builder()
                .id(1L)
                .title("Complete Assignment")
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllTasks() tests")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return 200 OK with slice of tasks")
        void getAllTasks_shouldReturnOkWithTasks() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<TaskResponseDto> taskSlice = new SliceImpl<>(List.of(sampleTaskResponse), pageable, false);

            when(taskService.getAllTasks(1L, pageable)).thenReturn(taskSlice);

            ResponseEntity<ApiResponse<Slice<TaskResponseDto>>> response = taskController.getAllTasks(pageable,
                    userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Tasks retrieved successfully");
            assertThat(response.getBody().getData()).isEqualTo(taskSlice);
            assertThat(response.getBody().getData().getContent()).hasSize(1);

            verify(taskService, times(1)).getAllTasks(1L, pageable);
        }

        @Test
        @DisplayName("Should propagate downstream exception when retrieving tasks")
        void getAllTasks_shouldPropagateException_whenServiceThrows() {
            Pageable pageable = PageRequest.of(0, 10);
            when(taskService.getAllTasks(1L, pageable))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> taskController.getAllTasks(pageable, userPrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }
    }

    @Nested
    @DisplayName("createTask() tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Should return 201 CREATED when task is created successfully")
        void createTask_shouldReturnCreated_whenRequestIsValid() {
            CreateTaskDto request = CreateTaskDto.builder()
                    .title("New Feature")
                    .isCompleted(false)
                    .build();

            when(taskService.createTask(1L, request)).thenReturn(sampleTaskResponse);

            ResponseEntity<ApiResponse<TaskResponseDto>> response = taskController.createTask(request, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Task created successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleTaskResponse);

            verify(taskService, times(1)).createTask(1L, request);
        }

        @Test
        @DisplayName("Should propagate exception when task creation fails downstream")
        void createTask_shouldPropagateException_whenServiceThrows() {
            CreateTaskDto request = CreateTaskDto.builder().title("Failing Task").build();

            doThrow(new RuntimeException("Creation error"))
                    .when(taskService).createTask(1L, request);

            assertThatThrownBy(() -> taskController.createTask(request, userPrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Creation error");
        }
    }

    @Nested
    @DisplayName("updateTask() tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should return 200 OK when task is updated successfully")
        void updateTask_shouldReturnOk_whenUpdateSucceeds() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder()
                    .title("Updated Task")
                    .isCompleted(true)
                    .build();

            when(taskService.updateTask(1L, 1L, updateDto)).thenReturn(sampleTaskResponse);

            ResponseEntity<ApiResponse<TaskResponseDto>> response = taskController.updateTask(1L, updateDto,
                    userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Task updated successfully");
            assertThat(response.getBody().getData()).isEqualTo(sampleTaskResponse);

            verify(taskService, times(1)).updateTask(1L, 1L, updateDto);
        }

        @Test
        @DisplayName("Should update task correctly for boundary task IDs (0L, -1L, Long.MAX_VALUE)")
        void updateTask_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = { 0L, -1L, Long.MAX_VALUE };
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Boundary").build();

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<TaskResponseDto>> response = taskController.updateTask(id, updateDto,
                        userPrincipal);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(taskService, times(1)).updateTask(1L, id, updateDto);
            }
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when task is not found")
        void updateTask_shouldPropagateException_whenTaskNotFound() {
            UpdateTaskDto updateDto = UpdateTaskDto.builder().title("Title").build();

            doThrow(new ResourceNotFoundException("Task Not Found"))
                    .when(taskService).updateTask(1L, 999L, updateDto);

            assertThatThrownBy(() -> taskController.updateTask(999L, updateDto, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task Not Found");
        }
    }

    @Nested
    @DisplayName("deleteTask() tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should return 200 OK when task is deleted successfully")
        void deleteTask_shouldReturnOk_whenDeleteSucceeds() {
            doNothing().when(taskService).deleteTask(1L, 1L);

            ResponseEntity<ApiResponse<Void>> response = taskController.deleteTask(1L, userPrincipal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getBody().getMessage()).isEqualTo("Task deleted successfully");
            assertThat(response.getBody().getData()).isNull();

            verify(taskService, times(1)).deleteTask(1L, 1L);
        }

        @Test
        @DisplayName("Should delete task correctly for boundary task IDs (0L, -1L, Long.MAX_VALUE)")
        void deleteTask_boundaryIds_shouldCallServiceCorrectly() {
            long[] boundaryIds = { 0L, -1L, Long.MAX_VALUE };

            for (long id : boundaryIds) {
                ResponseEntity<ApiResponse<Void>> response = taskController.deleteTask(id, userPrincipal);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(taskService, times(1)).deleteTask(1L, id);
            }
        }

        @Test
        @DisplayName("Should propagate exception when task deletion fails downstream")
        void deleteTask_shouldPropagateException_whenServiceThrows() {
            doThrow(new RuntimeException("Delete error"))
                    .when(taskService).deleteTask(1L, 1L);

            assertThatThrownBy(() -> taskController.deleteTask(1L, userPrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete error");
        }
    }
}
