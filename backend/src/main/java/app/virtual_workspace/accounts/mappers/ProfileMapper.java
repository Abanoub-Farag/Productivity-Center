package app.virtual_workspace.accounts.mappers;

import org.mapstruct.Mapper;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    UserProfileDataDto toUserProfileDataDto(User user, Profile profile);

    UserProfileDataDto toUserProfileDataDto(UpdateUserDataDto userDataDto, Profile profile);
}
