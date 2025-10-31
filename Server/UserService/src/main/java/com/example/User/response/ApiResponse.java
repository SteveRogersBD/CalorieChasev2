package com.example.User.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private HttpStatus statusCode;
    private String message;
    private T data;
    public static <T>ApiResponse<T>onSuccess(String message, T data)
    {return new ApiResponse<>(HttpStatus.OK, message, data);}
    public static <T>ApiResponse<T>onError(String message)
    {return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, message,null);}
    public static <T>ApiResponse<T>onError(String message,HttpStatus statusCode)
    {return new ApiResponse<>(statusCode, message,null);}
}
