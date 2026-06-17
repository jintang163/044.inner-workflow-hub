package com.innerworkflow.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应结果类
 * <p>
 * 用于封装分页查询接口的返回数据
 * </p>
 *
 * @param <T> 数据记录的类型
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据记录列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 构建分页结果
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页
     * @param size    每页大小
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records != null ? records : Collections.emptyList());
        result.setTotal(total != null ? total : 0L);
        result.setCurrent(current != null ? current : 1L);
        result.setSize(size != null ? size : 10L);
        result.setPages(calculatePages(total, size));
        return result;
    }

    /**
     * 从MyBatis-Plus的IPage对象构建分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 从MyBatis-Plus的IPage对象构建分页结果，并转换记录类型
     */
    public static <T, R> PageResult<R> of(IPage<T> page, Function<T, R> converter) {
        List<R> converted = page.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());
        return of(converted, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return of(Collections.emptyList(), 0L, 1L, 10L);
    }

    /**
     * 计算总页数
     */
    private static Long calculatePages(Long total, Long size) {
        if (total == null || total <= 0) {
            return 0L;
        }
        if (size == null || size <= 0) {
            return 1L;
        }
        return (total + size - 1) / size;
    }
}
