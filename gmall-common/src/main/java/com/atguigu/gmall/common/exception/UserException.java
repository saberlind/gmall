package com.atguigu.gmall.common.exception;

/**
 * @author saberlin
 * @create 2021/12/16 15:02
 */
public class UserException extends RuntimeException{

    public UserException(){}
    public UserException(String message){
        super(message);
    }
}
