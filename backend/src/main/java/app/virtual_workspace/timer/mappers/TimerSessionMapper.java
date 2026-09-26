package app.virtual_workspace.timer.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import app.virtual_workspace.timer.dtos.TimerSessionResponseDto;
import app.virtual_workspace.timer.models.TimerSession;

@Mapper(componentModel = "spring")
public interface TimerSessionMapper {

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "roomId", target = "roomId")
    TimerSessionResponseDto toDto(TimerSession timerSession);

}
