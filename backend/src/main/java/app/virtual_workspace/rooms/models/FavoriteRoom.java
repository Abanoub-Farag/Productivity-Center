package app.virtual_workspace.rooms.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import app.virtual_workspace.accounts.models.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "favorite_rooms", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
                "room_id" }), indexes = {
                                @Index(name = "idx_fav_room_id", columnList = "room_id")
                })
public class FavoriteRoom {

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

        @CreationTimestamp
        @Column(name = "added_at", nullable = false)
        private LocalDateTime addedAt;

        public Long getUserId() {
                return this.userId != null ? this.userId : (this.user != null ? this.user.getId() : null);
        }

        public Long getRoomId() {
                return this.roomId != null ? this.roomId : (this.room != null ? this.room.getId() : null);
        }
}
