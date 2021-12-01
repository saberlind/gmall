package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/1 11:45
 */
@Data
public class GroupVo extends AttrGroupEntity {
    private List<AttrEntity> attrEntities;
}
