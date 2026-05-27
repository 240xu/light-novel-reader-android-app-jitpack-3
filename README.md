# LightNovelReader Legado Plugin (LNR Plugin)

> 基于 Legado 核心引擎的 LightNovelReader 插件，通过 JitPack 依赖管理实现纯净的解析引擎库。

## 项目架构

```
light-novel-reader-android-app-jitpack-3/
├── legado-engine-core/          # 纯 Kotlin 解析引擎库 (JitPack 发布)
│   └── src/main/kotlin/io/legado/engine/
│       ├── analyzer/            # 规则解析器 (CSS/XPath/JSON/Regex/JS)
│       ├── model/               # 数据模型 (Book/Chapter/Source)
│       ├── helper/              # 内容处理工具
│       ├── coroutine/           # 协程工具
│       ├── interface/           # 抽象接口 (HTTP/JS引擎)
│       ├── exception/           # 异常定义
│       └── EngineFacade.kt      # 引擎门面 (主入口)
│
├── lnr-plugin/                  # LNR 插件项目
│   └── src/main/
│       ├── kotlin/.../lnrplugin/
│       │   ├── LNRLegadoPlugin.kt    # 插件入口
│       │   ├── adapter/              # 适配器层
│       │   │   ├── ExploreAdapter.kt # 发现页
│       │   │   ├── SearchAdapter.kt  # 搜索
│       │   │   ├── BookInfoAdapter.kt# 书籍详情
│       │   │   ├── ChapterListAdapter.kt # 章节目录
│       │   │   ├── ContentAdapter.kt # 正文解析
│       │   │   └── BackupAdapter.kt  # 备份恢复
│       │   ├── source/               # 书源管理
│       │   └── util/                 # 工具类 (HTTP/JS引擎)
│       └── assets/
│           ├── js/custom.js          # 段评注入脚本
│           └── source/               # 示例书源
└── settings.gradle.kts
```

## 核心引擎裁剪清单

### ✅ 保留并适配的模块 (零依赖或轻依赖)

| 模块 | 类名 | 依赖 | 说明 |
|------|------|------|------|
| RuleAnalyzer | `RuleAnalyzer` | 无 | 规则解析核心，统一分发器 |
| AnalyzeByRegex | `AnalyzeByRegex` | 无 | 正则解析，零外部依赖 |
| ContentHelp | `ContentHelp` | 无 | 内容辅助处理，纯正则 |
| AnalyzeByJSoup | `AnalyzeByJSoup` | jsoup | HTML/CSS 选择器解析 |
| AnalyzeByXPath | `AnalyzeByXPath` | jsoup, jsoup-xpath | XPath 解析 |
| AnalyzeByJSonPath | `AnalyzeByJSonPath` | json-path | JSON 路径解析 |
| CoroutineHelper | `CoroutineHelper` | kotlinx-coroutines | 纯 Kotlin 协程 |
| EngineFacade | `EngineFacade` | 全部 | 引擎门面，统一调用入口 |

### ❌ 完全移除的部分

- Room 数据库相关所有代码
- 所有 `android.content.Context` 依赖
- `splitties` 库依赖
- AndroidX 全部库依赖
- 所有 UI、Activity、Service、阅读器组件
- `LiveData`、`ViewModel`、`Paging` 等架构组件
- 所有 `@Database`、`@Dao`、`@Entity` 注解类

### 抽象接口

| 接口 | 用途 | LNR 宿主实现 |
|------|------|--------------|
| `IHttpHandler` | HTTP 请求 | `OkHttpHandler` |
| `IJsEngine` | JS 执行 | `JsEngineImpl` (Rhino) |

## 快速开始

### 第一步：发布引擎到 JitPack

```bash
# 1. Fork legado 仓库，创建 engine-core 分支
git checkout -b engine-core

# 2. 将引擎代码放入项目中，修改 build.gradle
#    groupId = "io.github.YOUR_USERNAME"
#    artifactId = "legado-engine-core"
#    version = "1.0.0-core"

# 3. 提交并推送
git add .
git commit -m "feat: pure Kotlin engine core for LNR plugin"
git push origin engine-core

# 4. 在 GitHub 创建 Release: v1.0.0-core

# 5. 访问 https://jitpack.io
#    输入仓库地址，查找 v1.0.0-core 版本
#    获得依赖坐标:
#    implementation("io.github.YOUR_USERNAME:legado-engine-core:1.0.0-core")
```

### 第二步：配置 LNR 插件项目

在 `settings.gradle.kts` 中已配置：
- 阿里云 Maven 镜像
- 华为云 Maven 镜像 (备用)
- JitPack 仓库
- 腾讯云 Gradle 下载镜像

在 `lnr-plugin/build.gradle.kts` 中添加依赖：
```kotlin
dependencies {
    implementation("io.github.YOUR_USERNAME:legado-engine-core:1.0.0-core")
}
```

### 第三步：构建与测试

```bash
# 构建 Debug APK
./gradlew :lnr-plugin:assembleDebug

# 构建 Release AAR
./gradlew :lnr-plugin:assembleRelease

# 打包成 .lnrp 文件 (LNR 插件格式)
# 将 output/aar 文件重命名为 .lnrp 扩展名
cp lnr-plugin/build/outputs/aar/lnr-plugin-release.aar \
   lnr-plugin/build/outputs/legado-plugin.lnrp

# 安装到 LNR 测试
# 将 .lnrp 文件传输到设备，在 LNR 中安装
```

## 书源规则格式

本插件兼容 Legado 标准书源 JSON 格式：

```json
{
  "bookSourceUrl": "https://example.com",
  "bookSourceName": "示例书源",
  "searchUrl": "https://example.com/search?keyword=searchKey",
  "ruleSearch": {
    "bookList": ".book-item",
    "name": ".book-title@text",
    "author": ".book-author@text",
    "bookUrl": ".book-title@href",
    "coverUrl": ".book-cover@src"
  }
}
```

### 规则前缀

| 前缀 | 引擎 | 说明 |
|------|------|------|
| `@css:` / `@jsoup:` | AnalyzeByJSoup | CSS 选择器 |
| `@XPath:` / `//` | AnalyzeByXPath | XPath 表达式 |
| `@json:` / `$.` | AnalyzeByJSonPath | JSON 路径 |
| `/regex/` | AnalyzeByRegex | 正则表达式 |
| `@js:` | JsEngine | JavaScript 代码 |

## 段评功能

通过 `custom.js` 注入 WebView 实现：

- SVG 评论图标自动生成于每段末尾
- 点击弹出评论浮层
- 支持坐标定位和响应式布局
- 与 LNR 评论 API 集成

## 备份恢复

支持以下数据的导入导出：
- 书源配置 (兼容 Legado 格式)
- 书架数据
- 插件设置

## 许可证

本项目使用 **GPL-3.0** 许可证。

Copyright (C) 2026 LNR Community

任何获取了插件二进制文件的用户，均有权要求获取完整的源代码。

## 致谢

- [Legado](https://gitee.com/lyc486/legado) - 核心解析引擎来源
- [LightNovelReader](https://github.com/dmzz-yyhyy/LightNovelReader) - 宿主应用
- [LNR Plugin Template](https://github.com/dmzz-yyhyy/LightNovelReaderPlguin-Template) - 插件模板
