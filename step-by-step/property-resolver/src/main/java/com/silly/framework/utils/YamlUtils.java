package com.silly.framework.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.Map;

/**
 * Parse yaml by snakeyaml.
 * SnakeYaml 默认读出的结构是树形结构，需要“拍平”成 'abc.xyz' 格式的 key；
 * SnakeYaml 默认会自动转换 int、boolean 等 value，需要禁用自动转换，把所有 value 均按 String 类型返回。
 */
public class YamlUtils {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        var loaderOptions = new LoaderOptions();
        var dumperOptions = new DumperOptions();
        var representer = new Representer(dumperOptions);
        var resolver = new NoImplicitResolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return null;
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
