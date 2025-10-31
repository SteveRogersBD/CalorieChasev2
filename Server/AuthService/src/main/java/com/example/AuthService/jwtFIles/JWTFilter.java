package com.example.AuthService.jwtFIles;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;


@Component
public class JWTFilter extends OncePerRequestFilter {
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    MyUserDetailsService userDetailsService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException
    {
        String jwt = jwtUtil.getTokenFromRequest(request);
        try{
            if(jwt != null && jwtUtil.validateToken(jwt))
            {
                String username = jwtUtil.getUsernameFromJWT(jwt);
                MyUserDetails userDetails = (MyUserDetails)
                        userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails
                                ,null,userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        filterChain.doFilter(request, response);


    }
}
