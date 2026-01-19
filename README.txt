
Nameless Dishes
无名料理
开发版本:1.20.1+Forge47.4.0
预计适配版本:1.20.1Forge，1.21.1Forge
LICENSE:MIT
介绍:这是一个允许玩家使用任意组合制作料理的模组。该模组将会锁定料理配方（可config设置取消锁定），只有玩家制作过一次才会解锁对应配方。同时，该模组会给予玩家提示，让玩家在尝试中解锁食物配方，获得“炼金”式的烹饪体验。
短期开发目标:在农夫乐事厨锅上实现功能。
已实现功能:使用任意组合制作料理。（暂时使用烤马铃薯替代无名料理）
未实现内容:
无名料理物品开发
优先度:高
1.需要制作无名料理和碗装无名料理，且可自定义饱和度，饱食度恢复量。
2.依据原料中食物的饱食度饱和度，输出特定饱食度饱和度无名料理（详见handler）
2.  无名料理物品展示
优先级:中
将合成食材写入无名料理的物品中，并且加入tooltip，鼠标悬停可查看原料信息。
隐藏配方
优先级:高
隐藏厨锅原有的配方，只有玩家制作出对应食物才解锁对应配方。
暂时不考虑jei等其他工具查看配方，只考虑原版查看途径。
加入提示
优先级:中
找到和原料最接近的配方
依据相似性（需要自己制定，例如交集大小/补集大小）给予玩家适当提示
未来展望
适配更多模组的料理台
加入配方物品，损坏配方物品等内容，丰富模组内容。
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
(this does not affect your code) and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your 
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources: 
=========================
Community Documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
LexManos' Install Video: https://youtu.be/8VEdtQLuLO0
Forge Forums: https://forums.minecraftforge.net/
Forge Discord: https://discord.minecraftforge.net/
