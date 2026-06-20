package com.innerworkflow.approval.vacation.adapter;

import com.innerworkflow.approval.entity.WfUserVacation;
import com.innerworkflow.approval.vacation.DingTalkVacationProperties;
import com.innerworkflow.approval.vacation.VacationCalendarAdapter;
import com.innerworkflow.auth.entity.SysUser;
import com.innerworkflow.auth.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("dingTalkVacationAdapter")
@ConditionalOnProperty(name = "workflow.vacation.dingtalk.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DingTalkVacationAdapter implements VacationCalendarAdapter {

    private final DingTalkVacationProperties properties;
    private final SysUserService sysUserService;
    private final RestTemplate restTemplate;

    private static final String TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String ATTENDANCE_LEAVE_URL = "https://oapi.dingtalk.com/topapi/attendance/getleaveinfo";
    private static final String ATTENDANCE_LIST_URL = "https://oapi.dingtalk.com/topapi/attendance/listRecord";

    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime = 0;

    @Override
    public String getSourceType() {
        return "2";
    }

    @Override
    public String getSourceName() {
        return "钉钉";
    }

    @Override
    public boolean isEnabled() {
        return properties != null && properties.isEnabled();
    }

    @Override
    public List<WfUserVacation> syncUserVacation(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取钉钉AccessToken失败");
                return Collections.emptyList();
            }

            String dingUserId = resolveDingUserId(userId);
            if (dingUserId == null) {
                log.warn("未找到用户的钉钉关联ID, userId={}", userId);
                return Collections.emptyList();
            }

            SysUser user = sysUserService.getById(userId);
            String userName = user != null ? user.getNickname() : "";

            List<WfUserVacation> result = new ArrayList<>();

            long startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userid", dingUserId);
            requestBody.put("start_time", startMillis);
            requestBody.put("end_time", endMillis);
            requestBody.put("offset", 0);
            requestBody.put("size", 50);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = ATTENDANCE_LEAVE_URL + "?access_token=" + accessToken;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer errcode = (Integer) body.get("errcode");
                if (errcode != null && errcode == 0) {
                    Map<String, Object> resultData = (Map<String, Object>) body.get("result");
                    if (resultData != null) {
                        List<Map<String, Object>> leaveInfos = (List<Map<String, Object>>) resultData.get("leave_info");
                        if (leaveInfos != null) {
                            for (Map<String, Object> leaveInfo : leaveInfos) {
                                WfUserVacation vacation = convertToVacation(userId, userName, leaveInfo);
                                if (vacation != null) {
                                    result.add(vacation);
                                }
                            }
                        }
                    }
                } else {
                    log.warn("钉钉查询休假记录返回错误, errcode={}, errmsg={}",
                            body.get("errcode"), body.get("errmsg"));
                }
            }

            return result;
        } catch (Exception e) {
            log.error("从钉钉同步用户休假失败, userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<WfUserVacation> syncUsersVacation(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime) {
        if (!isEnabled() || userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<WfUserVacation> allVacations = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                List<WfUserVacation> vacations = syncUserVacation(userId, startTime, endTime);
                if (vacations != null) {
                    allVacations.addAll(vacations);
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("批量同步钉钉休假失败, userId={}, error={}", userId, e.getMessage());
            }
        }
        return allVacations;
    }

    @Override
    public boolean isUserOnVacation(Long userId, LocalDateTime time) {
        if (!isEnabled() || userId == null || time == null) {
            return false;
        }

        try {
            String accessToken = getAccessToken();
            String dingUserId = resolveDingUserId(userId);
            if (accessToken == null || dingUserId == null) {
                return false;
            }

            long workDate = time.toLocalDate().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userIds", Collections.singletonList(dingUserId));
            requestBody.put("checkDate", workDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = ATTENDANCE_LIST_URL + "?access_token=" + accessToken;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer errcode = (Integer) body.get("errcode");
                if (errcode != null && errcode == 0) {
                    List<Map<String, Object>> records = (List<Map<String, Object>>) body.get("recordresult");
                    if (records != null) {
                        for (Map<String, Object> record : records) {
                            String timeResult = (String) record.get("timeResult");
                            String locationResult = (String) record.get("locationResult");
                            if ("Leave".equals(timeResult) || "NotSigned".equals(timeResult)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("钉钉实时检查休假状态失败, userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public WfUserVacation getCurrentVacation(Long userId) {
        if (!isEnabled() || userId == null) {
            return null;
        }

        try {
            List<WfUserVacation> vacations = syncUserVacation(userId,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

            if (vacations != null) {
                LocalDateTime now = LocalDateTime.now();
                for (WfUserVacation v : vacations) {
                    if (v.getStartTime() != null && v.getEndTime() != null
                            && !now.isBefore(v.getStartTime()) && !now.isAfter(v.getEndTime())) {
                        return v;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("钉钉获取当前休假失败, userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        try {
            String url = TOKEN_URL + "?appkey=" + properties.getAppKey()
                    + "&appsecret=" + properties.getAppSecret();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer errcode = (Integer) body.get("errcode");
                if (errcode != null && errcode == 0) {
                    cachedAccessToken = (String) body.get("access_token");
                    Integer expiresIn = (Integer) body.get("expires_in");
                    tokenExpireTime = System.currentTimeMillis() + (expiresIn != null ? expiresIn * 1000L : 7200 * 1000L);
                    return cachedAccessToken;
                } else {
                    log.error("获取钉钉AccessToken失败, errcode={}, errmsg={}",
                            body.get("errcode"), body.get("errmsg"));
                }
            }
        } catch (Exception e) {
            log.error("获取钉钉AccessToken异常: {}", e.getMessage(), e);
        }

        return null;
    }

    private String resolveDingUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }

        if (user.getThirdPartyUserId() != null && !user.getThirdPartyUserId().isEmpty()) {
            return user.getThirdPartyUserId();
        }

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail();
        }

        return String.valueOf(userId);
    }

    private WfUserVacation convertToVacation(Long userId, String userName, Map<String, Object> leaveInfo) {
        try {
            WfUserVacation vacation = new WfUserVacation();
            vacation.setUserId(userId);
            vacation.setUserName(userName);
            vacation.setSourceType(2);
            vacation.setVacationStatus(1);
            vacation.setAutoDelegate(1);

            Object leaveId = leaveInfo.get("leave_id");
            if (leaveId != null) {
                vacation.setSourceId(String.valueOf(leaveId));
            } else {
                Object id = leaveInfo.get("id");
                vacation.setSourceId(id != null ? String.valueOf(id) : UUID.randomUUID().toString());
            }

            Object type = leaveInfo.get("leave_type");
            vacation.setVacationType(mapVacationType(type != null ? String.valueOf(type) : ""));

            Object reason = leaveInfo.get("leave_reason");
            if (reason != null) {
                vacation.setVacationTitle(String.valueOf(reason));
                vacation.setRemark(String.valueOf(reason));
            } else {
                vacation.setVacationTitle(mapVacationTypeName(vacation.getVacationType()));
            }

            Object startObj = leaveInfo.get("start_time");
            Object endObj = leaveInfo.get("end_time");

            if (startObj instanceof Number && endObj instanceof Number) {
                long startMs = ((Number) startObj).longValue();
                long endMs = ((Number) endObj).longValue();
                vacation.setStartTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault()));
                vacation.setEndTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(endMs), ZoneId.systemDefault()));
            } else {
                return null;
            }

            vacation.setFullDay(isFullDay(vacation.getStartTime(), vacation.getEndTime()) ? 1 : 0);
            vacation.setCreateTime(LocalDateTime.now());
            vacation.setUpdateTime(LocalDateTime.now());

            return vacation;
        } catch (Exception e) {
            log.warn("转换钉钉休假记录失败, leaveInfo={}, error={}", leaveInfo, e.getMessage());
            return null;
        }
    }

    private int mapVacationType(String dingTalkType) {
        if (dingTalkType == null) {
            return 1;
        }
        switch (dingTalkType) {
            case "年假": return 1;
            case "事假": return 2;
            case "病假": return 3;
            case "出差": return 4;
            case "调休": return 5;
            default: return 6;
        }
    }

    private String mapVacationTypeName(Integer type) {
        if (type == null) return "休假";
        switch (type) {
            case 1: return "年假";
            case 2: return "事假";
            case 3: return "病假";
            case 4: return "出差";
            case 5: return "调休";
            default: return "其他";
        }
    }

    private boolean isFullDay(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null
                && start.getHour() == 0 && start.getMinute() == 0
                && (end.getHour() == 23 || end.getHour() == 0);
    }
}
