package com.innerworkflow.ai.config;

import com.innerworkflow.ai.grpc.ApprovalAiServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${grpc.ai-service.host:localhost}")
    private String host;

    @Value("${grpc.ai-service.port:50051}")
    private int port;

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public ApprovalAiServiceGrpc.ApprovalAiServiceBlockingStub approvalAiServiceBlockingStub(ManagedChannel channel) {
        return ApprovalAiServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public ApprovalAiServiceGrpc.ApprovalAiServiceStub approvalAiServiceAsyncStub(ManagedChannel channel) {
        return ApprovalAiServiceGrpc.newStub(channel);
    }
}
