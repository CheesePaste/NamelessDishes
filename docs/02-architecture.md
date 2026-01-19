# 架构设计

## 整体架构

Nameless Dishes 采用模块化、可扩展的架构设计，主要分为以下几个层次：

```
┌─────────────────────────────────────────┐
│         Minecraft/Forge 层              │
├─────────────────────────────────────────┤
│          Mixin 集成层                   │
│  - CookingPotBlockEntityMixin          │
├─────────────────────────────────────────┤
│       兼容性管理层                      │
│  - ModCompatibilityManager             │
├─────────────────────────────────────────┤
│       API 接口层                        │
│  - ICookingStation                     │
│  - ICookingRecipeHandler               │
├─────────────────────────────────────────┤
│       功能实现层                        │
│  - 物品系统                            │
│  - 配置系统                            │
│  - 工具类                              │
└─────────────────────────────────────────┘
```

## 核心模块

### 1. API 接口层 (`api/)

#### ICookingStation
定义了烹饪站的标准接口，用于与不同模组的烹饪方块交互：

```java
public interface ICookingStation {
    // 获取烹饪站中的输入物品
    List<ItemStack> getInputItems(BlockEntity blockEntity);

    // 设置烹饪站的输出
    void setOutput(BlockEntity blockEntity, ItemStack result);

    // 检查烹饪站是否正在工作
    boolean isCooking(BlockEntity blockEntity);

    // 重置烹饪站状态
    void resetCooking(BlockEntity blockEntity);

    // 获取当前配方
    Recipe<?> getCurrentRecipe(BlockEntity blockEntity);
}
```

#### ICookingRecipeHandler
定义了配方处理器的接口，用于处理不同模组的配方逻辑：

```java
public interface ICookingRecipeHandler {
    // 检查是否支持该方块
    boolean isSupportedBlock(BlockEntity blockEntity);

    // 检查是否允许无名烹饪
    boolean allowNamelessCrafting(Level level, BlockEntity blockEntity, List<ItemStack> inputs);

    // 创建无名料理
    ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs);

    // 执行烹饪
    boolean executeCooking(Level level, BlockEntity blockEntity, List<ItemStack> inputs, ItemStack result);
}
```

### 2. 兼容性管理层 (`compatibility/)

#### ModCompatibilityManager
核心管理器，负责：
- 检测已安装的烹饪模组
- 管理各个模组的兼容处理器
- 提供统一的处理器访问接口

**关键方法：**
- `initialize()`: 初始化所有兼容处理器
- `getHandlerForBlock()`: 根据方块实体获取对应的处理器
- `getHandlerForMod()`: 根据模组 ID 获取处理器
- `registerHandler()`: 动态注册新的处理器

#### 处理器实现
- `BaseCookingHandler`: 基础处理器，提供默认实现
- `FarmersDelightHandler`: Farmer's Delight 专用处理器

### 3. Mixin 集成层 (`mixin/)

#### CookingPotBlockEntityMixin
通过 Mixin 技术拦截 Farmer's Delight 的烹饪锅逻辑：

**注入点：** `cookingTick` 方法头部
**工作流程：**
1. 检查是否为服务端
2. 检查是否有原版配方
3. 如果没有原版配方，调用无名料理逻辑
4. 创建并输出无名料理

### 4. 物品系统 (`item/)

#### AbstractNamelessDishItem
所有料理物品的基类，提供核心功能：

**NBT 数据结构：**
```java
{
  "FoodLevel": int,        // 饥饿值
  "Saturation": float,     // 饱和度
  "WithBowl": boolean,     // 是否需要碗
  "Ingredients": [         // 食材列表
    {
      // ItemStack NBT 数据
    }
  ]
}
```

**关键方法：**
- `createDish()`: 创建料理物品
- `setDishData()`: 设置料理 NBT 数据
- `getFoodProperties()`: 动态获取食物属性
- `appendHoverText()`: 显示食材信息

#### NamelessDishItem
普通料理物品，可堆叠，食用后不返回容器。

#### NamelessDishWithBowlItem
带碗的料理物品，不可堆叠，食用后返回空碗。

### 5. 配置系统 (`config/)

#### NDConfig
Forge 配置实现，分为三个部分：

**通用设置 (general)**
- `enableFramework`: 启用/禁用框架
- `debugMode`: 调试模式
- `enabledMods`: 启用的模组列表
- `cookingBlocks`: 通用烹饪方块列表

**无名料理设置 (nameless_dishes)**
- `baseSaturationMultiplier`: 饱食度乘数 (0.1-2.0)
- `baseHungerMultiplier`: 饥饿值乘数 (10-200)
- `requireCooking`: 是否需要烹饪
- `minIngredients`: 最少食材数量 (1-9)
- `maxIngredients`: 最多食材数量 (1-9)

**模组特定设置 (mod_specific)**
- 各模组的专用配置

## 设计模式

### 1. 策略模式 (Strategy Pattern)
通过 `ICookingRecipeHandler` 接口，为不同模组实现不同的烹饪策略。

### 2. 单例模式 (Singleton Pattern)
`ModCompatibilityManager` 使用单例模式确保全局只有一个实例。

### 3. 工厂模式 (Factory Pattern)
通过 `ModCompatibilityManager` 根据模组 ID 创建对应的处理器。

### 4. 模板方法模式 (Template Method Pattern)
`AbstractNamelessDishItem` 定义了料理物品的通用行为，子类实现特定逻辑。

## 数据流

### 烹饪流程
```
玩家放入食材
    ↓
Mixin 拦截烹饪逻辑
    ↓
检查是否有原版配方
    ↓ (无原版配方)
获取对应模组处理器
    ↓
检查是否允许无名烹饪
    ↓
计算食物属性
    ↓
创建无名料理物品
    ↓
执行烹饪输出
    ↓
更新 GUI 和方块状态
```

### 扩展流程
```
开发者创建新处理器
    ↓
实现 ICookingRecipeHandler
    ↓
在 CompatibilityConfig 注册
    ↓
ModCompatibilityManager 自动加载
    ↓
创建对应的 Mixin（如果需要）
    ↓
测试功能
```

## 依赖关系

```
NamelessDishes (主类)
    ↓
ModCompatibilityManager
    ↓
ICookingRecipeHandler (接口)
    ↓
FarmersDelightHandler / BaseCookingHandler
    ↓
AbstractNamelessDishItem
    ↓
NamelessDishItem / NamelessDishWithBowlItem
```
