package com.silly.framework.io;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sub.AnnoScan;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceResolverTest {

    @Test
    public void scanClass() {
        var pkg = "com.silly.scan";
        var rr = new ResourceResolver(pkg);

        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });

        Collections.sort(classes);
        System.out.println(classes);

        String[] listClasses = new String[] {
                // list of some scan classes:
                "com.silly.scan.convert.ValueConverterBean",
                "com.silly.scan.destroy.AnnotationDestroyBean",
                "com.silly.scan.init.SpecifyInitConfiguration",
                "com.silly.scan.proxy.OriginBean",
                "com.silly.scan.proxy.FirstProxyBeanPostProcessor",
                "com.silly.scan.proxy.SecondProxyBeanPostProcessor",
                "com.silly.scan.nested.OuterBean",
                "com.silly.scan.nested.OuterBean$NestedBean",
                "com.silly.scan.sub1.Sub1Bean",
                "com.silly.scan.sub1.sub2.Sub2Bean",
                "com.silly.scan.sub1.sub2.sub3.Sub3Bean",
        };

        for (String clazz : listClasses) {
            assertTrue(classes.contains(clazz));
        }
    }

    @Test
    public void scanJar() {
        var pkg = PostConstruct.class.getPackageName();
        var rr = new ResourceResolver(pkg);

        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });

        // classes in jar:
        assertTrue(classes.contains(PostConstruct.class.getName()));
        assertTrue(classes.contains(PreDestroy.class.getName()));
        assertTrue(classes.contains(PermitAll.class.getName()));
        assertTrue(classes.contains(DataSourceDefinition.class.getName()));

        // jakarta.annotation.sub.AnnoScan is defined in classes:
        assertTrue(classes.contains(AnnoScan.class.getName()));
        // TODO 如果引入的包中和自定义包中存在同名类，且包路径相同，会发生什么？

    }

    @Test
    public void scanTxt() {
        var pgk = "com.silly.scan";
        var rr = new ResourceResolver(pgk);

        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".txt")) {
                return name.replace("\\", "/");
            }
            return null;
        });
        Collections.sort(classes);
        assertArrayEquals(new String[] {
                // txt files:
                "com/silly/scan/sub1/sub1.txt",
                "com/silly/scan/sub1/sub2/sub2.txt",
                "com/silly/scan/sub1/sub2/sub3/sub3.txt",
        }, classes.toArray(String[]::new));
    }
}
