package com.yourorg.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findAllByUserIdOrderByGeneratedAtDesc(Long userId);
    void deleteByUserIdAndStatusNot(Long userId, Status status);
}
