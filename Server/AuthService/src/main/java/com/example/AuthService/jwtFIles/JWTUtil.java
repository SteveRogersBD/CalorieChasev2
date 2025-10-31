package com.example.AuthService.jwtFIles;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.Base64;

@Component
public class JWTUtil {

    private String secretKey;
    public JWTUtil()
    {
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGenerator.generateKey();
            secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
        }catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
    public String getTokenFromRequest(HttpServletRequest request)
    {
        String token = request.getHeader("Authorization");
        if(token!=null && token.startsWith("Bearer "))
        {
            token = token.substring(7);
            return token;
        }
        return null;
    }
    
    public String getUsernameFromJWT(String token)
    {
        return Jwts.parser().setSigningKey(key())
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public boolean validateToken(String token)
    {
        try{
            Jwts.parser().setSigningKey(key()).parseClaimsJws(token);
            return true;
        }catch(MalformedJwtException | ExpiredJwtException | UnsupportedJwtException |
               IllegalArgumentException e){
            e.printStackTrace();
        }
        return false;
    }


    public String createJWTFromUsername(String username)
    {
        return Jwts.builder().setSubject(username).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+60*60*30000))
                .signWith(key())
                .compact();
    }

    

    public SecretKey key()
    {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
