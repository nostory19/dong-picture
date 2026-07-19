[简体中文](README.md)

<div align="center">

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot 3.2](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring_Cloud_Alibaba-2023.0.1-FF6A00?style=flat-square&logo=alibabacloud&logoColor=white)
![Nacos](https://img.shields.io/badge/Nacos-2.x-1B88E4?style=flat-square&logo=nacos&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3.5-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Ant Design Vue](https://img.shields.io/badge/Ant_Design_Vue-4.2-0170FE?style=flat-square&logo=antdesign&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Tencent COS](https://img.shields.io/badge/Tencent_COS-5.x-0052D9?style=flat-square&logo=tencentqq&logoColor=white)
![ShardingSphere](https://img.shields.io/badge/ShardingSphere-5.2-1C3C3C?style=flat-square&logo=apache&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-CRDT-000000?style=flat-square&logo=socketdotio&logoColor=white)
![Yjs](https://img.shields.io/badge/Yjs-CRDT-FFD500?style=flat-square&logo=yjs&logoColor=black)
![Pinia](https://img.shields.io/badge/Pinia-2.2-FFD500?style=flat-square&logo=vuedotjs&logoColor=black)
![ECharts](https://img.shields.io/badge/ECharts-5.4-AA344D?style=flat-square&logo=apache&logoColor=white)
![License MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

</div>

<br />

<div align="center">
  <h1>dong-picture 智能协同云图库</h1>
  <p><b>从单体到微服务 —— 支持多人实时协同编辑的智能云图库平台</b></p>
</div>

<p align="center">
  图片存储管理 · 空间权限控制 · 多维度空间分析 · 实时协同编辑 · CRDT 冲突解决 · WebRTC P2P 加速
</p>

<p align="center">
  <a href="#-项目概述">项目概述</a> ·
  <a href="#-系统架构">系统架构</a> ·
  <a href="#-核心功能">核心功能</a> ·
  <a href="#-技术栈">技术栈</a> ·
  <a href="#-项目结构">项目结构</a> ·
  <a href="#-快速开始">快速开始</a>
</p>

---

## 项目概述

本项目包含**两套架构实现**，共享相同的业务功能体系：

| 版本 | 目录 | 架构 | 说明 |
|------|------|------|------|
| **单体版** | `dong-picture-backend` + `dong-picture-frontend` | Spring Boot 2.7 + Vue 3 | 单体后端，适合快速部署和小规模使用 |
| **微服务版** | `dong-picture-micro-backend` + `dong-picture-micro-frontend` | Spring Cloud + Nacos + Vue 3 | 微服务拆分，支持独立部署、弹性伸缩、实时协同 |

两套版本的核心差异在于**后端架构**：单体版将所有功能集成于一个 Spring Boot 应用；微服务版将系统拆分为 **Gateway + 4 个独立业务服务**，通过 Nacos 服务发现和 OpenFeign 远程调用实现解耦，并额外引入了 **CRDT 实时协同编辑引擎**。

---

## 系统架构

### 微服务架构全景

```
┌─────────────────────────────────────────────────────────────┐
│                   浏览器 (Vue 3 + Vite + Ant Design Vue)      │
│                                                              │
│  ┌──────────┬──────────┬──────────┬──────────┬────────────┐ │
│  │ 图片浏览 │ 空间管理 │ 数据分析 │ 图片编辑 │ 协同编辑    │ │
│  │ 上传下载 │ 权限控制 │ ECharts  │ 裁剪旋转 │ Yjs CRDT   │ │
│  └──────────┴──────────┴──────────┴──────────┴────────────┘ │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┴────────────────┐
          │  REST API       WebSocket/WebRTC│
          └───────────────┬────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────┐
│           Spring Cloud Gateway (端口 8200)                    │
│                                                              │
│  路由分发 · JWT 全局鉴权 · CORS · 内部接口保护 · 白名单       │
└─────────────────────────┬───────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────────┐
        │                 │                     │
┌───────┴───────┐ ┌──────┴──────┐ ┌───────────┴───────────┐
│  User Service │ │Space Service│ │  Picture Service       │
│    :8201      │ │   :8202     │ │     :8203              │
│               │ │             │ │                        │
│  用户注册登录 │ │ 空间 CRUD   │ │  图片 CRUD · 上传下载  │
│  用户信息管理 │ │ 成员管理    │ │  图片搜索 · 标签分类   │
│  COS 文件操作 │ │ 角色权限    │ │  以图搜图 · 颜色搜索   │
│               │ │ 空间分析    │ │  点赞系统 · 多级缓存   │
└───────────────┘ └─────────────┘ │  分库分表 · AI 扩图     │
                                  │  WebSocket 实时编辑     │
                                  └────────────────────────┘
                                                  │
                                                  │ CRDT Engine
                                  ┌───────────────┴───────────┐
                                  │  Collaboration Service     │
                                  │        :8204              │
                                  │                           │
                                  │  CRDT 文档同步引擎         │
                                  │  Lamport 逻辑时钟          │
                                  │  状态向量增量同步          │
                                  │  WebRTC Signaling 中继     │
                                  │  在线状态追踪 (Presence)   │
                                  │  Redis 操作日志持久化      │
                                  └───────────────────────────┘
```

### 基础设施层

```
┌──────────────────────────────────────────────┐
│  Nacos (:8848)                                │
│  服务注册与发现 · 配置中心 (共享 common.yml)   │
├──────────────────────────────────────────────┤
│  MySQL (:3306)                                │
│  用户 · 图片 · 空间 · 空间成员 · 点赞          │
├──────────────────────────────────────────────┤
│  Redis (:6379)                                │
│  分布式缓存 · Session 共享 · 操作日志持久化    │
├──────────────────────────────────────────────┤
│  腾讯云 COS                                    │
│  图片对象存储 · CDN 加速                       │
└──────────────────────────────────────────────┘
```

### 数据流

**REST 请求**：浏览器 → API Gateway (JWT 鉴权) → Nacos 服务发现 → 目标微服务 → MySQL/Redis/COS → 返回

**实时协同编辑**：浏览器 → WebSocket 直连 Picture/Collaboration Service → CRDT Engine → Disruptor 事件处理 → 广播所有在线用户

**P2P 加速**：浏览器 ↔ WebRTC DataChannel (Cursor/Presence) / Signaling Service 中继

---

## 核心功能

### 图片管理

| 功能 | 说明 |
|------|------|
| **图片上传** | 支持文件上传和 URL 拉取，自动缩略图生成，格式/大小校验 |
| **图片浏览** | 首页瀑布流展示，支持关键词搜索、分类筛选、标签过滤、时间/热度排序 |
| **图片编辑** | 在线裁剪、旋转、缩放，支持批量编辑（批量命名规则、分类/标签设置） |
| **AI 智能扩图** | 对接阿里云 AI 接口，支持图片外扩生成（Out-Painting） |
| **以图搜图** | 基于颜色直方图的相似图片检索 |
| **图片审核** | 管理员审核流程，支持通过/拒绝，审核状态流转 |

### 空间与权限

| 功能 | 说明 |
|------|------|
| **空间类型** | 私有空间 + 团队空间，团队空间支持多人协作 |
| **空间等级** | 普通版 / 专业版 / 旗舰版，不同等级对应不同容量上限 |
| **成员管理** | 邀请/移除成员，角色分配（查看者 / 编辑者 / 管理员） |
| **细粒度权限** | 图片查看、上传、编辑、删除、成员管理等独立权限控制 |
| **RBAC 权限引擎** | 空间用户角色 → 权限映射，支持自定义权限组合 |

### 空间分析

| 功能 | 说明 |
|------|------|
| **使用量分析** | 存储空间已用/总量统计 |
| **分类分析** | 图片按分类维度的分布（饼图/柱状图） |
| **标签分析** | 标签词云展示，直观呈现标签使用频率 |
| **大小分布** | 图片文件大小区间统计 |
| **用户行为分析** | 各成员上传行为统计 |
| **排行榜** | 空间使用量排名（管理员视角） |

### 实时协同编辑 ⭐

| 功能 | 说明 |
|------|------|
| **多人同时编辑** | 基于 CRDT 算法，支持多人无冲突地同时编辑同一张图片（旋转、缩放、裁剪、滤镜等） |
| **三阶段同步** | 连接时通过 State Vector 增量同步，确保所有客户端状态最终一致 |
| **实时光标** | 多用户彩色光标叠加显示，实时展示每个人的编辑位置和操作 |
| **在线感知** | Presence 面板显示当前在线用户列表、编辑状态、连接状态 |
| **P2P 加速** | WebRTC DataChannel 直连传输光标和在线状态，减少服务器中转延迟 |
| **离线编辑** | IndexedDB 离线操作队列，断网后编辑操作暂存本地，恢复连接后自动同步 |
| **撤销/重做** | Yjs UndoManager 提供完整的撤销/重做支持 |
| **降级兼容** | 同时支持旧版独占编辑锁协议，向下兼容 |

### 社交与用户

| 功能 | 说明 |
|------|------|
| **用户系统** | 注册 / 登录 / 个人中心，JWT 无状态认证 |
| **点赞系统** | 图片点赞/取消点赞，Redis + Lua 原子操作，定时同步至 MySQL |
| **图片分享** | 链接复制 + 二维码分享 |

---

## 技术栈

### 后端

| 层级 | 单体版 | 微服务版 |
|------|--------|----------|
| **语言/JDK** | Java 8 | Java 17 |
| **框架** | Spring Boot 2.7.6 | Spring Boot 3.2.4 |
| **微服务** | — | Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0 |
| **服务发现** | — | Nacos (Discovery + Config) |
| **网关** | — | Spring Cloud Gateway + JWT 全局过滤 |
| **远程调用** | — | OpenFeign |
| **认证** | Sa-Token + Redis Session | JWT (jjwt) |
| **ORM** | MyBatis-Plus 3.5.10 | MyBatis-Plus 3.5.12 (Spring Boot 3) |
| **数据库** | MySQL 8.x | MySQL 8.x |
| **缓存** | Caffeine L1 + Redis L2 | Caffeine L1 + Redis L2 + HotKey 检测 |
| **分库分表** | ShardingSphere-JDBC 5.2 | ShardingSphere-JDBC 5.2 |
| **WebSocket** | Spring WebSocket + Disruptor | Spring WebSocket + Disruptor |
| **对象存储** | 腾讯云 COS | 腾讯云 COS |
| **API 文档** | Knife4j 4.4 | Knife4j 4.4 |

### 前端

| 层级 | 技术 |
|------|------|
| **框架** | Vue 3.5 (Composition API) |
| **构建** | Vite 6 |
| **语言** | TypeScript 5.6 |
| **UI 库** | Ant Design Vue 4.2 |
| **状态管理** | Pinia 2.2 |
| **路由** | Vue Router 4.4 |
| **HTTP** | Axios + JWT 拦截器 + 401 自动重定向 |
| **图表** | ECharts 5.4 + echarts-wordcloud |
| **图片裁剪** | vue-cropper |
| **实时协同** | Yjs (CRDT) + y-websocket |
| **P2P 通信** | WebRTC (RTCPeerConnection + DataChannel) |
| **离线存储** | IndexedDB |

---

## 项目结构

```
programNavigation/
├── dong-picture-backend/                  # 单体后端 (Spring Boot 2.7)
│   └── src/main/java/com/dong/dongpicturebackend/
│       ├── controller/                    # REST 控制器
│       ├── service/                       # 业务逻辑层
│       ├── mapper/                        # MyBatis Mapper
│       ├── model/                         # 实体 / VO / DTO / 枚举
│       ├── manager/                       # 外部集成 (COS、Sharding、WebSocket)
│       ├── annotation/                    # @AuthCheck 权限注解
│       └── config/                        # Spring 配置
│
├── dong-picture-frontend/                 # 单体前端 (Vue 3)
│   └── src/
│       ├── pages/                         # 页面组件 (15 个路由)
│       ├── components/                    # 通用组件 (搜索、上传、裁剪、分析图表)
│       ├── api/                           # OpenAPI 自动生成的 API 模块
│       ├── router/                        # Vue Router 配置
│       └── stores/                        # Pinia 状态 (登录用户)
│
├── dong-picture-micro-backend/            # 微服务后端 (Spring Cloud)
│   ├── dong-picture-backend-common/       # 公共模块：响应体、异常、工具类、注解
│   ├── dong-picture-backend-model/        # 数据模型：实体、VO、DTO、枚举
│   ├── dong-picture-backend-service-client/ # Feign 远程调用接口
│   ├── dong-picture-backend-gateway/      # API 网关 (:8200)
│   ├── dong-picture-backend-user-service/ # 用户服务 (:8201)
│   ├── dong-picture-backend-space-service/ # 空间服务 (:8202)
│   ├── dong-picture-backend-picture-service/ # 图片服务 (:8203)
│   └── dong-picture-backend-collaboration-service/ # 协同编辑服务 (:8204)
│
├── dong-picture-micro-frontend/           # 微服务前端 (Vue 3 + Yjs)
│   └── src/
│       ├── pages/                         # 页面组件 (15 个路由)
│       ├── components/                    # 组件 (+ CollaborativeCanvas, MultiUserCursors 等)
│       ├── utils/
│       │   ├── CollabWebSocket.ts         # CRDT WebSocket 三阶段同步客户端
│       │   ├── CanvasDocument.ts          # Yjs 协同文档封装
│       │   ├── WebRTCClient.ts            # WebRTC P2P DataChannel
│       │   └── OfflineStore.ts            # IndexedDB 离线操作队列
│       └── constants/                     # 枚举常量定义
│
└── dong_picture.sql                       # 数据库初始化脚本
```

---

## 快速开始

### 环境要求

- JDK 17（微服务版）/ JDK 8（单体版）
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Nacos Server 2.x（仅微服务版需要）

### 1. 克隆项目

```bash
git clone git@github.com:nostory19/dong-picture.git
cd dong-picture
```

### 2. 数据库初始化

```bash
mysql -u root -p < dong_picture.sql
```

### 3. 启动微服务后端

**前置**：启动 Nacos Server 和 Redis。

```bash
cd dong-picture-micro-backend

# 1. 编译整个项目（跳过测试）
mvn clean install -DskipTests

# 2. 按顺序启动各服务
cd dong-picture-backend-gateway && mvn spring-boot:run       # :8200
cd ../dong-picture-backend-user-service && mvn spring-boot:run    # :8201
cd ../dong-picture-backend-space-service && mvn spring-boot:run   # :8202
cd ../dong-picture-backend-picture-service && mvn spring-boot:run # :8203
cd ../dong-picture-backend-collaboration-service && mvn spring-boot:run  # :8204
```

### 4. 启动前端

```bash
cd dong-picture-micro-frontend
npm install
npm run dev
```

访问 http://localhost:5173

> 首次使用需要注册账号，管理员功能需要手动在数据库中设置 `userRole` 为 `admin`。

---

## License

[MIT](LICENSE)
