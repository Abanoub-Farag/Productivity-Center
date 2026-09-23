package app.virtual_workspace.rooms.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import app.virtual_workspace.accounts.models.User;
import app.virtual_workspace.rooms.models.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "room_members", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "room_id" })
}, indexes = {
                @Index(name = "idx_room_members_room_id", columnList = "room_id")
})
public class RoomMembers {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
        private Long userId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "room_id", nullable = false)
        private Room room;

        @Column(name = "room_id", nullable = false, insertable = false, updatable = false)
        private Long roomId;

        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Status status = Status.OFFLINE;

        @Column(name = "created_at", nullable = false, updatable = false)
        @CreationTimestamp
        private LocalDateTime createdAt;

        @Column(name = "last_active_at", nullable = false)
        @UpdateTimestamp
        private LocalDateTime lastActiveAt;

}
