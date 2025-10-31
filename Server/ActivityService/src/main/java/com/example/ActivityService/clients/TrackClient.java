package com.example.ActivityService.clients;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "track-service", url = "http://localhost:8084")
public interface TrackClient {
}
