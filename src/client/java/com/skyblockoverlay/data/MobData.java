package com.skyblockoverlay.data;

/**
 * 从生物头顶名称标签解析出的展示用数据。
 * 任一字段解析失败时用 -1 / null 表示“未知”，渲染层需要自行判断是否跳过。
 */
public record MobData(
        String rawName,
        int level,          // -1 = 未解析到
        float currentHealth, // -1 = 未解析到
        float maxHealth,     // -1 = 未解析到
        String rarity        // "DEFAULT" 表示未匹配到任何已知稀有度关键字
) {
    public boolean hasHealth() {
        return currentHealth >= 0 && maxHealth > 0;
    }

    public boolean hasLevel() {
        return level >= 0;
    }

    public float healthFraction() {
        if (!hasHealth()) return 1.0f;
        return Math.max(0f, Math.min(1f, currentHealth / maxHealth));
    }
}
