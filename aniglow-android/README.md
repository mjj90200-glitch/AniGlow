# AniGlow Android 学习工程

这是萤火番舍的原生 Android 客户端，使用 Kotlin 与 Jetpack Compose。

## 当前完成的 App 框架

- 首页：按移动端网页复刻品牌顶部栏、主视觉卡、站点统计和高分番剧，连接真实后端数据。
- 番剧：全部番剧双列列表和关键词搜索。
- AIGC：读取真实角色配置，并接入真实模型对话。
- 社区：默认展示全部帖子，图片保持原始比例并限制预览高度。
- 我的：复用网页账号体系，支持登录、注册、退出与管理自己的帖子。
- 全局：与移动端网页一致的品牌顶部栏、六项悬浮底部导航、统一加载/错误状态和视觉样式。

## 与网页端功能同步进度

| 功能 | Android 状态 |
|---|---|
| 首页统计与高分番剧 | 已接入真实数据 |
| 移动网页同款主页面视觉 | 首页、番剧、排行、每日、AIGC、社区均已对齐 |
| 二级页面统一视觉 | 番剧详情、帖子详情、发帖、聊天、登录及个人中心已对齐 |
| 番剧列表、筛选与分页 | 已完成真实题材/地区/格式/年份筛选和前后翻页；顶部全局搜索可直接进入并聚焦搜索框 |
| 番剧详情 | 已完成基础资料、评分列表与评分提交 |
| 社区全部帖子 | 已完成 |
| 帖子详情与回复列表 | 已完成 |
| 点赞与文字回复 | 已接入登录态真实接口 |
| 文字发帖 | 已接入真实接口 |
| 网页账号登录、注册、退出 | 已完成，登录状态保存在 App 私有空间 |
| AIGC 角色列表与真实对话 | 已接入真实模型接口，角色图片按网页资源回退显示；聊天记录按账号和角色保存在本机并支持清空 |
| 图片发帖 | 已接入系统相册多选、预览、移除与真实上传接口 |
| 番剧评分与萤火投票 | 已接入真实接口 |
| 评分回复与删除 | 待开发 |
| 中国用户评分榜与萤火投票榜 | 已接入真实接口 |
| 每日任务 | 已完成本机每日重置、浏览计时、评论任务、萤火票获取与真实投票消耗；跨端同步待接入 |
| 我的帖子 | 已完成查看与删除 |
| 番剧收藏 | 已完成本机持久化收藏与收藏列表，与当前网页端一致 |
| 个人资料编辑 | 已接入真实昵称与头像上传接口 |

## 已打通的数据链

```text
Compose Screen（显示界面）
    ↓ 观察 StateFlow
CommunityViewModel（页面状态）
    ↓ 调用
Repository（业务数据入口）
    ↓ 调用
Retrofit API（HTTP 请求）
    ↓
Spring Boot：/api/stats、/api/anime、/api/agent、/api/communities
```

模拟器通过 `10.0.2.2` 访问电脑上的 `localhost`。开发版接口地址是
`http://10.0.2.2:8081/api/`，正式版接口地址是 `https://www.mjj520.top/api/`。

## 常用命令

```powershell
# 构建开发 APK
.\gradlew.bat :app:assembleDebug

# 安装到已经启动的模拟器
.\gradlew.bat :app:installDebug

# 运行本地单元测试
.\gradlew.bat :app:testDebugUnitTest

# 查看连接设备
adb devices
```

`local.properties` 只记录本机 SDK 路径，已加入 `.gitignore`，不会提交。

## 可以从哪里开始学习

- `MainActivity.kt`：App 的入口，类似网页项目的启动入口。
- `AniGlowApp.kt`：六个主页面的底部导航和页面切换。
- `HomeScreen.kt`、`AnimeScreen.kt`：Compose 页面布局和列表示例。
- `CommunityScreen.kt`：Compose 页面，负责把状态绘制成社区帖子列表。
- `CommunityViewModel.kt`：管理加载中、成功、失败三种页面状态。
- `CommunityRepository.kt`：隔离页面与接口，让后续替换缓存或假数据更容易。
- `CommunityApi.kt`：描述后端 HTTP 接口。
- `MediaUrlResolverTest.kt`：最小单元测试示例，不启动模拟器也能运行。
