package com.skyblockoverlay.data;

import com.skyblockoverlay.config.OverlayConfig;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把实体的自定义名称（Text.getString()）解析为 MobData。
 *
 * 重要限制：Hypixel Skyblock 服务端下发的名称标签文本格式会随更新调整，
 * 这里的正则只是一个可用的起点，实际格式请在游戏内 F3+抓取或用
 * /trigger 之类的方式核实后，在 GUI 的 "Parsing" 分类里修改两个正则。
 */
public final class MobDataParser {

    // 编译好的正则按配置内容缓存，避免每帧重新编译
    private static String cachedLevelRegex = "";
    private static String cachedHealthRegex = "";
    private static Pattern levelPattern;
    private static Pattern healthPattern;

    // 名称关键字 -> 稀有度桶。按需在此扩充，或改造成从 JSON 资源文件加载。
    private static final Map<String, String> RARITY_KEYWORDS = new LinkedHashMap<>();
    static {
        RARITY_KEYWORDS.put("Voidgloom Seraph", "MYTHIC");
        RARITY_KEYWORDS.put("Inferno Demonlord", "MYTHIC");
        RARITY_KEYWORDS.put("Tarantula Broodfather", "LEGENDARY");
        RARITY_KEYWORDS.put("Revenant Horror", "LEGENDARY");
        RARITY_KEYWORDS.put("Sven Packmaster", "LEGENDARY");
        // ... 按需继续添加已知 Boss / 稀有生物名称
    }

    private MobDataParser() {}

    public static MobData parse(Entity entity, OverlayConfig cfg) {
        Text nameText = entity.getCustomName();
        if (nameText == null) {
            nameText = entity.getName();
        }
        String raw = nameText.getString();

        ensurePatterns(cfg);

        int level = -1;
        Matcher lm = levelPattern.matcher(raw);
        if (lm.find()) {
            try {
                level = Integer.parseInt(lm.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        float current = -1, max = -1;
        Matcher hm = healthPattern.matcher(raw);
        if (hm.find() && hm.groupCount() >= 2) {
            current = parseShorthandNumber(hm.group(1));
            max = parseShorthandNumber(hm.group(2));
        }

        String rarity = "DEFAULT";
        for (Map.Entry<String, String> e : RARITY_KEYWORDS.entrySet()) {
            if (raw.contains(e.getKey())) {
                rarity = e.getValue();
                break;
            }
        }

        return new MobData(raw, level, current, max, rarity);
    }

    private static void ensurePatterns(OverlayConfig cfg) {
        if (!cfg.levelTagRegex.equals(cachedLevelRegex)) {
            cachedLevelRegex = cfg.levelTagRegex;
            levelPattern = Pattern.compile(cachedLevelRegex);
        }
        if (!cfg.healthTagRegex.equals(cachedHealthRegex)) {
            cachedHealthRegex = cfg.healthTagRegex;
            healthPattern = Pattern.compile(cachedHealthRegex);
        }
    }

    /** 把 "1.2k" / "3.4m" / "1,234" 这类简写数字转成 float。 */
    private static float parseShorthandNumber(String s) {
        if (s == null || s.isEmpty()) return -1;
        String cleaned = s.replace(",", "").trim().toLowerCase();
        try {
            if (cleaned.endsWith("k")) {
                return Float.parseFloat(cleaned.substring(0, cleaned.length() - 1)) * 1_000f;
            } else if (cleaned.endsWith("m")) {
                return Float.parseFloat(cleaned.substring(0, cleaned.length() - 1)) * 1_000_000f;
            }
            return Float.parseFloat(cleaned);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
