package com.chris.gotravels.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chris.gotravels.frameworks.database.base.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注销表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_deletion")
public class UserDeletionDO extends BaseDO {
    /**
     * id
     */
    private Long id;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;
}
