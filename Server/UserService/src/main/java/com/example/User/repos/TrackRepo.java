package com.example.User.repos;

import com.example.User.entities.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepo extends JpaRepository<Track, Long> {
}
