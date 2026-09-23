package app.virtual_workspace.rooms.controllers;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.virtual_workspace.accounts.dtos.UserPrincipal;
import app.virtual_workspace.rooms.dtos.favoriteroom.FavoriteRoomResponseDto;
import app.virtual_workspace.rooms.services.FavoriteRoomService;
import app.virtual_workspace.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/favorites")
public class FavoriteRoomController {

        private final FavoriteRoomService favoriteRoomService;

        @GetMapping("")
        public ResponseEntity<ApiResponse<Slice<FavoriteRoomResponseDto>>> getFavoriteRooms(
                        @PageableDefault(size = 10, sort = "addedAt", direction = Sort.Direction.DESC) Pageable pageable,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                Slice<FavoriteRoomResponseDto> favoriteRooms = favoriteRoomService.getFavoriteRooms(
                                userPrincipal.getId(),
                                pageable);
                ApiResponse<Slice<FavoriteRoomResponseDto>> response = ApiResponse
                                .<Slice<FavoriteRoomResponseDto>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Returned Favorite Rooms Successfully")
                                .data(favoriteRooms)
                                .build();
                return ResponseEntity.ok().body(response);
        }

        @PostMapping("/{roomId}")
        public ResponseEntity<ApiResponse<Void>> addRoomToFavorite(
                        @PathVariable Long roomId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                favoriteRoomService.addRoomToFavorite(userPrincipal.getId(), roomId);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Added room to favorites successfully")
                                .build();

                return ResponseEntity.ok().body(response);
        }

        @DeleteMapping("/{roomId}")
        public ResponseEntity<ApiResponse<Void>> removeRoomFromFavorite(
                        @PathVariable Long roomId,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                favoriteRoomService.removeRoomFromFavorite(userPrincipal.getId(), roomId);

                ApiResponse<Void> response = ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Removed room from favorites successfully")
                                .build();

                return ResponseEntity.ok().body(response);
        }
}