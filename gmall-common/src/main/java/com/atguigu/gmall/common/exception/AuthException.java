package com.atguigu.gmall.common.exception;

/**
 * @author saberlin
 * @create 2021/12/17 18:55
 */
public class AuthException extends RuntimeException{
    public AuthException(){
        super();
    }

    public AuthException(String message) {
        super(message);
    }
}
