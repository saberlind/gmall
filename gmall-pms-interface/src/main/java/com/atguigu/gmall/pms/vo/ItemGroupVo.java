package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/14 23:42
 */
@Data
public class ItemGroupVo {

    private Long id;
    private String name;

    private List<AttrValueVo> attrValues;
}
