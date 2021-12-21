package com.atguigu.gmall.scheduled.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author saberlin
 * @create 2021/12/19 23:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserInfo {

    private Long userId;
    private String userKey;
}
