package app.virtual_workspace.accounts.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import app.virtual_workspace.accounts.dtos.data.UserDataDto;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.rooms.models.Room;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "updatedAt", source = "user.updatedAt")
    @Mapping(target = "roomsId", source = "room.id")
    UserDataDto toUserDataDto(User user, Profile profile, Room room);

    @Mapping(target = "id", source = "user.id")
    UserDataDto toUserDataDto(User user, Profile profile, Long roomsId);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "updatedAt", source = "user.updatedAt")
    @Mapping(target = "roomsId", source = "room.id")
    UserDataDto touserDataDto(User user, Profile profile, Room room);

    @Mapping(target = "id", source = "user.id")
    UserDataDto touserDataDto(User user, Profile profile, Long roomsId);
}
