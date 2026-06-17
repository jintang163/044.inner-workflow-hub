package com.innerworkflow.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * <p>
 * 基于Jackson封装，提供常用的JSON序列化/反序列化方法
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Slf4j
public class JsonUtils {

    /**
     * 默认日期时间格式
     */
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    /**
     * 全局共享的ObjectMapper实例
     */
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * 创建并配置ObjectMapper实例
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 日期格式配置
        mapper.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
        // 反序列化时忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 序列化时空对象不报错
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Java8时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 序列化器
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        // 反序列化器
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        mapper.registerModule(javaTimeModule);

        return mapper;
    }

    /**
     * 获取全局ObjectMapper实例（用于高级配置场景）
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败: {}", e.getMessage(), e);
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * 对象转格式化的JSON字符串（带缩进，便于调试）
     */
    public static String toPrettyJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转格式化JSON字符串失败: {}", e.getMessage(), e);
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * JSON字符串转对象（指定Class）
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败, json={}, class={}, error={}", json, clazz.getSimpleName(), e.getMessage());
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON字符串转对象（指定TypeReference，用于泛型场景）
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败, json={}, error={}", json, e.getMessage());
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON字符串转List
     */
    public static <T> List<T> parseList(String json, Class<T> elementClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementClass);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败, json={}, error={}", json, e.getMessage());
            throw new RuntimeException("JSON转List失败", e);
        }
    }

    /**
     * JSON字符串转Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败, json={}, error={}", json, e.getMessage());
            throw new RuntimeException("JSON转Map失败", e);
        }
    }

    /**
     * JSON字符串转Map（指定Value类型）
     */
    public static <V> Map<String, V> parseMap(String json, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory()
                .constructMapType(Map.class, String.class, valueClass);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败, json={}, error={}", json, e.getMessage());
            throw new RuntimeException("JSON转Map失败", e);
        }
    }

    /**
     * 从InputStream解析JSON为对象
     */
    public static <T> T parseObject(InputStream inputStream, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("从InputStream解析JSON失败, class={}, error={}", clazz.getSimpleName(), e.getMessage());
            throw new RuntimeException("解析JSON失败", e);
        }
    }

    /**
     * 对象转Map
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(obj, new TypeReference<>() {});
    }

    /**
     * Map转对象
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * 判断字符串是否为合法JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
