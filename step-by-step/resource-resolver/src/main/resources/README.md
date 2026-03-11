# 前言
在编写 IoC 容器之气那，首先要实现 @ComponentScan，解决“在指定包下扫描所有 Class”的问题。

## 说明
Java 的 ClassLoader 机制可以在指定的 Classpath 中根据类名加载指定的 Class，但遗憾的是，给出一个包名，例如，org.example,它并不能获取到该包下的所有 Class，
也不能获取子包。要在 Classpath 中扫描指定包名下的所有 Class，包括子包，实际上是在 Classpath 中搜索所有文件，找出文件名匹配的 .class 文件。
例如，Classpath 中搜索的文件 org/example/Hello.class 就符合包名 org.example, 我们需要根据文件路径把它变成 org.example.Hello, 就相当于获得了类名。
因此，搜索 Class 变成了搜索文件。

## 解释
1. ClassLoader 的核心职责是：按需加载某个已知类名的 .class 文件
2. 它只负责根据完整类名，找到 .class 文件，并加载 JVM，并不会维护 包 -> 类列表的索引
3. 所以，我们需要定义一个 Record 类型表示文件
4. 然后模仿 Spring 提供一个 ResourceResolver 来扫描获取到的 Resource

