package com.skyblockoverlay.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * 粗略判断当前是否连接在 Hypixel（及 Skyblock 子模式）上，
 * 避免这个 mod 在其它服务器上也生效。
 *
 * 判断方式仅基于当前连接的服务器地址，不读取、不上传任何账号或
 * 服务器数据。真实项目里通常还会结合计分板标题 / Tab 列表标题
 * 二次确认是否处于 Skyblock 子服，这里只给出最基础的一层。
 */
public final class SkyblockServerDetector {

    private SkyblockServerDetector() {}

    public static boolean isLikelySkyblock() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return false;
        String addr = info.address.toLowerCase();
        return addr.contains("hypixel.net");
    }
}
