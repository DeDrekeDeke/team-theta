package com.example.cvmanager.cv.repository;

import com.example.cvmanager.cv.model.Cv;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CvRepository extends JpaRepository<Cv, Long> {

    List<Cv> findByOwnerId(Long ownerId);

    @Query("""
            SELECT cv FROM Cv cv
            JOIN cv.owner owner
            WHERE cv.archivedAt IS NULL
                AND (
                    lower(cv.title) LIKE lower(concat('%', :query, '%'))
                    OR lower(owner.email) LIKE lower(concat('%', :query, '%'))
                    OR lower(cv.summary) LIKE lower(concat('%', :query, '%'))
                )
            ORDER BY cv.updatedAt DESC
            """)
    List<Cv> search(@Param("query") String query);

    List<Cv> findByArchivedAtIsNull(Sort sort);

    Optional<Cv> findByIdAndArchivedAtIsNull(Long id);
}
