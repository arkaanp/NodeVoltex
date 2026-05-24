package com.nodevoltex.backend.repository;

import com.nodevoltex.backend.entity.Beatmap;
import com.nodevoltex.backend.entity.Score;
import com.nodevoltex.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoreRepository extends JpaRepository<Score, UUID> {
    Optional<Score> findByUserAndBeatmap(User user, Beatmap beatmap);
    List<Score> findByBeatmapOrderByScoreDesc(Beatmap beatmap, Pageable pageable);
    List<Score> findTop10ByUserOrderByVolforceDesc(User user);
    List<Score> findByUserOrderByCreatedAtDesc(User user);
}
