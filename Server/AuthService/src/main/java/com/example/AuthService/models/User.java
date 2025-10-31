package com.example.AuthService.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private Long id;
    private String username;
    private String fullName;
    private String role="USER";
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private String dp;
    private String gender;
    private String age;
    private String weight;
    private String height;

}
