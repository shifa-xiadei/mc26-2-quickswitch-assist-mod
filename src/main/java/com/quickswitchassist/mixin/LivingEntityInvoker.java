package com.quickswitchassist.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 原版属性容器只在 {@code LivingEntity.tick()} 末尾刷新一次。
 * 秒切换完武器后必须立刻强制刷新，否则射程/攻击力还是上一把武器的值。
 *
 * <p>26.2：原 yarn 的 {@code updateAttributes()} 在官方名字里叫 {@code refreshDirtyAttributes()}。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("refreshDirtyAttributes")
    void quickswitchassist$updateAttributes();
}
