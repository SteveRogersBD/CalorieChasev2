package com.example.ScoreService.controller;

import com.example.ScoreService.service.ScoreService;
import com.example.ScoreService.entity.Score;
import com.example.ScoreService.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @PostMapping
    public ApiResponse<Score> createScore(@RequestBody Score score) {
        return ApiResponse.onSuccess("Score created successfully!!!",
                scoreService.createScore(score));
    }

    @GetMapping
    public ApiResponse<List<Score>> getAllScores() {
        return ApiResponse.onSuccess("Scores retrieved successfully!!!",
                scoreService.getAllScores());
    }

    @GetMapping("/{id}")
    public ApiResponse<Score> getScoreById(@PathVariable Long id) {
        return ApiResponse.onSuccess("Score retrieved successfully!!!",
                scoreService.getScoreById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Score> updateScore(@PathVariable Long id, @RequestBody Score score) {
        return ApiResponse.onSuccess("Score updated successfully!!!",
                scoreService.updateScore(id, score));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteScore(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return ApiResponse.onSuccess("Score deleted successfully!!!", null);
    }

}
