package com.silly.framework.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyResolverTest {
    @Test
    public void propertyValue() {
        var props = new Properties();
        props.setProperty("app.title", "Silly Framework");
        props.setProperty("app.version", "1.0.0");
        props.setProperty("jdbc.url", "jdbc:mysql://localhost:3306/normal");
        props.setProperty("jdbc.username", "root");
        props.setProperty("jdbc.password", "root");
        props.setProperty("jdbc.pool-size", "20");
        props.setProperty("jdbc.auto-commit", "true");
        props.setProperty("scheduler.started-at", "2023-01-01T19:00:00");
        props.setProperty("scheduler.backup-at", "20:00:00");
        props.setProperty("scheduler.interval", "1h");
        props.setProperty("scheduler.cleanup", "P2DT8H21M");

        var pr = new PropertyResolver(props);
        assertEquals("Silly Framework", pr.getProperty("app.title"));
        assertEquals("1.0.0", pr.getProperty("app.version"));
        assertEquals("1.0.0", pr.getProperty("app.version", "unknown"));
        assertNull(pr.getProperty("app.description"));
        assertEquals("ggBoy", pr.getProperty("app.author", "ggBoy"));

        assertTrue(pr.getProperty("jdbc.auto-commit", boolean.class));
        assertEquals(Boolean.TRUE, pr.getProperty("jdbc.auto-commit", Boolean.class));
        assertEquals(Boolean.TRUE, pr.getProperty("jdbc.auto-commit", boolean.class));
        assertTrue(pr.getProperty("jdbc.detect-leak", boolean.class, true));

        assertEquals(20, pr.getProperty("jdbc.pool-size", int.class));
        assertEquals(20, pr.getProperty("jdbc.pool-size", Integer.class));
        assertEquals(5, pr.getProperty("jdbc.idle", int.class, 5));

        assertEquals(LocalDateTime.parse("2023-01-01T19:00:00"), pr.getProperty("scheduler.started-at", LocalDateTime.class));
        assertEquals(LocalTime.parse("20:00:00"), pr.getProperty("scheduler.backup-at", LocalTime.class));
        assertEquals("1h", pr.getProperty("scheduler.interval", String.class));
        assertEquals(LocalTime.parse("21:00:00"), pr.getProperty("scheduler.restart-at", LocalTime.class, LocalTime.parse("21:00:00")));
        assertEquals(Duration.ofMinutes((2 * 24 + 8) * 60 + 21), pr.getProperty("scheduler.cleanup", Duration.class));
    }

    @Test
    public void requiredProperty() {
        var props = new Properties();
        props.setProperty("app.title", "Silly Framework");
        props.setProperty("app.version", "1.0.0");

        var pr = new PropertyResolver(props);
        // 测试是否抛出了指定的异常
        assertThrows(NullPointerException.class, () -> {
            pr.getRequiredProperty("not.exists");
        });
        assertThrows(IOException.class, () -> {
            pr.getRequiredProperty("not.exists");
        });
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void propertyHolder() {
        String home = System.getenv("JAVA_HOME");
        System.out.println("env HOME = " + home);

        var props = new Properties();
        props.setProperty("app.title", "Silly Framework");

        var pr = new PropertyResolver(props);
        assertEquals("Silly Framework", pr.getProperty("${app.title}"));
        assertThrows(NullPointerException.class, () -> {
            pr.getProperty("${app.description}");
        });

        assertEquals("1.0.0", pr.getProperty("${app.version:1.0.0}"));
        assertEquals(1, pr.getProperty("${app.version:1}", int.class));
        assertThrows(NumberFormatException.class, () -> {
            pr.getProperty("${app.version:x}", int.class);
        });

        assertEquals(home, pr.getProperty("${app.path:${JAVA_HOME}}"));
        assertEquals(home, pr.getProperty("${app.path:${app.home:${JAVA_HOME}}}"));
        assertEquals("/not-exist", pr.getProperty("${app.path:${app.home:${ENV_NOT_EXIST:/not-exist}}}"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void propertyHolderOnWin() {
        String os = System.getenv("OS");
        System.out.println("env OS = " + os);

        var props = new Properties();
        var pr = new PropertyResolver(props);
        assertEquals("Windows_NT", pr.getProperty("${app.os:${OS}}"));
    }
}
