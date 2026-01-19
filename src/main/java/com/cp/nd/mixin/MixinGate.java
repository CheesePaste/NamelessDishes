package com.cp.nd.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class MixinGate implements IMixinConfigPlugin {
    private static boolean isDataGen() {
        return "true".equalsIgnoreCase(System.getProperty("namelessdishes.datagen"))
                || "true".equalsIgnoreCase(System.getProperty("forge.datagen"));
    }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !isDataGen();
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}