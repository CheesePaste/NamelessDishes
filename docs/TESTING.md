# NamelessDishes Testing Framework

This document describes the testing framework implemented for the NamelessDishes Minecraft mod.

## Overview

The testing framework uses a hybrid approach:
- **JUnit 5** for testing pure Java logic (path conversion, JSON serialization)
- **Forge GameTest** for testing game mechanics and integration

## Test Statistics

| Test Type | Count | Purpose |
|-----------|-------|---------|
| JUnit Tests | ~69 tests | Pure logic, data validation |
| GameTest Classes | 3 classes | Game integration tests |

## Project Structure

```
src/
├── test/java/com/cp/nd/
│   ├── util/
│   │   └── FoodUtilTest.java                    # Nutrition value calculation tests
│   ├── recipe/storage/
│   │   ├── NamelessDishRecipeDataTest.java     # JSON serialization tests
│   │   └── RecipeStorageManagerTest.java       # Path conversion tests
│   ├── recipe/
│   │   └── RecipeRegisterManagerTest.java      # Validation logic tests
│   └── item/
│       └── AbstractNamelessDishItemTest.java   # NBT read/write tests (not implemented)
│
└── main/java/com/cp/nd/test/
    ├── RecipeTest.java                          # Recipe system GameTests
    ├── ItemTest.java                            # Item system GameTests
    └── IntegrationTest.java                     # Complete workflow GameTests
```

## Running Tests

### Run JUnit Tests
```bash
./gradlew test
```

Test results are saved to: `build/reports/tests/test/index.html`

### Run GameTest
```bash
./gradlew runGameTestServer
```

## Test Implementation Details

### JUnit Tests

These tests focus on code that doesn't require Minecraft game environment:

1. **RecipeStorageManagerTest** - Tests path conversion logic
   - ✅ Block ID to directory name conversion
   - ✅ Round-trip conversions
   - ✅ Null/empty input handling
   - ✅ Special characters handling
   - **Status**: PASSING

2. **NamelessDishRecipeDataTest** - Tests JSON serialization
   - ✅ Recipe data creation
   - ✅ JSON serialization/deserialization
   - ✅ Ingredient data handling
   - ✅ Missing field validation
   - ✅ NBT data handling
   - **Status**: PASSING

3. **RecipeRegisterManagerTest** - Tests registration validation
   - ⚠️ Requires GameTest environment for most tests
   - ✅ Singleton pattern validation
   - **Status**: PLACEHOLDER (requires GameTest)

4. **FoodUtilTest** - Tests nutrition calculation
   - ⚠️ Requires Minecraft game environment
   - **Status**: PLACEHOLDER (requires GameTest)

### GameTest Classes

These tests run in an actual Minecraft game environment:

1. **RecipeTest** (9 tests)
   - Recipe registration
   - Recipe ID generation
   - Recipe uniqueness
   - Duplicate handling
   - Cooking block differences
   - Bowl vs no-bowl variants
   - Ingredient count boundaries

2. **ItemTest** (11 tests)
   - Dish creation
   - NBT data validation
   - Edibility check
   - Nutrition value calculation
   - Ingredient storage
   - Container item behavior
   - Non-edible item filtering

3. **IntegrationTest** (9 tests)
   - Complete cooking flow
   - **NBT stacking prevention** - Different recipes don't stack
   - **Same recipe stacking** - Identical recipes stack correctly
   - **Output slot full with different NBT** - Boundary test
   - **Partial filling of output slot** - Stack management
   - Recipe persistence
   - Configuration boundaries
   - Multiple recipe types
   - Ingredient ordering independence
   - Concurrent registration

## Key Test Scenarios

### NBT Stacking Tests (Critical)

The integration tests include comprehensive NBT stacking tests:

1. **testNBTStackingPrevention**: Verifies that dishes with different ingredients don't stack
2. **testSameRecipeStacking**: Verifies that identical dishes can stack
3. **testOutputSlotFullWithDifferentNBT**: Tests behavior when output slot is full
4. **testPartialFillingOfOutputSlot**: Tests partial stack filling

These tests ensure that:
- Recipes with different NBT data remain separate
- Recipes with identical NBT data stack correctly
- Output slot management handles edge cases properly

## Configuration

### JUnit Dependencies (build.gradle)
```gradle
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.0'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.0'
testImplementation 'org.mockito:mockito-core:5.7.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.7.0'
```

### GameTest Registration
GameTests are registered in `NamelessDishes.java`:
```java
modEventBus.addListener(this::registerGameTests);

private void registerGameTests(RegisterGameTestsEvent event) {
    event.register(RecipeTest.class);
    event.register(ItemTest.class);
    event.register(IntegrationTest.class);
}
```

## Test Coverage

### What Works (JUnit)
- ✅ Path conversion logic
- ✅ JSON serialization/deserialization
- ✅ Data model validation

### What Requires GameTest
- ⚠️ ItemStack creation and manipulation
- ⚠️ Nutrition value calculation (requires FoodProperties)
- ⚠️ Recipe registration (requires RecipeManager)
- ⚠️ NBT read/write (requires game environment)
- ⚠️ Cooking pot interaction
- ⚠️ Complete workflow testing

## Known Limitations

1. **JUnit Tests**: Most JUnit tests are placeholders because they require the Minecraft game environment. The useful JUnit tests are those that test pure Java logic without Minecraft dependencies.

2. **Structure Templates**: The GameTest structure templates (`empty_template.nbt`, `cooking_pot_template.nbt`) need to be created. See `src/main/resources/data/nameless_dishes/structures/test/README.md` for instructions.

3. **FoodUtil Tests**: Cannot be properly tested in JUnit because they require:
   - ItemStack instances
   - FoodProperties from game registry
   - BlockEntityType registration

## Next Steps

1. **Create Structure Templates**: Follow the instructions in `src/main/resources/data/nameless_dishes/structures/test/README.md` to create the required NBT structure files.

2. **Run GameTests**: Execute `./gradlew runGameTestServer` to run the integration tests in a real game environment.

3. **Extend Tests**: Add more GameTest scenarios as new features are implemented.

## Test Results Summary

### Current Status
- **JUnit Tests**: 69 tests total
  - Passing: ~34 tests (RecipeStorageManagerTest, NamelessDishRecipeDataTest)
  - Failing/Placeholder: ~35 tests (require GameTest environment)

- **GameTest Classes**: 3 classes with 29 test methods total
  - Ready to run once structure templates are created

### Expected GameTest Results
Once structure templates are created and GameTests are run:
- RecipeTest: 9 tests
- ItemTest: 11 tests
- IntegrationTest: 9 tests

Total: 29 GameTest methods

## References

- [Game Tests - Minecraft Forge Documentation](https://docs.minecraftforge.net/en/1.18.x/misc/gametest/)
- [How to use the Game Test Framework on Forge](https://gist.github.com/SizableShrimp/60ad4109e3d0a23107a546b3bc0d9752)
- [Game Tests | NeoForged docs](https://docs.neoforged.net/docs/misc/gametest)

## Contributing

When adding new tests:

1. **For pure Java logic**: Add JUnit tests in `src/test/java/com/cp/nd/`
2. **For game mechanics**: Add GameTests in `src/main/java/com/cp/nd/test/`
3. **Register GameTests**: Add to `registerGameTests()` in `NamelessDishes.java`
4. **Update this document**: Keep test statistics current

## Test Maintenance

- Keep test data isolated
- Use descriptive test names
- Document complex test scenarios
- Update structure templates when test requirements change
