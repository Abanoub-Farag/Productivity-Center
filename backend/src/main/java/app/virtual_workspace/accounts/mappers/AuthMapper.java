package app.virtual_workspace.accounts.mappers;

import org.mapstruct.Mapper;

import app.virtual_workspace.accounts.dtos.auth.CreateUserRequestDto;
import app.virtual_workspace.accounts.models.User;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    User registerDtoToModel(CreateUserRequestDto request);
}