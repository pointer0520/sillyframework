package com.silly.framework.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class YamlUtilsTest {

    @Test
    public void testLoadYaml() {
        Map<String, Object> configs = YamlUtils.loadYamlAsPlainMap("/application.yml");
        System.out.println(configs);
    }
}
