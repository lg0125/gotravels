package com.chris.gotravels.frameworks.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.chris.gotravels.frameworks.common.enums.DelEnum;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * 元数据处理器
 * */
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 数据新增时填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "delFlag", Integer.class, DelEnum.NORMAL.code());
    }

    /**
     * 数据修改时填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
    }
}
/*
* 3. 元数据处理器
*   MyBatisPlus为我们提供了一种便利的方式为这些实体基础属性赋值，那就是元数据处理器
*   可在数据新增或修改，再或者新增和修改时对元数据进行变更
* */
