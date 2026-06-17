package com.innerworkflow.api.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.innerworkflow.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserIdOrNull();
        } catch (Exception e) {
            log.debug("获取当前用户失败，使用null作为createBy/updateBy");
        }

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserIdOrNull();
        } catch (Exception e) {
            log.debug("获取当前用户失败，使用null作为updateBy");
        }

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
    }
}
