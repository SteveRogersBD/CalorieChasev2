package com.example.AuthService.clients;

import com.example.AuthService.models.RegisterRequest;
import com.example.AuthService.models.User;
import com.example.AuthService.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {

    @GetMapping("/user/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username);

    @PostMapping("/user/register")
    public ApiResponse<User> registerUser(@RequestBody RegisterRequest registerRequest);

    @GetMapping("/user/verify")
    public ApiResponse<User> verifyUser(@RequestParam String email,
                                        @RequestParam String password );

    @GetMapping("/username/{username}")
    public ApiResponse<User> findByUsername(@PathVariable String username);

}
