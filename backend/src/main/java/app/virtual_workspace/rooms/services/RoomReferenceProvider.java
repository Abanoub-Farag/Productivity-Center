package app.virtual_workspace.rooms.services;

import org.springframework.stereotype.Service;

import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RoomReferenceProvider {

    private final RoomRepository roomRepository;

    public Room getReference(Long roomId) {
        return roomRepository.getReferenceById(roomId);
    }

}
