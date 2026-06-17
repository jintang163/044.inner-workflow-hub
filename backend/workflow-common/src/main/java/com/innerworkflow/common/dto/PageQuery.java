package com.innerworkflow.common.dto;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用分页查询参数DTO
 * <p>
 * 用于接收前端传递的分页和排序参数
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认页码
     */
    private static final Integer DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    private static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小限制
     */
    private static final Integer MAX_PAGE_SIZE = 500;

    /**
     * 允许的排序字符（防止SQL注入）
     */
    private static final String ORDER_BY_PATTERN = "^[a-zA-Z0-9_, ]+$";

    /**
     * 当前页码（从1开始）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 500, message = "每页大小不能超过500")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 排序字段（多个字段用逗号分隔）
     */
    private String orderByColumn;

    /**
     * 排序方向（asc/desc），与orderByColumn一一对应，多个用逗号分隔
     */
    private String isAsc;

    /**
     * 构建MyBatis-Plus分页对象（无排序）
     */
    public <T> Page<T> buildPage() {
        return buildPage(null);
    }

    /**
     * 构建MyBatis-Plus分页对象（支持排序）
     *
     * @param defaultOrder 默认排序规则，格式如 "create_time desc"
     */
    public <T> Page<T> buildPage(String defaultOrder) {
        Page<T> page = new Page<>(this.pageNum, this.pageSize);

        // 处理排序
        List<OrderItem> orderItems = buildOrderItems(defaultOrder);
        if (!orderItems.isEmpty()) {
            page.addOrder(orderItems);
        }

        return page;
    }

    /**
     * 构建排序项列表
     */
    private List<OrderItem> buildOrderItems(String defaultOrder) {
        String columns = this.orderByColumn;
        String directions = this.isAsc;

        // 如果没有指定排序，使用默认排序
        if (StrUtil.isBlank(columns) && StrUtil.isNotBlank(defaultOrder)) {
            String[] parts = defaultOrder.trim().split("\\s+");
            if (parts.length == 1) {
                columns = parts[0];
                directions = "asc";
            } else if (parts.length == 2) {
                columns = parts[0];
                directions = parts[1];
            }
        }

        if (StrUtil.isBlank(columns)) {
            return List.of();
        }

        // 防SQL注入校验
        if (!columns.matches(ORDER_BY_PATTERN)) {
            return List.of();
        }

        String[] columnArray = columns.split(",");
        String[] directionArray = directions != null ? directions.split(",") : new String[0];

        return Arrays.stream(columnArray)
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(column -> {
                    int index = Arrays.asList(columnArray).indexOf(column);
                    boolean isAscending = index < directionArray.length
                            ? "asc".equalsIgnoreCase(directionArray[index].trim())
                            : true;
                    return isAscending ? OrderItem.asc(column) : OrderItem.desc(column);
                })
                .collect(Collectors.toList());
    }
}
