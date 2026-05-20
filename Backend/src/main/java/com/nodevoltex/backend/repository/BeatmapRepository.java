package com.nodevoltex.backend.repository;

import com.nodevoltex.backend.entity.Beatmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeatmapRepository extends JpaRepository<Beatmap, String> {
}
