package com.chris.gotravels.userservice.dto.resp;

import com.chris.gotravels.userservice.serialize.IdCardDesensitizationSerializer;
import com.chris.gotravels.userservice.serialize.PhoneDesensitizationSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 乘车人返回参数
 * <p>
 * 前端调用 HTTP 请求获取数据时，
 * SpringMVC 通过 Jackson 进行序列化数据时，
 * 操作证件号码和手机号两个字段就会采用自定义序列化器，完成敏感信息脱敏功能
 */
@Data
@Accessors(chain = true)
public class PassengerRespDTO {
    /**
     * 乘车人id
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号码
     */
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
    private String idCard;

    /**
     * 真实证件号码
     */
    private String actualIdCard;

    /**
     * 优惠类型
     */
    private Integer discountType;

    /**
     * 手机号
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 真实手机号
     */
    private String actualPhone;

    /**
     * 添加日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createDate;

    /**
     * 审核状态
     */
    private Integer verifyStatus;
}
