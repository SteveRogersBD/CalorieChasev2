package com.example.ScoreService.service;

import com.example.ScoreService.repo.ScoreRepo;
import com.example.ScoreService.entity.Score;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreService {

    @Autowired
    private ScoreRepo scoreRepo;

    public List<Score> getAllScores() {
        return scoreRepo.findAll();
    }

    public Score getScoreById(Long id) {
        return scoreRepo.findById(id).orElseThrow();
    }

    public Score createScore(Score score) {
        return scoreRepo.save(score);
    }

    public Score updateScore(Long id, Score score) {
        score.setId(id);
        return scoreRepo.save(score);
    }

    public void deleteScore(Long id) {
        scoreRepo.deleteById(id);
    }

}
