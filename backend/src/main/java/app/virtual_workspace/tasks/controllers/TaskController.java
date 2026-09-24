package app.virtual_workspace.tasks.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.shared.dtos.ApiResponse;
import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.dtos.UpdateTaskDto;
import app.virtual_workspace.tasks.services.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
public class TaskController {

        private final TaskService taskService;

        @GetMapping("")
        public ResponseEntity<ApiResponse<Slice<TaskResponseDto>>> getAllTasks(
                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                Slice<TaskResponseDto> tasks = taskService.getAllTasks(userPrincipal.getId(), pageable);

                ApiResponse<Slice<TaskResponseDto>> response = ApiResponse.<Slice<TaskResponseDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Tasks retrieved successfully")
                                .data(tasks)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PostMapping("")
        public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(
                        @Valid @RequestBody CreateTaskDto taskRequest,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                TaskResponseDto taskResponseDto = taskService.createTask(userPrincipal.getId(), taskRequest);

                ApiResponse<TaskResponseDto> response = ApiResponse.<TaskResponseDto>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Task created successfully")
                                .data(taskResponseDto)
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping("/{taskId}")
        public ResponseEntity<ApiResponse<TaskResponseDto>> updateTask(
                        @PathVariable Long taskId,
                        @Valid @RequestBody UpdateTaskDto updateTaskDto,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                TaskResponseDto task = taskService.updateTask(userPrincipal.getId(), taskId, updateTaskDto);

                ApiResponse<TaskResponseDto> response = ApiResponse.<TaskResponseDto>builder()
                                .status(HttpStatus.OK.value())
                                .message("Task updated successfully")
                                .data(task)
                                .build();

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{taskId}")
        public ResponseEntity<ApiResponse<Void>> deleteTask(
                        @PathVariable Long taskId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                taskService.deleteTask(userPrincipal.getId(), taskId);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Task deleted successfully")
                                .build();

                return ResponseEntity.ok(response);
        }
}