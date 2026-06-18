package com.innerworkflow.common.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.util.SecurityUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.Set;

public class CustomTenantLineHandler implements TenantLineHandler {

    private final Set<String> ignoreTables;

    public CustomTenantLineHandler(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        return new NullValue();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (ignoreTables != null && ignoreTables.contains(tableName)) {
            return true;
        }
        if (isSuperAdmin()) {
            return true;
        }
        return TenantContext.getTenantId() == null;
    }

    private boolean isSuperAdmin() {
        try {
            return SecurityUtils.hasRole("SUPER_ADMIN");
        } catch (Exception e) {
            return false;
        }
    }
}
