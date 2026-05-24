package com.nodevoltex.backend.service;

import com.nodevoltex.backend.dto.LeaderboardEntry;
import com.nodevoltex.backend.dto.ScoreRequest;
import com.nodevoltex.backend.dto.ScoreSubmitResponse;
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

    private double getGradeCoefficient(String grade) {
        if (grade == null) return 0.0;
        switch (grade.trim().toUpperCase()) {
            case "S": return 1.05;
            case "AAA+": return 1.02;
            case "AAA": return 1.00;
            case "AA+": return 0.97;
            case "AA": return 0.94;
            case "A+": return 0.91;
            case "A": return 0.88;
            case "B": return 0.85;
            case "C": return 0.82;
            case "D": return 0.80;
            default: return 0.00;
        }
    }

    private double getClearCoefficient(Score score) {
        if (score == null) return 1.0;
        int misses = score.getMisses() != null ? score.getMisses() : 0;
        int laserMisses = score.getLaserMisses() != null ? score.getLaserMisses() : 0;
        int nears = score.getNears() != null ? score.getNears() : 0;
        int fars = score.getFars() != null ? score.getFars() : 0;

        boolean isPuc = (misses == 0 && laserMisses == 0 && nears == 0 && fars == 0);
        boolean isUc = (misses == 0 && laserMisses == 0);

        if (isPuc) {
            return 1.1;
        } else if (isUc) {
            return 1.06;
        } else {
            return 1.00;
        }
    }

    @Transactional
    public ScoreSubmitResponse submitScore(String username, ScoreRequest request) {
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

        // 1. Calculate play volforce
        double level = request.getLevel() != null ? request.getLevel() + 0.5 : 0.5;
        double scoreVal = request.getScore() != null ? request.getScore() : 0.0;
        double gradeCoeff = getGradeCoefficient(request.getGrade());
        
        Score tempScore = new Score();
        tempScore.setMisses(request.getMisses());
        tempScore.setLaserMisses(request.getLaserMisses());
        tempScore.setNears(request.getNears());
        tempScore.setFars(request.getFars());
        double clearCoeff = getClearCoefficient(tempScore);

        double playVolforce = (level * (scoreVal / 10000000.0) * gradeCoeff * clearCoeff * 20.0) * 5.0 * 0.001;
        playVolforce = Math.floor(playVolforce * 1000.0) / 1000.0;

        // 2. Fetch old best 10 and sum
        List<Score> oldTop10 = scoreRepository.findTop10ByUserOrderByVolforceDesc(user);
        double oldTotalVolforce = oldTop10.stream().mapToDouble(Score::getVolforce).sum();
        oldTotalVolforce = Math.round(oldTotalVolforce * 1000.0) / 1000.0;

        // 3. Check for high score and save
        var existingScore = scoreRepository.findByUserAndBeatmap(user, beatmap);
        boolean isNewHighScore = false;

        if (existingScore.isPresent()) {
            if (request.getScore() > existingScore.get().getScore()) {
                updateScoreDetails(existingScore.get(), request);
                existingScore.get().setVolforce(playVolforce);
                existingScore.get().setCreatedAt(java.time.LocalDateTime.now());
                scoreRepository.save(existingScore.get());
                isNewHighScore = true;
            }
        } else {
            var newScore = new Score();
            newScore.setUser(user);
            newScore.setBeatmap(beatmap);
            updateScoreDetails(newScore, request);
            newScore.setVolforce(playVolforce);
            newScore.setCreatedAt(java.time.LocalDateTime.now());
            scoreRepository.save(newScore);
            isNewHighScore = true;
        }

        // 4. Fetch new best 10 and sum
        double newTotalVolforce = oldTotalVolforce;
        double volforceGained = 0.0;

        if (isNewHighScore) {
            List<Score> newTop10 = scoreRepository.findTop10ByUserOrderByVolforceDesc(user);
            newTotalVolforce = newTop10.stream().mapToDouble(Score::getVolforce).sum();
            newTotalVolforce = Math.round(newTotalVolforce * 1000.0) / 1000.0;

            volforceGained = newTotalVolforce - oldTotalVolforce;
            volforceGained = Math.round(volforceGained * 1000.0) / 1000.0;
            if (volforceGained < 0.0) {
                volforceGained = 0.0;
            }

            user.setVolforce(newTotalVolforce);
            userRepository.save(user);
        }

        return new ScoreSubmitResponse(playVolforce, newTotalVolforce, volforceGained);
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.nodevoltex.backend.dto.UserScoreDTO> getUserBestScores(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return scoreRepository.findTop10ByUserOrderByVolforceDesc(user).stream()
                .map(this::convertToUserScoreDTO)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.nodevoltex.backend.dto.UserScoreDTO> getUserRecentScores(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return scoreRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToUserScoreDTO)
                .collect(Collectors.toList());
    }

    private com.nodevoltex.backend.dto.UserScoreDTO convertToUserScoreDTO(Score score) {
        return new com.nodevoltex.backend.dto.UserScoreDTO(
                score.getBeatmap().getId(),
                score.getBeatmap().getTitle(),
                score.getBeatmap().getArtist(),
                score.getBeatmap().getDifficulty(),
                score.getBeatmap().getLevel(),
                score.getScore(),
                score.getGrade(),
                score.getVolforce(),
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
                score.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
    }
}
