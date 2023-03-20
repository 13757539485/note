jdk包含jre和jvm

jdk提供工具：javac

jre包含java类库和jvm

jvm是一种规范，Classloader用来加载class，然后使用解析器翻译代码

jvm是用c++语言编写

解释执行：用c++解释器去解析class中的代码，所以经过翻译速度会慢一点

JIT执行：class代码翻译成汇编码(codecache)，速度相对比较快

jvm是语言无关性，跨平台


只要符合jvm规范可以自行开发jvm
