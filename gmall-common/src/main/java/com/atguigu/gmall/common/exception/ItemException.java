package com.atguigu.gmall.common.exception;

/**
 * @author saberlin
 * @create 2021/12/15 22:22
 */
public class ItemException extends RuntimeException{
    public ItemException(){
    }

    public ItemException(String message){
        super(message);
    }
}
