# 待实现功能与开发路线图

## 当前状态

### 已完成功能 ✅
- [x] 自由烹饪核心系统
- [x] 动态食物属性计算
- [x] Farmer's Delight 完整集成
- [x] NBT 数据存储和读取
- [x] 食材 Tooltip 显示
- [x] 配置系统
- [x] 模组检测和兼容性管理
- [x] 带碗/不带碗料理物品

### 进行中功能 🔄
- [ ] 食谱解锁系统（部分完成）

## 高优先级功能 🔴

### 1. 食谱解锁系统

#### 功能描述
隐藏原模组的配方，玩家在首次成功烹饪出某个料理后，才能解锁对应的配方。这样既保留了自由烹饪的乐趣，又不会让原模组的内容失去意义。

#### 实现方案

**新增文件结构：**
```
src/main/java/com/cp/nd/
├── recipe/
│   ├── RecipeUnlockManager.java      # 解锁管理器
│   ├── UnlockedRecipeData.java       # 解锁数据存储
│   └── RecipeMatcher.java            # 配方匹配器
├── network/
│   ├── RecipeUnlockPacket.java       # 解锁同步数据包
│   └── NetworkHandler.java           # 网络处理器
└── persistence/
    └── PlayerProgressData.java       # 玩家进度持久化
```

**核心类设计：**

`RecipeUnlockManager.java`
```java
public class RecipeUnlockManager {
    // 检查是否应该解锁配方
    public boolean shouldUnlockRecipe(ItemStack result);

    // 解锁配方
    public void unlockRecipe(ServerPlayer player, Recipe<?> recipe);

    // 检查配方是否已解锁
    public boolean isRecipeUnlocked(ServerPlayer player, ResourceLocation recipeId);

    // 同步解锁数据到客户端
    public void syncUnlockedRecipes(ServerPlayer player);
}
```

`UnlockedRecipeData.java`
```java
public class UnlockedRecipeData {
    private Set<ResourceLocation> unlockedRecipes;

    // 从 NBT 加载
    public static UnlockedRecipeData fromNBT(CompoundTag tag);

    // 保存到 NBT
    public CompoundTag toNBT();

    // 添加解锁的配方
    public void addUnlockedRecipe(ResourceLocation recipeId);

    // 检查是否已解锁
    public boolean isUnlocked(ResourceLocation recipeId);
}
```

**配置选项：**
```toml
[recipe_unlock]
    # 是否启用配方解锁系统
    enabled = true
    # 解锁方式：first_craft（首次制作）/ similar（相似料理）
    unlockMode = "first_craft"
    # 是否隐藏未解锁的配方
    hideUnlockedRecipes = true
    # 相似度阈值（0.0-1.0）
    similarityThreshold = 0.8
```

**修改的现有文件：**
- `NamelessDishes.java`: 注册网络系统和解锁管理器
- `CookingPotBlockEntityMixin.java`: 在烹饪完成后检查并解锁配方
- `NDConfig.java`: 添加解锁相关配置

#### 预期效果
1. 玩家首次烹饪时看不到任何 Farmer's Delight 的配方
2. 通过自由烹饪制作出类似料理后，自动解锁对应的配方
3. 解锁的配方保存在玩家数据中，跨世界持久化
4. 支持服务器端配置和客户端同步

### 2. 名声系统（Reputation System）

#### 功能描述
玩家制作的料理越多，名声越高，能获得特殊效果或奖励。

#### 实现方案

**新增文件结构：**
```
src/main/java/com/cp/nd/reputation/
├── PlayerReputation.java             # 玩家名声数据
├── ReputationManager.java            # 名声管理器
└── events/
    └── ReputationEventHandler.java   # 名声事件处理
```

**核心机制：**
- 制作新的料理组合 = +1 名声
- 制作已有料理 = +0.1 名声
- 名声等级：
  - 新手厨师 (0-10): 无特殊效果
  - 熟练厨师 (11-50): 饱食度 +10%
  - 大厨 (51-100): 饱食度 +20%，烹饪速度 +10%
  - 传奇厨师 (101+): 饱食度 +30%，烹饪速度 +20%

### 3. 食谱提示系统

#### 功能描述
当玩家放入的食材接近某个已知配方时，给予提示。

#### 实现方案

**新增文件结构：**
```
src/main/java/com/cp/nd/hint/
├── RecipeHintManager.java            # 提示管理器
├── RecipeSimilarity.java             # 配方相似度计算
└── gui/
    └── RecipeHintOverlay.java        # GUI 提示覆盖层
```

