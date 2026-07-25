# AutoCraft

纯客户端 Fabric Mod，自动化原版工作台合成操作，专为生电红石"合成站"设计。

## 支持的版本

- **Minecraft 1.21**（当前构建目标）
- **Minecraft 1.20.6**（兼容，需切换映射）
- 1.20.5、1.21.1（低代价额外支持）

## 前置依赖

- Fabric Loader >= 0.16.10
- Fabric API
- Minecraft >= 1.20.6
- Java >= 21
- Baritone（可选，用于高级寻路）

## 快速入门

### 1. 保存合成表

打开工作台，摆放配方物品，点击 **💾 +** 按钮 → 弹出命名框（默认填充产物国际化名称）→ Enter 保存。

或使用命令：
```
/autocraft recipe save <名称>
/autocraft recipe list
/autocraft recipe show <名称>
/autocraft recipe delete <名称>
/autocraft recipe overwrite <名称>
```

### 2. 创建合成站

```
/autocraft station create <名称>          # 创建
/autocraft station set-workbench <名称>   # 看向工作台
/autocraft station add-input <名称>       # 看向输入容器（可多次）
/autocraft station set-output <名称>      # 看向输出容器
/autocraft station bind-recipe <名称> <配方>
/autocraft station highlight <名称>       # 边框高亮开关
/autocraft station list                   # 列表
/autocraft station show <名称>            # 详情
```

### 3. 启动合成

```
/autocraft task start <站名>              # 无限合成（默认）
/autocraft task start <站名> 64           # 合成 64 个产物
/autocraft task start <站名> -1           # 显式无限
/autocraft task pause                     # 暂停（或按空格）
/autocraft task resume                    # 恢复
/autocraft task stop                      # 停止
/autocraft task status                    # 状态
```

## 工作原理

1. **PlanningState** — 分析背包 + 计算所需材料，若不够则前往输入容器
2. **TakeMaterialsState** — 打开输入容器，shift-click 取料，循环直到凑够或容器耗尽
3. **PlaceRecipeState** — 打开工作台，按配方位置摆放物品（批量：满格 64 时左键整组，尾批右键逐个）
4. **TakeResultState** — 循环 shift-click 取产物直到格子空
5. **DepositResultState** — 打开输出容器，存入产物
6. 循环直到目标达成，或材料/输出超时停止

**重试机制**：每个容器至少检查 2 次，30 秒超时，重试间隔 2 秒。任务停止后覆盖层 5 秒淡出，报告输出到聊天栏。

## 构建

```bash
./gradlew build    # 构建 1.21
```

## 配置文件

`.minecraft/config/autocraft/`：

- `config.json` — 重试间隔、超时等
- `recipes.json` — 保存的合成表
- `stations.json` — 合成站配置

## 许可证

MIT
