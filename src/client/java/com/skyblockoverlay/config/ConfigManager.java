package com.skyblockoverlay.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.Map;

/**
 * 用 YACL 的 ConfigClassHandler 包装 OverlayConfig：
 * - handler.instance() 拿到运行时配置对象（其它类读它做渲染判断）
 * - handler.load() / handler.save() 走 GSON 落盘到 config/skyblockoverlay.json
 * - createScreen() 构建游戏内 GUI
 */
public final class ConfigManager {

    public static final ConfigClassHandler<OverlayConfig> HANDLER =
            ConfigClassHandler.createBuilder(OverlayConfig.class)
                    .id(net.minecraft.util.Identifier.of("skyblockoverlay", "config"))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(net.fabricmc.loader.api.FabricLoader.getInstance()
                                    .getConfigDir().resolve("skyblockoverlay.json"))
                            .setJson5(true)
                            .build())
                    .build();

    private ConfigManager() {}

    public static void load() {
        HANDLER.load();
    }

    public static OverlayConfig get() {
        return HANDLER.instance();
    }

    public static Screen createScreen(Screen parent) {
        OverlayConfig cfg = HANDLER.instance();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Skyblock Mob Overlay"))
                .save(HANDLER::save);

        // ---------- 通用设置 ----------
        builder.category(ConfigCategory.createBuilder()
                .name(Text.literal("General"))
                .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("启用叠加层 (Master Toggle)"))
                        .binding(true, () -> cfg.masterEnabled, v -> cfg.masterEnabled = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("显示血量条"))
                        .binding(true, () -> cfg.showHealthBar, v -> cfg.showHealthBar = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("显示等级标签"))
                        .binding(true, () -> cfg.showLevelTag, v -> cfg.showLevelTag = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("显示稀有度着色"))
                        .binding(true, () -> cfg.showRarityTag, v -> cfg.showRarityTag = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Text.literal("渲染距离 (格)"))
                        .description(OptionDescription.of(Text.literal(
                                "超过该距离不再绘制叠加层；仅是性能/整洁度控制，本身不提供任何穿墙可见性。")))
                        .binding(48.0, () -> cfg.renderDistance, v -> cfg.renderDistance = v)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(8.0, 128.0).step(1.0))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("随距离缩放文字"))
                        .binding(true, () -> cfg.distanceScaling, v -> cfg.distanceScaling = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .build());

        // ---------- 稀有度颜色 ----------
        ConfigCategory.Builder colorCategory = ConfigCategory.createBuilder()
                .name(Text.literal("Rarity Colors"));

        for (Map.Entry<String, Integer> entry : cfg.rarityColors.entrySet()) {
            String key = entry.getKey();
            colorCategory.option(Option.<Color>createBuilder()
                    .name(Text.literal(key))
                    .binding(
                            new Color(cfg.rarityColors.getOrDefault(key, 0xFFFFFF)),
                            () -> new Color(cfg.rarityColors.getOrDefault(key, 0xFFFFFF)),
                            v -> cfg.rarityColors.put(key, v.getRGB() & 0xFFFFFF)
                    )
                    .controller(ColorControllerBuilder::create)
                    .build());
        }
        builder.category(colorCategory.build());

        // ---------- 名称标签解析 ----------
        builder.category(ConfigCategory.createBuilder()
                .name(Text.literal("Parsing"))
                .option(Option.<String>createBuilder()
                        .name(Text.literal("等级标签正则"))
                        .description(OptionDescription.of(Text.literal(
                                "用于从生物头顶名称中提取等级，需含一个捕获组。服务器文本格式变化时在此调整。")))
                        .binding(cfg.levelTagRegex, () -> cfg.levelTagRegex, v -> cfg.levelTagRegex = v)
                        .controller(StringControllerBuilder::create)
                        .build())
                .option(Option.<String>createBuilder()
                        .name(Text.literal("血量标签正则"))
                        .binding(cfg.healthTagRegex, () -> cfg.healthTagRegex, v -> cfg.healthTagRegex = v)
                        .controller(StringControllerBuilder::create)
                        .build())
                .build());

        return builder.build().generateScreen(parent);
    }
}
