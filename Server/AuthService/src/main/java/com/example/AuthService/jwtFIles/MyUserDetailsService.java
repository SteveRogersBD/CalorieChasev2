package com.example.AuthService.jwtFIles;



import com.example.AuthService.clients.UserClient;
import com.example.AuthService.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    UserClient userClient;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userClient.getUserByUsername(username).getBody();
        return new MyUserDetails(user);
    }
}
