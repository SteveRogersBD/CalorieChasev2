package com.example.ScoreService.repo;

import com.example.ScoreService.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepo extends JpaRepository<Score, Long>{
}
