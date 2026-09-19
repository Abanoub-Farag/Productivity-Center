package app.virtual_workspace.tasks.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.dtos.UpdateTaskDto;
import app.virtual_workspace.tasks.mappers.TaskMapper;
import app.virtual_workspace.tasks.models.Task;
import app.virtual_workspace.tasks.repositories.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public Slice<TaskResponseDto> getAllTasks(Long userId, Pageable pageable) {
        Slice<Task> tasks = taskRepository.findTasksByUserId(userId, pageable);
        return tasks.map(taskMapper::toAllTasksResponseDto);
    }

    @Transactional
    public TaskResponseDto createTask(Long userId, CreateTaskDto taskRequest) {
        Task task = taskMapper.toModel(taskRequest);
        task.setUserId(userId);
        Task newTask = taskRepository.save(task);
        return taskMapper.taskResponseDto(newTask);
    }

    @Transactional
    public void updateTask(Long userId, Long taskId, UpdateTaskDto taskRequest) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task With Id: " + taskId + " Not Found"));

        if (taskRequest.getTitle() != null) {
            task.setTitle(taskRequest.getTitle());
        }
        if (taskRequest.getIsCompleted() != null) {
            task.setCompleted(taskRequest.getIsCompleted());
        }

        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Boolean deleted = taskRepository.deleteByIdAndUserId(taskId, userId);
        if (!deleted) {
            throw new ResourceNotFoundException("Task with id: " + taskId + " not found");
        }
    }

}
