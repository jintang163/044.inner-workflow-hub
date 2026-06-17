package com.innerworkflow.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson全局配置类
 * <p>
 * 配置全局的JSON序列化/反序列化规则，包括：
 * - 日期时间格式
 * - 空值处理策略
 * - 时区设置
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
public class JacksonConfig {

    /**
     * 默认日期时间格式
     */
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 默认时间格式
     */
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    /**
     * 配置Jackson ObjectMapper自定义规则
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 配置时区为亚洲/上海
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));

            // 全局配置：忽略JSON中存在但对象中不存在的属性
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // 全局配置：空对象不抛异常
            builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

            // 全局配置：Date类型使用时间戳输出关闭（使用自定义格式）
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 全局配置：仅序列化非空字段（可选，根据业务需要调整）
            builder.serializationInclusion(JsonInclude.Include.ALWAYS);

            // 注册Java8时间模块
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // LocalDateTime 序列化/反序列化
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

            // LocalDate 序列化/反序列化
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            // LocalTime 序列化/反序列化
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT);
            javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

            builder.modules(javaTimeModule);
        };
    }

    /**
     * 配置全局ObjectMapper（供非Spring管理的场景使用）
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false).build();
    }
}
