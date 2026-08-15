package com.skyblockoverlay.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * MC 26.1 起底层渲染管线（Blaze3D pipeline）相较旧版本有较大改动，
 * 具体的 RenderPhase / RenderPipeline 构造方式请对照 Loom 生成的
 * genSources（Mojang 官方映射，无需再反混淆）核实实际类名和参数。
 * 下面给出的是符合旧版 1.21.x 系 RenderLayer.of(...) 语义的写法，
 * 移植到 26.1 时通常只需要把等价的 RenderPhase 常量替换掉。
 *
 * 关键约束只有一条：不要使用 disableDepthTest() / 任何 "SEE_THROUGH" /
 * "NO_DEPTH" 系列 phase —— 一旦引入，这个 RenderLayer 画出来的东西
 * 就会变成能穿墙看到的 ESP 效果，与本 mod 的设计目标相悖。
 */
public final class OverlayRenderLayers {

    private static final RenderLayer DEPTH_TESTED_QUADS = RenderLayer.of(
            "skyblockoverlay_quads",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    // 显式启用深度测试 + 深度写入关闭（避免血条互相遮挡出现闪烁），
                    // 但仍然会被"地形/方块"的深度值正常挡住。
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private OverlayRenderLayers() {}

    public static RenderLayer depthTestedQuads() {
        return DEPTH_TESTED_QUADS;
    }
}
