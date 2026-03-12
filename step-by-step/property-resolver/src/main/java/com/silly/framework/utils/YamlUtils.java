package com.silly.framework.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse yaml by snakeyaml.
 * SnakeYaml 默认读出的结构是树形结构，需要“拍平”成 'abc.xyz' 格式的 key；
 * SnakeYaml 默认会自动转换 int、boolean 等 value，需要禁用自动转换，把所有 value 均按 String 类型返回。
 */
public class YamlUtils {
    /**
     * 1.配置 YAML 解析器：创建 SnakeYAML 的加载器、表示器和解析器实例，使用 NoImplicitResolver 禁用隐式类型转换
     * 2.读取并解析文件：通过 ClassPathUtils.readInputStream 读取文件并解析为 Map 树状结构
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYaml(String path) {
        var loaderOptions = new LoaderOptions();
        var dumperOptions = new DumperOptions();
        var representer = new Representer(dumperOptions);
        var resolver = new NoImplicitResolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path, (input) -> {
            return (Map<String, Object>) yaml.load(input);
        });
    }

    /**
     * 1.加载 YAML 文件：调用 loadYaml 方法获取 Map 树状结构
     * 2.展平处理：调用 convertTo 方法将 Map 树状结构转换成 'abc.xyz' 键值对
     */
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        Map<String, Object> data = loadYaml(path);
        Map<String, Object> plain = new LinkedHashMap<>();
        convertTo(data, "", plain);
        return plain;
    }

    /**
     * 1.递归展平嵌套 Map：遍历源 Map，将多层嵌套结构转换为一维键值对
     * 2.层级路径拼接：遇到子 Map 时递归处理，用点号连接键名形成路径
     * 3.类型处理：List 直接存储，其它类型转为 String
     */
    static void convertTo(Map<String, Object> source, String prefix, Map<String, Object> plain) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                // 层级处理
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + ".", plain);
            } else if (value instanceof List){
                plain.put(prefix + key, value);
            } else {
                plain.put(prefix + key, value.toString());
            }
        }
    }
}

/**
 * Disable ALL implicit convert and treat all values as String.
 */
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}
