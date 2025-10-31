package com.example.ActivityService.clients;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "score-service", url = "http://localhost:8083")
public interface ScoreClient {

}
