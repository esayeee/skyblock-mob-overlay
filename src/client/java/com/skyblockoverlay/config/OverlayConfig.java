package com.skyblockoverlay.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 纯数据类，被 YACL 的 ConfigClassHandler 通过反射读写并序列化为 JSON。
 * 所有字段都必须是可被 Gson 处理的类型（基本类型 / 枚举 / 简单集合）。
 */
public class OverlayConfig {

    // ------- 总开关 -------
    public boolean masterEnabled = true;

    // ------- 显示项开关 -------
    public boolean showHealthBar = true;
    public boolean showLevelTag = true;
    public boolean showRarityTag = true;

    // ------- 渲染范围（仅影响“开始渲染叠加层”的距离，不提供任何穿墙能力） -------
    public double renderDistance = 48.0;

    // 叠加层整体的额外高度偏移（在实体眼睛高度之上多少格）
    public double verticalOffset = 0.35;

    // 文字/血条随距离缩放，越远越小，避免糊成一片
    public boolean distanceScaling = true;
    public float baseTextScale = 1.0f;

    // ------- 按稀有度着色（血条颜色 + 名称颜色），可在 GUI 里逐个调 -------
    public Map<String, Integer> rarityColors = defaultRarityColors();

    // ------- 名称标签解析用的正则（服务器文本格式可能变化，暴露给玩家自行调整） -------
    // 默认假设格式类似："[Lv100] Zombie" 以及独立一行 "1.2k/1.2k❤"
    public String levelTagRegex = "\\[Lv(\\d+)]";
    public String healthTagRegex = "([\\d.,]+[km]?)\\s*/\\s*([\\d.,]+[km]?)\\s*\\u2764";

    // ------- 只显示指定生物（留空 = 不过滤，全部显示） -------
    public boolean useMobFilter = false;
    public java.util.Set<String> mobFilterList = new java.util.LinkedHashSet<>();

    private static Map<String, Integer> defaultRarityColors() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("COMMON", 0xFFFFFF);
        map.put("UNCOMMON", 0x55FF55);
        map.put("RARE", 0x5555FF);
        map.put("EPIC", 0xAA00AA);
        map.put("LEGENDARY", 0xFFAA00);
        map.put("MYTHIC", 0xFF55FF);
        map.put("BOSS", 0xFF5555);
        map.put("DEFAULT", 0xAAAAAA);
        return map;
    }
}
