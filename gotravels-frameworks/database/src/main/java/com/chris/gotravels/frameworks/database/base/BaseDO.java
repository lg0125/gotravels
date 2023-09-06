package com.chris.gotravels.frameworks.database.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 数据持久层基础属性
 */
@Data
public class BaseDO {
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
/*
* 2. 实体基础属性
* 因为每个表都有公共的一些属性，比如创建时间、修改时间以及是否逻辑删除标识
* 避免每个实体上都重复定义,在持久层对象中抽象一个基础的 BaseDO 杜绝上述重复行为
* */
