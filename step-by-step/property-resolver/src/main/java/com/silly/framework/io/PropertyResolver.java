package com.silly.framework.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

public class PropertyResolver {

    Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        // load environment variables
        this.properties.putAll(System.getenv());
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }
        // register converters
        // 允许注入多种类型
        converters.put(String.class, s -> s);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);

        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);

        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);

        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);

        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);

        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);
    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    @Nullable
    public String getProperty(String key) {
        PropertyExpr keyExpr = parsePropertyExpr(key);
        // 如果 key 是 ${} 形式，则 keyExpr 不为空
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                return getRequiredProperty(keyExpr.key());
            }
        }
        // 否则从配置项中查询
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        String property = getProperty(key);
        return property == null ? parseValue(defaultValue) : property;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }


    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    /**
     * 将字符串值转换为目标类型：
     * 1. 从 converters 映射表查找对应类型的转换函数
     * 2. 未找到则抛出 IllegalArgumentException
     * 3. 调用转换函数进行转换，并强转为目标类型 T 返回
     */
    @SuppressWarnings("unchecked")
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            return value;
        }
        if (expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            return getRequiredProperty(expr.key());
        }
    }

    /**
     * - 检查 key 是否为 ${} 格式
     * - 查找冒号分隔符：
     *     ·无冒号：提取变量名，无默认值（如 ${key}）
     *     ·有冒号：分离变量名和默认值（如 ${key:default}）
     * - 返回 PropertyExpr 对象，包含变量名和可选默认值
     * - 非 ${} 格式返回 null
     */
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(':');
            if (n == (-1)) {
                // no default value: ${key}
                String k = notEmpty(key.substring(2, key.length() - 1));
                return new PropertyExpr(k, null);
            } else {
                // has default value: ${key:default}
                String k = notEmpty(key.substring(2, n));
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }


}

record PropertyExpr(String key, String defaultValue) {
}
