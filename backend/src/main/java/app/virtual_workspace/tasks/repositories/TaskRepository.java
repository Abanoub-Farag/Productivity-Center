package app.virtual_workspace.tasks.repositories;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.tasks.models.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId")
    Slice<Task> findTasksByUserId(@Param("userId") Long userId, Pageable pageable);

    Task findTasksById(Long id);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    Boolean deleteByIdAndUserId(Long id, Long userId);
}
