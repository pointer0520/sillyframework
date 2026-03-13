package com.silly.framework.utils;

import com.silly.framework.annotation.Bean;
import com.silly.framework.annotation.Component;
import com.silly.framework.exception.BeanDefinitionException;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ClassUtils {


	/**
	 * 递归查找注解
	 * 1. 直接获取目标类上指定类型的注解
	 * 2. 遍历类上所有注解，递归检查这些注解是否也标注了目标注解
	 * 3. 如果找到多个则抛异常
	 */
	public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
		A a = target.getAnnotation(annoClass);
		for (Annotation anno : target.getAnnotations()) {
			Class<? extends Annotation> annoType = anno.annotationType();
			if (!annoType.getPackageName().endsWith("java.lang.annotation")) {
				A found = findAnnotation(annoType, annoClass);
				if (found != null) {
					if (a != null) {
						throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
					}
					a = found;
				}
			}
		}
		return a;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass) {
		for (Annotation anno : annos) {
			if (anno.annotationType().equals(annoClass)) {
				return (A) anno;
			}
		}
		return null;
	}


	/**
	 * 从方法的 @Bean 注解中获取 value 值作为 Bean 名称。
	 * 如果 value 为空，则使用方法名作为 Bean 名称。
	 */
	public static String getBeanName(Method method) {
		Bean bean = method.getAnnotation(Bean.class);
		String name = bean.value();
		if (name.isEmpty()) {
			name = method.getName();
		}
		return name;
	}

	/**
	 * 获取 Bean 的名称：
	 * 优先查找类上的 @Component 注解，如果存在则使用其 value 值
	 * 如果没有直接找到 @Component，则检查其他注解是否元标注了，并获取其 value 值
	 * 如果都没找到，则使用类名首字母小写作为 Bean 名称
	 */
	public static String getBeanName(Class<?> clazz) {
		String name = "";
		// 查找 @Component:
		Component component = clazz.getAnnotation(Component.class);
		if (component != null) {
			// @Component exist:
			name = component.value();
		} else {
			// 未找到 @Component，继续在其他注解中寻找
			for (Annotation anno : clazz.getAnnotations()) {
				if (findAnnotation(anno.annotationType(), Component.class) != null) {
					try {
						// 获取 注解的 value 方法 并且反射调用获取值
						name = (String) anno.annotationType().getMethod("value").invoke(anno);
					} catch (ReflectiveOperationException e) {
						throw new BeanDefinitionException("Cannot get annotation value.", e);
					}
				}
			}
		}
		if (name.isEmpty()) {
			// default name: "HelloWorld" => "helloWorld"
			name = clazz.getSimpleName();
			name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		}
		return name;
	}

	/**
	 * 查找带有指定注解的无参方法，如 @PostConstruct, @PreDestroy
	 * 1.过滤出类中所有待指定注解的方法
	 * 2.验证方法参数为空
	 * 3.未找到返回空，找到多个抛异常
	 */
	@Nullable
	public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
		// try get declared method:
		List<Method> ms = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annoClass)).map(m -> {
			if (m.getParameterCount() != 0) {
				throw new BeanDefinitionException(
						String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName())
				);
			}
			return m;
		}).toList();
		if (ms.isEmpty()) {
			return null;
		}
		if (ms.size() == 1) {
			return ms.getFirst();
		}
		throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
	}


	/**
	 * 获取指定名称的无参方法
	 */
	public static Method getNamedMethod(Class<?> clazz, String methodName) {
		try {
			return clazz.getDeclaredMethod(methodName);
		} catch (ReflectiveOperationException e) {
			throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
		}
	}

}
