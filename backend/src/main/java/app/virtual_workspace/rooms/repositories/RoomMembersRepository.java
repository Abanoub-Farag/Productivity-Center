package app.virtual_workspace.rooms.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto;
import app.virtual_workspace.rooms.models.RoomMembers;

public interface RoomMembersRepository extends JpaRepository<RoomMembers, Long> {

    boolean existsByUserIdAndRoomId(Long userId, Long roomId);

    @Transactional
    @Modifying
    @Query("UPDATE RoomMembers r SET r.lastActiveAt = :localDateTime WHERE r.user.id = :userId AND r.room.id = :roomId")
    void updateLastActiveAt(@Param("userId") Long userId, @Param("roomId") Long roomId,
            @Param("localDateTime") LocalDateTime localDateTime);

    @Transactional
    @Modifying
    @Query("UPDATE RoomMembers r SET r.status = 'OFFLINE' WHERE r.lastActiveAt < :localDateTime AND r.status = 'ONLINE'")
    void disconnectNonActiveUsers(@Param("localDateTime") LocalDateTime localDateTime);

    List<RoomMembers> findRoomMembersByRoomId(Long roomId);

    @Query("""
                SELECT new app.virtual_workspace.rooms.dtos.RoomMembers.RoomMemberDto(
                    u.id,
                    u.firstName,
                    u.lastName,
                    p.bio,
                    rm.status,
                    p.gender,
                    p.dateOfBirth
                )
                FROM RoomMembers rm
                JOIN rm.user u
                LEFT JOIN Profile p ON p.user = u
                WHERE rm.room.id = :roomId
            """)
    List<RoomMemberDto> findByRoomIdWithProfileAndUser(@Param("roomId") Long roomId);
}
