# common

#### 介绍
通用组件
包含代码混淆（插件、启动器）、缓存、范围组件（数据范围）

# common

# cache
>
>cache   缓存组件

# range
数据范围组件，可配置批量增加查询条件
>range   数据范围组件  

# encrypt
可以加密核心代码，防止反编译。在运行时通过自定义类加载器加载，如果有集成spring，会自动注册到spring容器。
项目分为两大块，一个是maven插件（用于加密class、根据需要是否复制到目标目录）、另外是类加载器。
模块说明：
>encrypt
>   encrypt-plugin 插件实现，在打包（和以后）阶段会自动进行加密。
>
>   encrypt-key    配置加密信息，本地key 或 远程获取方式
> 
>   encrypted      需要加密的class模块，主要看pom文件。
>
>   test           测试模块。需要依赖loader组件。 需要注意，在启动的时候，不要扫描（被加密所在的包,如果是spring项目，程序会自动注入）。
>                  CmdLauncher 为命令行启动方式，启动jar包，包含启动混淆类加载器、解析jar包内的混淆后的class，来达到正常运行带有混淆后的代码。
>                  config 包下有cache、range组件配置的方法。
> 
# common-new
