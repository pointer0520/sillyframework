package com.silly.framework.utils;

import com.silly.framework.io.InputStreamCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class ClassPathUtils {
    /**
     * 泛型方法：用于安全地读取 classpath 中的资源文件并通过回调函数处理
     * 1.路径处理：去掉开头的 "/"
     * 2.读取资源：通过类加载器从 classpath 获取输入流
     * 3.异常处理：文件不存在时抛出 FileNotFoundException，IO 错误时包装为 UncheckedIOException
     * 4.回调执行：将输入流传递给 InputStreamCallback 接口处理并返回结果
     */
    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try (InputStream input = getContextClassLoader().getResourceAsStream(path)){
            if (input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String readString(String path) {
        return readInputStream(path, (input) -> {
            byte[] data = input.readAllBytes();
            return new String(data, StandardCharsets.UTF_8);
        });
    }

    static ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassPathUtils.class.getClassLoader();
        }
        return cl;
    }
}
