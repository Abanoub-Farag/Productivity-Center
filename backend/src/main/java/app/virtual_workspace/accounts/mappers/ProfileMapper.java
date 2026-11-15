package app.virtual_workspace.accounts.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import app.virtual_workspace.accounts.dtos.data.UpdateUserDataDto;
import app.virtual_workspace.accounts.dtos.profile.UserProfileDataDto;
import app.virtual_workspace.accounts.models.Profile;
import app.virtual_workspace.accounts.models.User;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    @Mapping(target = "profilePictureUrl", source = "profile.imageUrl")
    UserProfileDataDto toUserProfileDataDto(User user, Profile profile);

    @Mapping(target = "profilePictureUrl", source = "profile.imageUrl")
    UserProfileDataDto toUserProfileDataDto(UpdateUserDataDto userDataDto, Profile profile);
}