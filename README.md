# common

#### 介绍
通用组件
包含缓存、范围组件（数据范围）

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
# common


# encryptClass
可以加密核心代码，防止反编译。在运行时通过自定义类加载器加载，如果有集成spring，会自动注册到spring容器。
项目分为两大块，一个是maven插件（用于加密class、根据需要是否复制到目标目录）、另外是类加载器。

模块说明：
>encrypt-plugin 插件实现。
>
>encrypted      需要加密的class模块，主要看pom文件。
>
>test           测试模块。需要依赖loader组建。 需要注意，在启动的时候，不要扫描（被加密所在的包,如果是spring项目，程序会自动注入）。
# common-new
