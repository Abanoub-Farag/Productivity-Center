package app.virtual_workspace.rooms.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.rooms.models.Room;
import app.virtual_workspace.rooms.models.enums.Visibility;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Room getRoomById(Long roomId);

    Slice<Room> findBy(Pageable pageable);

    Slice<Room> findByVisibilityNot(Visibility visibility, Pageable pageable);

    Boolean existsByUserId(Long userId);

}
