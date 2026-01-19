# 功能实现细节

## 核心功能实现

### 1. 自由烹饪系统

#### 实现位置
- **Mixin**: `mixin/fd/CookingPotBlockEntityMixin.java`
- **Handler**: `compatibility/fd/FarmersDelightHandler.java`
- **配置**: `config/NDConfig.java`

#### 工作原理

**步骤 1: 拦截烹饪逻辑**
```java
@Inject(method = "cookingTick", at = @At("HEAD"), cancellable = true)
private static void onCookingTick(Level level, BlockPos pos, BlockState state,
                                  CookingPotBlockEntity cookingPot, CallbackInfo ci) {
    // 检查是否为客户端
    if (level.isClientSide()) return;

    // 检查加热状态和输入
    if (!cookingPot.isHeated(level, pos) || !hasInput()) return;
}
```

**步骤 2: 检查原版配方**
```java
// 尝试匹配 Farmer's Delight 的配方
Optional<CookingPotRecipe> recipe = getMatchingRecipe(inventory);
if (recipe.isPresent()) {
    return; // 有原版配方，让原版处理
}
```

**步骤 3: 验证无名烹饪条件**
```java
// 获取输入物品
List<ItemStack> inputs = getInputItems();

// 检查配置条件
- 食材数量在范围内 (minIngredients ~ maxIngredients)
- 所有物品都是食物
- 模组兼容性已启用
```

**步骤 4: 创建无名料理**
```java
// 计算食物属性
int foodLevel = calculateFoodLevel(inputs);
float saturation = calculateSaturation(inputs);

// 创建料理物品
ItemStack namelessResult = NamelessDishItem.createDish(
    foodLevel, saturation, inputs, needsBowl
);
```

### 2. 动态食物属性计算

#### 实现位置
- `compatibility/base/BaseCookingHandler.java:24-76`

#### 计算公式

**饥饿值计算**
```java
// 基础算法
int totalFoodValue = 0;
for (ItemStack input : inputs) {
    if (input.isEdible()) {
        FoodProperties food = input.getFoodProperties();
        totalFoodValue += food.getNutrition();
    }
}

// 应用配置乘数
int finalFoodLevel = (int)(totalFoodValue * (config.baseHungerMultiplier / 100.0));

// 限制范围 (1-20)
finalFoodLevel = Mth.clamp(finalFoodLevel, 1, 20);
```

**饱和度计算**
```java
// 收集所有食材的饱和度
float totalSaturation = 0;
for (ItemStack input : inputs) {
    if (input.isEdible()) {
        FoodProperties food = input.getFoodProperties();
        totalSaturation += food.getSaturationModifier();
    }
}

// 计算平均饱和度并应用乘数
float avgSaturation = totalSaturation / inputs.size();
float finalSaturation = avgSaturation * config.baseSaturationMultiplier;

// 限制范围 (0.1-2.0)
finalSaturation = Mth.clamp(finalSaturation, 0.1f, 2.0f);
```

### 3. NBT 数据管理

#### 实现位置
- `item/AbstractNamelessDishItem.java:85-114`

#### 数据结构

**创建料理时保存数据**
```java
public static void setDishData(ItemStack stack, int foodLevel, float saturation,
                               List<ItemStack> ingredients, boolean withBowl) {
    CompoundTag tag = stack.getOrCreateTag();

    // 保存基础属性
    tag.putInt("FoodLevel", foodLevel);
    tag.putFloat("Saturation", saturation);
    tag.putBoolean("WithBowl", withBowl);

    // 保存食材列表
    ListTag ingredientsList = new ListTag();
    for (ItemStack ingredient : ingredients) {
        if (!ingredient.isEmpty()) {
            CompoundTag ingredientTag = new CompoundTag();
            ItemStack singleItem = ingredient.copy();
            singleItem.setCount(1); // 只保存一个
            singleItem.save(ingredientTag);
            ingredientsList.add(ingredientTag);
        }
    }
    tag.put("Ingredients", ingredientsList);
}
```

**读取数据**
```java
// 获取饥饿值
public static int getFoodLevel(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    return tag != null ? tag.getInt("FoodLevel") : 0;
}

// 获取饱和度
public static float getSaturation(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    return tag != null ? tag.getFloat("Saturation") : 0.0f;
}

// 获取食材列表
public static List<ItemStack> getIngredients(ItemStack stack) {
    List<ItemStack> ingredients = new ArrayList<>();
    CompoundTag tag = stack.getTag();

    if (tag != null && tag.contains("Ingredients", Tag.TAG_LIST)) {
        ListTag ingredientsList = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredientsList.size(); i++) {
            CompoundTag ingredientTag = ingredientsList.getCompound(i);
            ingredients.add(ItemStack.of(ingredientTag));
        }
    }

    return ingredients;
}
```

### 4. Tooltip 显示

#### 实现位置
- `item/AbstractNamelessDishItem.java:32-57`

