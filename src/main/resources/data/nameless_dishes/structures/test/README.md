# GameTest Structure Templates

This directory contains the NBT structure files used by GameTests.

## Required Templates

### 1. empty_template.nbt
A basic empty 3x3x3 test space with no blocks.
Used by tests that don't require specific blocks (like RecipeTest and ItemTest).

### 2. cooking_pot_template.nbt
A test space containing a Farmers' Delight cooking pot at position (1, 1, 1).
Used by IntegrationTest tests that need to interact with the cooking pot.

## How to Create These Templates

### Option 1: Using In-Game Structure Block (Recommended)

1. **Launch your test client**:
   ```bash
   ./gradlew runClient
   ```

2. **Create the structure in-game**:
   - Place a structure block: `/give @p structure_block`
   - Place it at the corner where you want your structure to start
   - Right-click the structure block to open its GUI

3. **For empty_template**:
   - Set "Data" mode (corner button)
   - Set size to X=3, Y=3, Z=3
   - Set structure name to `nameless_dishes:test/empty_template`
   - Click "Save" (this creates an empty area)

4. **For cooking_pot_template**:
   - First create a 3x3x3 empty area as above
   - Place a cooking pot at position (1, 1, 1) relative to the structure block
   - Set structure name to `nameless_dishes:test/cooking_pot_template`
   - Click "Save"

5. **Export the structure**:
   - The structure file will be saved to:
     `run/saves/world_name/generated/minecraft/structures/nameless_dishes/test/`
   - Copy the `.nbt` file to:
     `src/main/resources/data/nameless_dishes/structures/test/`

### Option 2: Using NBT Editors

You can use tools like:
- [Amidst](https://github.com/toolbox4minecraft/amidst)
- [NBTExplorer](https://github.com/jaquadro/NBTExplorer)
- [Minecraft Structure Editor](https://www.planetminecraft.com/tool/structure-editor/)

To create the NBT files manually.

### Option 3: Generate Programmatically

You can also write a small mod or use commands to export structures:

```
/structure save nameless_dishes:test/empty_template ~~~ ~~~ ~~~
```

## Template Format

The templates should follow this format:
- Size: 3x3x3 blocks minimum
- Origin: Bottom-north-west corner
- Important blocks should be at reasonable positions (not at edges)

For cooking_pot_template:
- Cooking pot at (1, 1, 1)
- Air blocks elsewhere
- Optional: Heat source below the cooking pot at (1, 0, 1)

## Current Status

- [ ] empty_template.nbt - NOT YET CREATED
- [ ] cooking_pot_template.nbt - NOT YET CREATED

## Testing Without Templates

If templates are missing, you can still run tests by:
1. Using the empty template name in @GameTest annotation
2. The test will use a default empty structure from the GameTest framework

Note: Some tests may fail if they expect specific blocks to be present.
