package com.nodevoltex.backend.controller;

import com.nodevoltex.backend.entity.Beatmap;
import com.nodevoltex.backend.repository.BeatmapRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beatmaps")
public class BeatmapController {

    private final BeatmapRepository beatmapRepository;

    public BeatmapController(BeatmapRepository beatmapRepository) {
        this.beatmapRepository = beatmapRepository;
    }

    @GetMapping
    public ResponseEntity<List<Beatmap>> getBeatmaps() {
        return ResponseEntity.ok(beatmapRepository.findAll());
    }
}
