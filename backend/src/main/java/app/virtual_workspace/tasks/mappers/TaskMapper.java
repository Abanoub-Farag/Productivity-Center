package app.virtual_workspace.tasks.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import app.virtual_workspace.tasks.dtos.CreateTaskDto;
import app.virtual_workspace.tasks.dtos.TaskResponseDto;
import app.virtual_workspace.tasks.models.Task;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "completed", target = "isCompleted")
    TaskResponseDto toAllTasksResponseDto(Task tasks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Task toModel(CreateTaskDto taskRequest);

    @Mapping(source = "completed", target = "isCompleted")
    TaskResponseDto taskResponseDto(Task task);
}
