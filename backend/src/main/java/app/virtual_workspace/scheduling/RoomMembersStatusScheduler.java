package app.virtual_workspace.scheduling;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import app.virtual_workspace.rooms.repositories.RoomMembersRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomMembersStatusScheduler {
    
    private final RoomMembersRepository roomMembersRepository;

    @Scheduled(fixedRate = 30000)
    public void disconnectNonActiveUsers(){

        Instant localDateTime = Instant.now().minusSeconds(30);
        roomMembersRepository.disconnectNonActiveUsers(localDateTime);

    }

}
