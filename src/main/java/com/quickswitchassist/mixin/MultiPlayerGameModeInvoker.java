package com.quickswitchassist.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 原版发包同步选中槽是 private —— 调用它必须走 @Invoker，不能用 @Shadow。
 *  <p>26.2：类名 MultiPlayerGameMode，方法名 ensureHasSentCarriedItem（原 yarn 的 syncSelectedSlot）。 */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeInvoker {

    @Invoker("ensureHasSentCarriedItem")
    void quickswitchassist$syncSelectedSlot();
}
