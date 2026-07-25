# AutoCraft

纯客户端 Fabric Mod，自动化原版工作台合成操作，专为生电红石"合成站"设计。

## 支持的版本

- **Minecraft 1.21**（主要开发目标）
- **Minecraft 1.20.6**（兼容目标）
- 1.20.5、1.21.1（低代价额外支持）

## 功能概览

- 在工作台 GUI 中一键保存当前配方
- 通过 `/autocraft` 命令管理系统设置合成站（工作台、输入/输出容器）
- 自动规划材料：分析背包 + 输入容器，精确计算所需数量，不多拿不少拿
- 自动导航（Baritone 优先，回退内置 A* 寻路）
- 发包操作物品栏：自动打开发包工作台、摆放配方、取产物、存产物
- 半透明 HUD 覆盖界面（按空格暂停/恢复）
- 智能重试：材料缺失 / 输出满时自动重试，每容器至少检查 2 次，30 秒超时
- 支持目标数量（可设为无限）

## 前置依赖

- **Fabric Loader** >= 0.16.0
- **Fabric API**
- **Minecraft** >= 1.20.5
- **Java** >= 21
- **Baritone**（可选，用于高级寻路）

## 快速入门

### 1. 保存合成表

打开工作台，像正常合成一样摆放物品，点击 GUI 中新增的 **💾 +** 按钮即可自动保存配方。

### 2. 创建合成站配置

```
/autocraft station create 主合成站
```

看向工作台方块：
```
/autocraft station set-workbench 主合成站
```

看向输入容器（可多次添加）：
```
/autocraft station add-input 主合成站
```

看向输出容器：
```
/autocraft station set-output 主合成站
```

绑定合成表：
```
/autocraft station bind-recipe 主合成站 recipe_xxxxx
```

开/关容器边框高亮：
```
/autocraft station highlight 主合成站
```

### 3. 启动自动合成

合成 64 个：
```
/autocraft task start 主合成站 64
```

无限合成（直到材料耗尽或输出满）：
```
/autocraft task start 主合成站 -1
```

### 4. 控制任务

```
/autocraft task pause     — 暂停（或按空格）
/autocraft task resume    — 恢复
/autocraft task stop      — 停止
/autocraft task status    — 查看状态
```

## 构建

```bash
# 构建 1.21 版本（默认）
./gradlew build

# 构建 1.20.6 版本
./gradlew build -Pminecraft_version=1.20.6
```

构建产物在 `build/libs/` 目录。

## 配置文件

所有配置存放在 `.minecraft/config/autocraft/`：

- **config.json** — Mod 设置（重试间隔、超时等）
- **recipes.json** — 保存的合成表
- **stations.json** — 合成站配置

## 架构

```
com.autocraft/
├── AutoCraftMod.java          (入口)
├── config/                    (配置系统)
├── recipe/                    (合成表: 数据模型 + 管理器 + Mixin)
├── station/                   (合成站: 数据模型 + 管理器 + 高亮)
├── task/                      (任务引擎: 状态机 + 材料规划 + 重试)
│   └── states/                (逐个状态实现)
├── interaction/               (发包操作: 容器交互 + 物品栏扫描)
├── navigation/                (导航: Baritone / A* 回退)
├── overlay/                   (HUD 覆盖界面 + 输入拦截)
├── command/                   (/autocraft 命令系统)
└── util/                      (工具类)
```

## 许可证

MIT License
