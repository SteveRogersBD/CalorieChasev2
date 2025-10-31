package com.example.AuthService.controllers;

import com.example.AuthService.clients.UserClient;
import com.example.AuthService.jwtFIles.JWTUtil;
import com.example.AuthService.models.LogInRequest;
import com.example.AuthService.models.RegisterRequest;
import com.example.AuthService.models.User;
import com.example.AuthService.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/auth")
public class AuthController {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JWTUtil jwtUtil;


    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LogInRequest loginRequest)
    {
        ApiResponse<User> response = userClient.verifyUser(loginRequest.getEmail(),
                loginRequest.getPassword());
        User user = response.getData();
        String token = jwtUtil.createJWTFromUsername(user.getUsername());
        return ApiResponse.onSuccess("Login Successful", token);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String,Object>> register(@RequestBody RegisterRequest registrationRequest)
    {
        ApiResponse<User> response = userClient.registerUser(registrationRequest);
        String token = jwtUtil.createJWTFromUsername(registrationRequest.getUsername());
        User user = response.getData();
        return ApiResponse.onSuccess("Registration Successful",
                Map.of("User", user, "Token", token));
    }

    @GetMapping("/verify-token")
    public ApiResponse<Map<String,Object>>tokenVerification(String token) {

        Boolean flag = jwtUtil.validateToken(token);
        if (flag)
        {
            String username = jwtUtil.getUsernameFromJWT(token);
            User user = userClient.findByUsername(username).getData();
            return ApiResponse.onSuccess("Token Validated",
                    Map.of("Token", token,"User", user ));
        }
        return ApiResponse.onError("Token unverified");
    }

}
