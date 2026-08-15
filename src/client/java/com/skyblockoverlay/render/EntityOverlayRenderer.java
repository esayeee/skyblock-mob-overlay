package com.skyblockoverlay.render;

import com.skyblockoverlay.config.OverlayConfig;
import com.skyblockoverlay.data.MobData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

/**
 * 所有绘制调用都：
 *   1) 使用调用方（LivingEntityRenderer.render）已经建立好的 MatrixStack，
 *      也就是绑定在实体世界坐标系上的矩阵——而不是屏幕空间 HUD 坐标；
 *   2) 使用【开启深度测试】的 RenderLayer / TextLayerType，
 *      因此会被地形、方块正常遮挡，天然不具备穿墙能力。
 *
 * 这是与"透视 ESP"最本质的区别：ESP 通常故意关闭深度测试（NoDepth / SeeThrough）
 * 来实现穿墙可见，这里反其道而行——始终显式启用深度测试。
 */
public final class EntityOverlayRenderer {

    private EntityOverlayRenderer() {}

    public static void render(
            LivingEntity entity,
            MobData data,
            OverlayConfig cfg,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            float tickDelta
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // 距离裁剪：纯粹的性能/整洁度控制，不是可见性判定
        double distSq = mc.player.squaredDistanceTo(entity);
        if (distSq > cfg.renderDistance * cfg.renderDistance) return;

        matrices.push();

        // 定位到实体头顶上方，跟随实体身高动态偏移
        double height = entity.getHeight();
        matrices.translate(0.0, height + cfg.verticalOffset, 0.0);

        // Billboard：始终朝向摄像机，和原版生物名字牌一致的做法
        matrices.multiply(mc.gameRenderer.getCamera().getRotation());

        float scale = cfg.baseTextScale * 0.025f; // 与原版名字牌缩放量级保持一致
        if (cfg.distanceScaling) {
            float distFactor = (float) Math.max(0.6, Math.min(1.4, 12.0 / Math.sqrt(distSq)));
            scale *= distFactor;
        }
        matrices.scale(-scale, -scale, scale);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = mc.textRenderer;

        int rarityColor = cfg.rarityColors.getOrDefault(data.rarity(), 0xAAAAAA) | 0xFF000000;

        float cursorY = 0f;

        if (cfg.showLevelTag && data.hasLevel()) {
            String levelStr = "Lv" + data.level();
            drawDepthTestedText(textRenderer, matrices, vertexConsumers, levelStr, cursorY, rarityColor, light);
            cursorY -= 10f;
        }

        if (cfg.showHealthBar && data.hasHealth()) {
            drawHealthBar(vertexConsumers, positionMatrix, data, cursorY);
            cursorY -= 8f;

            String hpText = formatHp(data.currentHealth()) + " / " + formatHp(data.maxHealth());
            drawDepthTestedText(textRenderer, matrices, vertexConsumers, hpText, cursorY, 0xFFFFFFFF, light);
        }

        matrices.pop();
    }

    private static void drawDepthTestedText(
            TextRenderer textRenderer,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            String text,
            float y,
            int color,
            int light
    ) {
        float x = -textRenderer.getWidth(text) / 2f;
        // 关键参数：最后一个 boolean（seeThrough）传 false
        // -> 使用 TextLayerType.NORMAL（深度测试开启），文字会被墙体正常遮挡。
        // 若传 true 则会退化成"穿墙可见"的名字牌渲染模式，这里明确禁止。
        textRenderer.draw(
                text,
                x, y,
                color,
                false,                     // shadow
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0x00000000,                // 背景色（透明，不画名字牌底板）
                light
        );
    }

    private static void drawHealthBar(
            VertexConsumerProvider vertexConsumers,
            Matrix4f matrix,
            MobData data,
            float y
    ) {
        float width = 40f;
        float height = 3f;
        float fraction = data.healthFraction();

        // 自定义的、显式启用深度测试的简单 RenderLayer，
        // 用于绘制血条底色 + 填充色两个矩形。
        RenderLayer layer = OverlayRenderLayers.depthTestedQuads();
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);

        // 背景（深红/灰）
        addQuad(consumer, matrix, -width / 2f, y - height, width / 2f, y, 0x66000000);
        // 前景（按血量百分比填充，绿->红过渡）
        int fgColor = healthColor(fraction);
        addQuad(consumer, matrix, -width / 2f, y - height, -width / 2f + width * fraction, y, fgColor);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float x2, float y2, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        consumer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        consumer.vertex(matrix, x1, y2, 0).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        consumer.vertex(matrix, x2, y1, 0).color(r, g, b, a);
    }

    private static int healthColor(float fraction) {
        int r = (int) (255 * (1 - fraction));
        int g = (int) (255 * fraction);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private static String formatHp(float v) {
        if (v >= 1_000_000f) return String.format("%.1fm", v / 1_000_000f);
        if (v >= 1_000f) return String.format("%.1fk", v / 1_000f);
        return String.valueOf((int) v);
    }
}
