package com.atguigu.gmall.common.exception;

/**
 * @author saberlin
 * @create 2021/12/22 19:54
 */
public class OrderException extends RuntimeException {

    public OrderException(){}

    public OrderException(String message) {
        super(message);
    }
}
