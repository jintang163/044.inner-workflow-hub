package com.innerworkflow.approval.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class WfBatchRemindResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Integer remindMessageCount;

    private List<RemindFailItem> failItems = new ArrayList<>();

    @Data
    public static class RemindFailItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long taskId;

        private String taskNo;

        private String reason;

        public RemindFailItem(Long taskId, String taskNo, String reason) {
            this.taskId = taskId;
            this.taskNo = taskNo;
            this.reason = reason;
        }
    }
}
