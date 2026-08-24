# mirror-plugin

一个通过全局 Gradle 属性选择 Maven 镜像的 Settings 插件。插件会在
`settings.gradle(.kts)` 执行完成后检查依赖仓库；如果选中的镜像尚不存在，
就将它插入 `dependencyResolutionManagement.repositories` 的第一个位置。

## 通过 submodule 使用

将仓库加入项目，例如：

```shell
git submodule add https://github.com/storytellerF/mirror-plugin.git gradle/mirror-plugin
```

在项目的 `settings.gradle.kts` 顶部配置 included build 并应用插件：

```kotlin
pluginManagement {
    includeBuild("gradle/mirror-plugin")
}

plugins {
    id("com.storytellerf.mirror")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Groovy DSL：

```groovy
pluginManagement {
    includeBuild('gradle/mirror-plugin')
}

plugins {
    id 'com.storytellerf.mirror'
}
```

## 全局配置

在 `~/.gradle/gradle.properties` 中选择预置镜像：

```properties
mavenMirror=aliyun
```

支持的值：

- `aliyun`
- `tencent`
- `huawei`
- `none`（禁用）
- 任意以 `https://` 或 `http://` 开头的 Maven 仓库 URL

也可以用独立属性指定 URL；它的优先级高于 `mavenMirror`：

```properties
mavenMirrorUrl=https://example.com/repository/maven-public/
```

如果两个属性都未设置，插件不会修改仓库列表。插件只处理项目依赖仓库，
不会改变加载插件自身所使用的 `pluginManagement.repositories`。

## 本地验证

```shell
./gradlew test
```
