package com.innerworkflow.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.innerworkflow")
@MapperScan(basePackages = {
        "com.innerworkflow.auth.mapper",
        "com.innerworkflow.bpmn.mapper",
        "com.innerworkflow.form.mapper",
        "com.innerworkflow.approval.mapper",
        "com.innerworkflow.notify.mapper",
        "com.innerworkflow.ai.mapper"
})
@EnableAsync
@EnableScheduling
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
