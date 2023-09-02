package com.chris.gotravels.frameworks.convention.page;

import lombok.Data;

/**
 * 分页请求对象
 * {@link PageRequest}、{@link PageResponse}
 * 可以理解是防腐层的一种实现，不论底层 ORM 框架，对外分页参数属性不变
 */
@Data
public class PageRequest {
    /**
     * 当前页
     */
    private Long current = 1L;

    /**
     * 每页显示条数
     */
    private Long size = 10L;
}

/*
 * 3. 封装分页对象
 *
 * MyBatisPlus 分页对象已经足够好了，为什么还要单独封装分页对象？
 * 对于业务比较单一公司来说，不需要考虑这种情况，但是如果公司业务比较多就会出现不同的场景
 * 我觉得下面两个原因是封装分页对象的主要出发点：
 *       提供出去的 API 包，包里有个对象用了分页对象
 *       但是，依赖这个包的客户端没有使用 MyBatisPlus，难道要因为一个分页对象，把整个 MyBatisPlus 包都依赖进去么？
 *       随着现在技术架构发展日新月异，谁也不能保证哪个框架可以长久留存
 *       如果强依赖 MyBatisPlus，后续如果换了 ORM 框架，对于整体修改是个灾难
 * */