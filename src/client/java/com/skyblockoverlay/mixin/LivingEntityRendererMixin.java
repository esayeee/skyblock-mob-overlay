package com.skyblockoverlay.mixin;

import com.skyblockoverlay.config.ConfigManager;
import com.skyblockoverlay.config.OverlayConfig;
import com.skyblockoverlay.data.MobData;
import com.skyblockoverlay.data.MobDataParser;
import com.skyblockoverlay.render.EntityOverlayRenderer;
import com.skyblockoverlay.util.SkyblockServerDetector;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 挂载点：LivingEntityRenderer#render(...)。
 *
 * 26.1 起原版实体渲染大量迁移到了 EntityRenderState 快照对象上
 * （避免渲染线程直接读 Entity 字段），具体重构后的方法签名/字段名
 * 请以 Loom genSources 反编译出的官方映射源码为准 —— 这里给出的是
 * 语义上等价的注入点：在"实体已经完成一次完整的正常世界渲染"之后
 * （TAIL），复用同一个 MatrixStack + VertexConsumerProvider 再叠加
 * 绘制一层信息。因为矩阵栈和原版实体渲染共享，所以天然继承同样的
 * 深度测试 / 视锥剔除行为——只有屏幕上真正可见的实体才会被这个
 * Mixin 触发绘制调用。
 *
 * 泛型参数按 26.1 的三段式（Entity / RenderState / Model）书写；
 * 若实际版本仍是旧的两段式 <T extends LivingEntity, M>，
 * 把 render 的参数换回 (T entity, float yaw, float tickDelta, ...) 即可。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<
        T extends LivingEntity,
        S extends LivingEntityRenderState,
        M> {

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void skyblockOverlay$afterRender(
            T entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        OverlayConfig cfg = ConfigManager.get();
        if (!cfg.masterEnabled) return;

        // 只在检测到当前连接的是目标服务器时启用，避免在其它服务器上误触发
        if (!SkyblockServerDetector.isLikelySkyblock()) return;

        if (cfg.useMobFilter && !cfg.mobFilterList.isEmpty()) {
            String name = entity.getName().getString();
            boolean matched = cfg.mobFilterList.stream().anyMatch(name::contains);
            if (!matched) return;
        }

        MobData data = MobDataParser.parse(entity, cfg);
        EntityOverlayRenderer.render(entity, data, cfg, matrices, vertexConsumers, light, tickDelta);
    }
}
