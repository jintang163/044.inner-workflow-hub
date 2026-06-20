package com.innerworkflow.approval.config;

import com.innerworkflow.approval.task.WfEscalationQuartzJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    private static final String ESCALATION_JOB_IDENTITY = "escalationJob";
    private static final String ESCALATION_TRIGGER_IDENTITY = "escalationTrigger";

    @Bean
    public JobDetail escalationJobDetail() {
        return JobBuilder.newJob(WfEscalationQuartzJob.class)
                .withIdentity(ESCALATION_JOB_IDENTITY)
                .storeDurably()
                .withDescription("超时升级扫描任务")
                .build();
    }

    @Bean
    public Trigger escalationTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 */5 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(escalationJobDetail())
                .withIdentity(ESCALATION_TRIGGER_IDENTITY)
                .withSchedule(scheduleBuilder)
                .withDescription("超时升级触发器-每5分钟执行一次")
                .build();
    }
}
