package com.silly.framework.context;

import com.silly.framework.annotation.ComponentScan;
import com.silly.framework.annotation.Configuration;
import com.silly.framework.annotation.Import;
import com.silly.framework.exception.BeanNotOfRequiredTypeException;
import com.silly.framework.exception.NoUniqueBeanDefinitionException;
import com.silly.framework.io.PropertyResolver;
import com.silly.framework.io.ResourceResolver;
import com.silly.framework.utils.ClassUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AnnotationConfigApplicationContext {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final PropertyResolver propertyResolver;
	protected final Map<String, BeanDefinition> beans;

	public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
		this.propertyResolver = propertyResolver;
		// 扫描获取所有 Bean 的 Class 类型

		// 创建 Bean 的定义
		beans = null;
	}

	protected Set<String> scanForClassNames(Class<?> configClass) {
		// 获取要扫描的 package 名称
		ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
		final String[] scanPackages = scan == null || scan.value().length == 0 ? new String[] { configClass.getPackage().getName() } : scan.value();
		logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

		Set<String> classNameSet = new HashSet<>();
		for (String pkg : scanPackages) {
			// 扫描 package
			logger.atDebug().log("scan package: {}", pkg);
			var rr = new ResourceResolver(pkg);
			List<String> classList = rr.scan(res -> {
				String name = res.name();
				if (name.endsWith(".class")) {
					return name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.');
				}
				return null;
			});
			if (logger.isDebugEnabled()) {
				classList.forEach(className -> logger.atDebug().log("class found by component scan: {}", className));
			}
			classNameSet.addAll(classList);
		}

		// 查找 @Import(A.class)
		Import importConfig = configClass.getAnnotation(Import.class);
		if (importConfig != null) {
			for (Class<?> importConfigClass : importConfig.value()) {
				String importClassName = importConfigClass.getName();
				if (classNameSet.contains(importClassName)) {
					logger.warn("ignore import: {} for it is already been scanned.", importClassName);
				} else {
					logger.debug("class found by import: {}", importClassName);
					classNameSet.add(importClassName);
				}
			}
		}
		return classNameSet;
	}

	boolean isConfigurationDefinition(BeanDefinition def) {
		return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
	}

	@Nullable
	public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
		BeanDefinition def = findBeanDefinition(name);
		if (def == null) {
			return null;
		}
		if (!requiredType.isAssignableFrom(def.getBeanClass())) {
			throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.",
					requiredType.getName(), name, def.getBeanClass().getName()));
		}
		return def;
	}

	@Nullable
	public BeanDefinition findBeanDefinition(String name) {
		return this.beans.get(name);
	}

	/**
	 * 根据类型查找 BeanDefinition
	 */
	public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
		return this.beans.values().stream()
				.filter(def -> type.isAssignableFrom(def.getBeanClass()))
				.sorted().toList();
	}

	@Nullable
	public BeanDefinition findBeanDefinition(Class<?> type) {
		List<BeanDefinition> defs = findBeanDefinitions(type);
		if (defs.isEmpty()) {
			return null;
		}
		if (defs.size() == 1) {
			return defs.getFirst();
		}
		List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).toList();
		if (primaryDefs.size() == 1) {
			return primaryDefs.getFirst();
		}
		if (primaryDefs.isEmpty()) {
			throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
		} else {
			throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
		}
	}
}