#### 显示逻辑
```java
@Override
public void appendHoverText(ItemStack stack, Level level,
                            List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, level, tooltip, flag);

    CompoundTag tag = stack.getTag();
    if (tag != null && tag.contains("Ingredients", Tag.TAG_LIST)) {
        // 显示标题
        tooltip.add(Component.translatable("tooltip.nameless_dishes.ingredients")
                .withStyle(ChatFormatting.DARK_GRAY));

        // 显示食材列表
        ListTag ingredients = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ingredientTag = ingredients.getCompound(i);
            ItemStack ingredientStack = ItemStack.of(ingredientTag);
            if (!ingredientStack.isEmpty()) {
                // 缩进显示食材名称
                tooltip.add(Component.literal("  ")
                    .append(ingredientStack.getHoverName())
                    .withStyle(ChatFormatting.DARK_GREEN));
            }
        }
    }
}
```

### 5. 容器物品处理

#### 实现位置
- `item/NamelessDishWithBowlItem.java`

#### 工作原理
```java
public class NamelessDishWithBowlItem extends AbstractNamelessDishItem {

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
                                     LivingEntity entity) {
        // 调用父类方法处理食用
        super.finishUsingItem(stack, level, entity);

        // 返回空碗
        if (!level.isClientSide()) {
            return new ItemStack(Items.BOWL);
        }
        return stack;
    }

    @Override
    public boolean isEdible() {
        return true;
    }
}
```

### 6. 配置系统

#### 实现位置
- `config/NDConfig.java`
- `NamelessDishes.java:29`

#### 配置加载流程
```java
// 1. 注册配置文件
ModLoadingContext.get().registerConfig(
    ModConfig.Type.COMMON,
    NDConfig.SPEC,
    "namelessdishes-common.toml"
);

// 2. 配置文件结构
[general]
    enableFramework = true
    debugMode = false
    enabledMods = ["farmersdelight"]

[nameless_dishes]
    baseSaturationMultiplier = 0.6
    baseHungerMultiplier = 80
    requireCooking = true
    minIngredients = 1
    maxIngredients = 9
```

#### 配置热重载
```java
public void reload() {
    CompatibilityConfig.reload();  // 重新加载兼容性配置
    initialize();                   // 重新初始化处理器
}
```

### 7. 模组检测

#### 实现位置
- `util/ModDetectionUtil.java`

#### 检测逻辑
```java
public class ModDetectionUtil {
    private static final Set<String> KNOWN_COOKING_MODS = Set.of(
        "farmersdelight",
        "cookingforblockheads",
        "pamhc2foodcore"
    );

    public static Set<String> getLoadedCookingMods() {
        Set<String> loadedMods = new HashSet<>();
        for (String modId : KNOWN_COOKING_MODS) {
            if (ModList.get().isLoaded(modId)) {
                loadedMods.add(modId);
            }
        }
        return loadedMods;
    }
}
```

## 关键技术点

### 1. Mixin 注解配置
```java
@Mixin(CookingPotBlockEntity.class)
public class CookingPotBlockEntityMixin {
    @Inject(
        method = "cookingTick",
        at = @At("HEAD"),
        cancellable = true,
        remap = false  // 重要：不重新映射字段名
    )
    private static void onCookingTick(...) {
        // 注入逻辑
    }
}
```

### 2. 访问器模式
```java
// 使用访问器访问私有方法
CookingPotBlockEntityAccessor accessor = (CookingPotBlockEntityAccessor) cookingPot;
accessor.farmersdelight$hasInput();
accessor.farmersdelight$getInventory();
```

### 3. NBT 数据验证
```java
// 安全地读取 NBT 数据
CompoundTag tag = stack.getTag();
if (tag != null && tag.contains("Key", Tag.TAG_INT)) {
    int value = tag.getInt("Key");
}
```

### 4. 异常处理
```java
try {
    ICookingRecipeHandler handler = compat.getHandlerClass()
        .getDeclaredConstructor()
        .newInstance();
    activeHandlers.put(compat.getModId(), handler);
} catch (InstantiationException | IllegalAccessException e) {
    LOGGER.error("无法实例化处理器: {}", compat.getHandlerClass(), e);
    modStatus.put(compat.getModId(), false);
}
```

## 性能优化

### 1. 并发集合
```java
private final Map<String, ICookingRecipeHandler> activeHandlers =
    new ConcurrentHashMap<>();
```

### 2. 缓存机制
```java
// 处理器实例化后缓存
private final Map<String, ICookingRecipeHandler> handlerCache = new HashMap<>();
```

### 3. 条件检查优化
```java
// 先检查简单条件
if (level.isClientSide()) return;
if (!cookingPot.isHeated(level, pos)) return;
if (!hasInput()) return;

// 再进行复杂计算
if (recipe.isPresent()) return;
```

## 调试支持

### 1. 日志记录
```java
NamelessDishes.LOGGER.debug("Created nameless dish at {}", pos);
NamelessDishes.LOGGER.info("检测到兼容模组: {}", mods);
NamelessDishes.LOGGER.error("无法实例化处理器: {}", className, e);
```

### 2. 调试模式
```java
if (NDConfig.INSTANCE.debugMode.get()) {
    // 输出详细调试信息
    LOGGER.debug("Inputs: {}", inputs);
    LOGGER.debug("Food Level: {}, Saturation: {}", foodLevel, saturation);
}
```
