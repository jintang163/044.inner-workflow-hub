package com.innerworkflow.common.context;

public class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    public static void clear() {
        TENANT_HOLDER.remove();
    }

    public static boolean hasTenant() {
        return TENANT_HOLDER.get() != null;
    }
}