**相似度算法：**
```java
public double calculateSimilarity(List<ItemStack> inputs, Recipe<?> recipe) {
    // 获取配方所需物品
    List<Ingredient> recipeIngredients = recipe.getIngredients();

    // 计算交集
    int matches = 0;
    for (ItemStack input : inputs) {
        for (Ingredient ingredient : recipeIngredients) {
            if (ingredient.test(input)) {
                matches++;
                break;
            }
        }
    }

    // 相似度 = 匹配数 / max(输入数, 配方数)
    return (double) matches / Math.max(inputs.size(), recipeIngredients.size());
}
```

## 中优先级功能 🟡

### 4. 更多烹饪站支持

#### 目标模组
- [ ] Cooking for Blockheads
- [ ] Pam's HarvestCraft 2
- [ ] Sparse Flowers
- [ ] 其他流行的烹饪模组

#### 实现方案

**新增文件结构：**
```
src/main/java/com/cp/nd/compatibility/
├── cfb/
│   └── CookingForBlockheadsHandler.java
├── pam/
│   └── PamsHarvestCraftHandler.java
└── ...
```

**实现步骤：**
1. 分析目标模组的烹饪方块和配方系统
2. 实现 `ICookingRecipeHandler` 接口
3. 创建对应的 Mixin（如果需要）
4. 在 `CompatibilityConfig` 中注册
5. 测试功能

### 5. JEI/REI 集成

#### 功能描述
在 JEI/REI 中显示自由烹饪的配方，包括：
- 玩家制作过的料理组合
- 配方的相似度提示
- 营养价值预览

#### 实现方案

**新增文件结构：**
```
src/main/java/com/cp/nd/integration/
├── jei/
│   ├── NamelessDishCategory.java     # JEI 分类
│   ├── NamelessDishRecipe.java       # JEI 配方
│   └── JEIPlugin.java                # JEI 插件
└── rei/
    └── ... (类似结构)
```

### 6. 食材质量系统

#### 功能描述
为食材添加质量属性，影响最终料理的品质。

**质量等级：**
- 普通 (品质倍率 1.0x)
- 优秀 (品质倍率 1.2x)
- 稀有 (品质倍率 1.5x)
- 传说 (品质倍率 2.0x)

**实现方案：**
```java
public class IngredientQuality {
    public static Quality getQuality(ItemStack stack);

    public enum Quality {
        COMMON(1.0f),
        GOOD(1.2f),
        RARE(1.5f),
        LEGENDARY(2.0f);

        private final float multiplier;
    }
}
```

## 低优先级功能 🟢

### 7. 料理成就系统

#### 成就列表
- [ ] "初次尝试" - 制作第一个无名料理
- [ ] "疯狂科学家" - 制作 100 种不同的料理
- [ ] "大厨" - 解锁所有 Farmer's Delight 配方
- [ ] "完美的平衡" - 制作一个完美的平衡料理

### 8. 自定义料理名称

玩家可以为自己的料理创建命名，增强个性化体验。

### 9. 料理装饰系统

使用额外的装饰物品（如花草）来美化料理，提供额外的视觉奖励。

## 技术债务和改进

### 代码质量
- [ ] 添加更多单元测试
- [ ] 完善 JavaDoc 注释
- [ ] 重构 `BaseCookingHandler`，减少重复代码
- [ ] 优化 NBT 数据存储效率

### 性能优化
- [ ] 实现配方缓存机制
- [ ] 优化相似度计算算法
- [ ] 减少不必要的数据复制

### 国际化
- [ ] 完善英文翻译
- [ ] 添加其他语言支持（如中文、日语）

## 预期时间线

### v0.2.0 - 食谱解锁系统
- 预计开发时间：2-3 周
- 包含功能：配方解锁、数据持久化、网络同步

### v0.3.0 - 更多烹饪站支持
- 预计开发时间：3-4 周
- 包含功能：Cooking for Blockheads、Pam's HarvestCraft 2

### v0.4.0 - 食谱提示系统
- 预计开发时间：2 周
- 包含功能：相似度计算、GUI 提示

### v1.0.0 - 正式发布
- 整合所有功能
- 完善文档和本地化
- 性能优化和 bug 修复

## 贡献指南

欢迎社区贡献！如果你想帮助实现这些功能：

1. 在 GitHub Issues 中声明你要实现的功能
2. Fork 项目并创建功能分支
3. 遵循现有代码风格
4. 添加必要的测试
5. 提交 Pull Request

### 开发环境设置
```bash
# 克隆项目
git clone https://github.com/yourusername/NamelessDishes.git

# 导入到 IDE（推荐 IntelliJ IDEA）

# 运行游戏（开发环境）
./gradlew runClient
```

### 代码规范
- 使用 Java 17 特性
- 遵循 Google Java Style Guide
- 所有公共方法需要 JavaDoc
- 使用英文注释和变量名
