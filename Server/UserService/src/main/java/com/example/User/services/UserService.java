package com.example.User.services;

import com.example.User.controllers.RegisterRequest;
import com.example.User.exception.UserNotFoundException;
import com.example.User.repos.UserRepo;
import com.example.User.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id).orElseThrow();
    }

    public User createUser(User user) {
        return userRepo.save(user);
    }

    public User updateUser(Long id, User user) {
        user.setId(id);
        return userRepo.save(user);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    public User findByUsername(String username)
    {
        return userRepo.findByUsername(username).orElseThrow(
                ()->new IllegalStateException("User not found: " + username)
        );
    }

    public User findByEmail(String email)
    {
        return userRepo.findByEmail(email).orElseThrow(
                ()->new IllegalStateException("User not found: " + email)
        );
    }

    public boolean verifyUserReg(String username, String email) {
        return userRepo.findByUsername(username).isEmpty()
                && userRepo.findByEmail(email).isEmpty();
    }


    public User register(RegisterRequest user) {
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("Username already exists: " + user.getUsername());
        }
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already exists: " + user.getEmail());
        }
        User user1 = new User();
        user1.setUsername(user.getUsername());
        user1.setEmail(user.getEmail());
        user1.setPassword(passwordEncoder.encode(user.getPassword()));
        user1.setFullName(user.getFullName());
        return userRepo.save(user1);
    }

    public User login(String username, String password) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalStateException("Invalid password");
        }
        return user;
    }

    public User verifyUser(String email, String password)
    {
        if(!userRepo.existsByEmail(email))
            throw new IllegalStateException("User not found: " + email);
        User user = userRepo.findByEmail(email).orElseThrow(
                ()->new UserNotFoundException("User not found with this email: "
                        + email)
        );
        if(!passwordEncoder.matches(password, user.getPassword()))
            throw new IllegalStateException("Invalid password");
        return user;
    }
    
    public User getUserByUsername(String username)
    {
        return userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with username: " +
                                username));
    }


}
