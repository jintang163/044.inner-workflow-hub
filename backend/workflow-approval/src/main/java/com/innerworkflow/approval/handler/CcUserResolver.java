package com.innerworkflow.approval.handler;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.innerworkflow.auth.entity.SysDepartment;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysDeptService;
import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.util.JsonUtils;
import com.innerworkflow.common.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CcUserResolver {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_DEPT = "DEPT";
    public static final String TYPE_ROLE = "ROLE";
    public static final String TYPE_ROLE_CODE = "ROLE_CODE";
    public static final String TYPE_START_USER_DEPT = "START_USER_DEPT";
    public static final String TYPE_DEPT_LEADER = "DEPT_LEADER";
    public static final String TYPE_START_USER_DEPT_LEADER = "START_USER_DEPT_LEADER";

    public List<Long> resolveCcUsers(Object ccConfig, Long startUserId, Long startDeptId) {
        Set<Long> ccUserIds = new HashSet<>();

        if (ccConfig == null) {
            return new ArrayList<>(ccUserIds);
        }

        try {
            List<Map<String, Object>> ccList = null;
            if (ccConfig instanceof String) {
                ccList = JsonUtils.parseObject((String) ccConfig, new TypeReference<List<Map<String, Object>>>() {});
            } else if (ccConfig instanceof List) {
                ccList = JsonUtils.parseObject(JsonUtils.toJsonString(ccConfig),
                        new TypeReference<List<Map<String, Object>>>() {});
            }

            if (ccList == null || ccList.isEmpty()) {
                return new ArrayList<>(ccUserIds);
            }

            SysUserService userService = SpringContextHolder.getBean(SysUserService.class);

            for (Map<String, Object> ccItem : ccList) {
                String type = (String) ccItem.get("type");
                Object value = ccItem.get("value");

                if (StrUtil.isBlank(type) || value == null) {
                    continue;
                }

                switch (type) {
                    case TYPE_USER:
                        handleUserType(value, ccUserIds);
                        break;
                    case TYPE_DEPT:
                        handleDeptType(value, ccUserIds, userService);
                        break;
                    case TYPE_ROLE:
                        handleRoleType(value, ccUserIds, userService);
                        break;
                    case TYPE_ROLE_CODE:
                        handleRoleCodeType(value, ccUserIds, userService);
                        break;
                    case TYPE_START_USER_DEPT:
                        handleStartUserDeptType(startDeptId, ccUserIds, userService);
                        break;
                    case TYPE_DEPT_LEADER:
                        handleDeptLeaderType(value, ccUserIds);
                        break;
                    case TYPE_START_USER_DEPT_LEADER:
                        handleStartUserDeptLeaderType(startDeptId, ccUserIds);
                        break;
                    default:
                        log.warn("未知的抄送类型: {}", type);
                }
            }

            if (startUserId != null) {
                ccUserIds.remove(startUserId);
            }

        } catch (Exception e) {
            log.error("解析抄送人失败, ccConfig={}, error={}", ccConfig, e.getMessage(), e);
        }

        return new ArrayList<>(ccUserIds);
    }

    private void handleUserType(Object value, Set<Long> ccUserIds) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item instanceof Number) {
                    ccUserIds.add(((Number) item).longValue());
                } else if (item instanceof String) {
                    try {
                        ccUserIds.add(Long.parseLong((String) item));
                    } catch (NumberFormatException e) {
                        log.warn("用户ID格式错误: {}", item);
                    }
                }
            }
        } else if (value instanceof Number) {
            ccUserIds.add(((Number) value).longValue());
        } else if (value instanceof String) {
            try {
                ccUserIds.add(Long.parseLong((String) value));
            } catch (NumberFormatException e) {
                log.warn("用户ID格式错误: {}", value);
            }
        }
    }

    private void handleDeptType(Object value, Set<Long> ccUserIds, SysUserService userService) {
        if (userService == null) {
            return;
        }
        if (value instanceof Number) {
            Long deptId = ((Number) value).longValue();
            List<SysUser> users = userService.listByDeptId(deptId);
            ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
        } else if (value instanceof String) {
            try {
                Long deptId = Long.parseLong((String) value);
                List<SysUser> users = userService.listByDeptId(deptId);
                ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
            } catch (NumberFormatException e) {
                log.warn("部门ID格式错误: {}", value);
            }
        }
    }

    private void handleRoleType(Object value, Set<Long> ccUserIds, SysUserService userService) {
        if (userService == null) {
            return;
        }
        if (value instanceof Number) {
            Long roleId = ((Number) value).longValue();
            List<SysUser> users = userService.listByRoleId(roleId);
            ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
        } else if (value instanceof String) {
            try {
                Long roleId = Long.parseLong((String) value);
                List<SysUser> users = userService.listByRoleId(roleId);
                ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
            } catch (NumberFormatException e) {
                log.warn("角色ID格式错误: {}", value);
            }
        }
    }

    private void handleRoleCodeType(Object value, Set<Long> ccUserIds, SysUserService userService) {
        if (userService == null || !(value instanceof String)) {
            return;
        }
        String roleCode = (String) value;
        List<SysUser> users = userService.listByRoleCode(roleCode);
        ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
    }

    private void handleStartUserDeptType(Long startDeptId, Set<Long> ccUserIds, SysUserService userService) {
        if (userService == null || startDeptId == null) {
            return;
        }
        List<SysUser> users = userService.listByDeptId(startDeptId);
        ccUserIds.addAll(users.stream().map(SysUser::getId).collect(Collectors.toList()));
    }

    private void handleDeptLeaderType(Object value, Set<Long> ccUserIds) {
        try {
            SysDeptService deptService = SpringContextHolder.getBean(SysDeptService.class);
            if (deptService == null) {
                return;
            }
            if (value instanceof Number) {
                Long deptId = ((Number) value).longValue();
                SysDepartment dept = deptService.getById(deptId);
                if (dept != null && dept.getLeaderUserId() != null) {
                    ccUserIds.add(dept.getLeaderUserId());
                }
            } else if (value instanceof String) {
                try {
                    Long deptId = Long.parseLong((String) value);
                    SysDepartment dept = deptService.getById(deptId);
                    if (dept != null && dept.getLeaderUserId() != null) {
                        ccUserIds.add(dept.getLeaderUserId());
                    }
                } catch (NumberFormatException e) {
                    log.warn("部门ID格式错误: {}", value);
                }
            }
        } catch (Exception e) {
            log.error("处理部门负责人类型失败, value={}, error={}", value, e.getMessage(), e);
        }
    }

    private void handleStartUserDeptLeaderType(Long startDeptId, Set<Long> ccUserIds) {
        if (startDeptId == null) {
            return;
        }
        try {
            SysDeptService deptService = SpringContextHolder.getBean(SysDeptService.class);
            if (deptService == null) {
                return;
            }
            SysDepartment dept = deptService.getById(startDeptId);
            if (dept != null && dept.getLeaderUserId() != null) {
                ccUserIds.add(dept.getLeaderUserId());
            }
        } catch (Exception e) {
            log.error("处理发起人部门负责人类型失败, startDeptId={}, error={}", startDeptId, e.getMessage(), e);
        }
    }
}
