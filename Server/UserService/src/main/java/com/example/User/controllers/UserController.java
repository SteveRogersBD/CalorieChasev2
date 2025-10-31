package com.example.User.controllers;

import com.example.User.entities.*;
import com.example.User.response.ApiResponse;
import com.example.User.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }
    
    

    @PostMapping("/register")
    public ApiResponse<User> registerUser(@RequestBody RegisterRequest registerRequest)
    {
        
        return ApiResponse.onSuccess("Registration Successful",
                userService.register(registerRequest));
    }

    @GetMapping("/verify")
    public ApiResponse<User> verifyUser(@RequestParam String email,
                                           @RequestParam String password)
    {
        User user = userService.verifyUser(email,password);
        return ApiResponse.onSuccess("Verification Successful", user);
    }


    @GetMapping("/username/{username}")
    public ApiResponse<User> findByUsername(@PathVariable String username) {
        return ApiResponse.onSuccess("User found successfully",
                userService.findByUsername(username));
    }

}
