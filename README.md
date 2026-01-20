# Nameless Dishes

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[**中文文档**](README_zh.md) | English

---

## About

A Minecraft mod that allows players to cook dishes using any combination of ingredients.

### Core Features

- **Free Cooking System** - Cook dishes with any ingredient combination
- **Recipe Unlocking** - Recipes are locked by default (configurable), unlocked only after crafting once
- **Exploration Gameplay** - Get hints through trial and error, experience "alchemy-style" cooking
- **Dynamic Attributes** - Dish saturation and hunger values calculated dynamically based on ingredients
- **Recipe Recording** - Successful combinations can be recorded in recipe books

---

## Version Info

| Item | Information |
|------|-------------|
| **Development Version** | Minecraft 1.20.1 + Forge 47.4.0 |
| **Planned Support** | 1.20.1 Forge, 1.21.1 Forge |
| **License** | [MIT License](LICENSE) |
| **Author** | CheesePaste |
| **Development** | AI-Assisted Development |

---

## Development Progress

### ✅ Implemented Features

- Free cooking with any ingredient combination
- Nameless dish and bowl of nameless dish items
- Dynamic calculation of hunger and saturation based on ingredients
- Item tooltip displaying ingredient information
- Dish naming and recipe synthesis system

### 🚧 Short-term Goals

Implement core functionality on Farmers' Delight cooking pot

---

## Roadmap

### 🔴 High Priority

#### 1. Hidden Recipe System

- [ ] Hide original cooking pot recipes
- [ ] Unlock recipes only after player crafts them
- [ ] Vanilla recipe viewing only (JEI integration not planned)

#### 2. Hint System

- [ ] Find recipes closest to current ingredients
- [ ] Provide appropriate hints based on similarity algorithms (e.g., intersection/union size)

### 🟡 Medium Priority

#### 1. Dish Naming and Recipe Synthesis

- [ ] Allow players to name nameless dishes on crafting table or specific blocks
- [ ] Named dishes can be recorded in recipe books
- [ ] Support custom dish names and descriptions

#### 2. Recipe Sharing System

- [ ] Players can create recipe books from successful combinations
- [ ] Recipe books can be traded or shared between players
- [ ] Using a recipe book unlocks the corresponding cooking pot recipe

---

## Future Plans

- [ ] Support more modded cooking stations
- [ ] Implement recipe item damage and inheritance mechanics
- [ ] Add dish rating system (based on ingredient combination logic)
- [ ] Support cross-mod integration, adding special effects for other mod ingredients

---

## System Details

### Nameless Dish System

1. **Dynamic Attribute Calculation**
   - Weighted calculation based on ingredient hunger and saturation values
   - Considers nutritional value and rarity of ingredients
   - Configurable weights and calculation methods

2. **Ingredient Tracking**
   - Records all ingredient information during crafting
   - Displays ingredient list on hover
   - Supports NBT-sensitive ingredient recognition

3. **Naming and Recipe Creation**
   - Use crafting table or special workstation to name dishes
   - Create recipe books from named dishes
   - Recipe books can be stored in player's personal recipe collection

### Unlocking Mechanism

- All cooking pot recipes hidden initially
- Recipes unlocked upon successful crafting
- Recipes can be recorded and shared via recipe books
- Configurable difficulty settings for unlocking

---

## Dependencies

- **Minecraft**: 1.20.1
- **Forge**: 47.4.0
- **Farmers Delight**: (via CurseMaven)
- **Optional**: JEI and other recipe viewing mods (future compatibility)

---

## Configuration Options

```
# General Settings
general {
  # Whether to hide original recipes
  hideOriginalRecipes=true
  
  # Unlock difficulty: easy(craft once), normal(craft 3 times), hard(craft 5 times)
  unlockDifficulty="normal"
  
  # Whether to enable hint system
  enableHints=true
}

# Attribute Calculation
calculation {
  # Hunger value calculation weight
  hungerWeight=1.0
  
  # Saturation calculation weight  
  saturationWeight=1.0
  
  # Whether to consider ingredient rarity
  considerRarity=true
}
```

---

## Resources

- [Forge Documentation](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)
- [Forge Forums](https://forums.minecraftforge.net/)
- [Forge Discord](https://discord.minecraftforge.net/)
- [Farmers Delight Mod](https://www.curseforge.com/minecraft/mc-mods/farmers-delight)

---

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details

---

## AI Development Statement

This project is developed with AI assistance. The AI helps with:
- Code generation and optimization
- Documentation writing
- Problem-solving and debugging
- System design and architecture planning

All code is reviewed and maintained by human developers.
