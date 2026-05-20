package com.nodevoltex.backend.service;

import com.nodevoltex.backend.dto.LeaderboardEntry;
import com.nodevoltex.backend.dto.ScoreRequest;
import com.nodevoltex.backend.entity.Beatmap;
import com.nodevoltex.backend.entity.Score;
import com.nodevoltex.backend.repository.BeatmapRepository;
import com.nodevoltex.backend.repository.ScoreRepository;
import com.nodevoltex.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final BeatmapRepository beatmapRepository;

    public ScoreService(ScoreRepository scoreRepository, UserRepository userRepository, BeatmapRepository beatmapRepository) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.beatmapRepository = beatmapRepository;
    }

    @Transactional
    public void submitScore(String username, ScoreRequest request) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var beatmap = beatmapRepository.findById(request.getMapId())
                .orElseGet(() -> {
                    var newBeatmap = new Beatmap(
                            request.getMapId(),
                            request.getTitle(),
                            request.getArtist(),
                            request.getDifficulty(),
                            request.getLevel(),
                            null
                    );
                    return beatmapRepository.save(newBeatmap);
                });

        var existingScore = scoreRepository.findByUserAndBeatmap(user, beatmap);

        if (existingScore.isPresent()) {
            if (request.getScore() > existingScore.get().getScore()) {
                updateScoreDetails(existingScore.get(), request);
                scoreRepository.save(existingScore.get());
            }
        } else {
            var newScore = new Score();
            newScore.setUser(user);
            newScore.setBeatmap(beatmap);
            updateScoreDetails(newScore, request);
            scoreRepository.save(newScore);
        }
    }

    private void updateScoreDetails(Score score, ScoreRequest request) {
        score.setScore(request.getScore());
        score.setGrade(request.getGrade());
        score.setMaxCombo(request.getMaxCombo());
        score.setsCriticals(request.getsCriticals());
        score.setCriticals(request.getCriticals());
        score.setNears(request.getNears());
        score.setMids(request.getMids());
        score.setFars(request.getFars());
        score.setMisses(request.getMisses());
        score.setLaserTicks(request.getLaserTicks());
        score.setLaserMisses(request.getLaserMisses());
        score.setEarly(request.getEarly());
        score.setLate(request.getLate());
        score.setReplayDataJson(request.getReplayDataJson());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(String mapId) {
        var beatmapOpt = beatmapRepository.findById(mapId);
        if (beatmapOpt.isEmpty()) {
            return List.of(); // Return empty list instead of 404/500 which causes client errors
        }

        return scoreRepository.findByBeatmapOrderByScoreDesc(beatmapOpt.get(), PageRequest.of(0, 50))
                .stream()
                .map(score -> new LeaderboardEntry(
                        score.getUser().getUsername(),
                        score.getScore(),
                        score.getGrade(),
                        score.getMaxCombo(),
                        score.getsCriticals(),
                        score.getCriticals(),
                        score.getNears(),
                        score.getMids(),
                        score.getFars(),
                        score.getMisses(),
                        score.getLaserTicks(),
                        score.getLaserMisses(),
                        score.getEarly(),
                        score.getLate(),
                        score.getUser().getProfilePictureUrl(),
                        score.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        score.getReplayDataJson()
                ))
                .collect(Collectors.toList());
    }
}
