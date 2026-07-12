package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PacketScreen {

    public static ResourceLocation PACKET_SHOW_SCREEN = new ResourceLocation(Main.MOD_ID, "show_screen");

    public static void sendScreenBlockS2C(ServerPlayer player, String screenName, BlockPos pos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUtf(screenName);
        packet.writeBlockPos(pos);

        try {
            Class<?> registryClass = Class.forName("org.mtr.mod.Registry");
            Method sendToPlayer = registryClass.getMethod("sendToPlayer",
                    Class.forName("net.minecraft.server.level.ServerPlayer"),
                    ResourceLocation.class,
                    FriendlyByteBuf.class);
            sendToPlayer.invoke(null, player, PACKET_SHOW_SCREEN, packet);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to send screen packet via reflection:", e);
        }
    }

    public static void receiveScreenS2C(FriendlyByteBuf packet) {
        Minecraft minecraftClient = Minecraft.getInstance();
        String screenName = packet.readUtf();
        BlockPos pos = packet.readBlockPos();

        minecraftClient.execute(() -> {
            Object screenInstance = null;
            try {
                // Загружаем классы экранов по имени. Если они временно сломаны, PacketScreen всё равно скомпилируется!
                if ("eye_candy".equals(screenName)) {
                    Class<?> clazz = Class.forName("cn.zbx1425.mtrsteamloco.gui.EyeCandyScreen");
                    screenInstance = clazz.getConstructor(BlockPos.class).newInstance(pos);
                } else if ("brush_edit_rail".equals(screenName)) {
                    Class<?> clazz = Class.forName("cn.zbx1425.mtrsteamloco.gui.BrushEditRailScreen");
                    screenInstance = clazz.getDeclaredConstructor().newInstance();
                }

                if (screenInstance != null) {
                    openScreenReflectively(minecraftClient, screenInstance);
                }
            } catch (Exception e) {
                Main.LOGGER.error("Failed to instantiate screen: " + screenName, e);
            }
        });
    }

    // Всеядный метод для открытия любых экранов (ванильных и MTR 4)
    private static void openScreenReflectively(Minecraft mc, Object screenObj) {
        // 1. Если это чистый ванильный Screen
        if (screenObj instanceof net.minecraft.client.gui.screens.Screen) {
            mc.setScreen((net.minecraft.client.gui.screens.Screen) screenObj);
            return;
        }

        // 2. Если это маппинг MTR 4 (вытаскиваем встроенный ванильный Screen из поля 'data')
        try {
            Field dataField = screenObj.getClass().getField("data");
            dataField.setAccessible(true);
            Object vanillaScreen = dataField.get(screenObj);
            if (vanillaScreen instanceof net.minecraft.client.gui.screens.Screen) {
                mc.setScreen((net.minecraft.client.gui.screens.Screen) vanillaScreen);
                return;
            }
        } catch (Exception ignored) {}

        // 3. Ультимативный вариант через MTR 4 Minecraft-холдер
        try {
            Class<?> mtrMinecraftClass = Class.forName("org.mtr.mapping.holder.Minecraft");
            Object mtrMc = mtrMinecraftClass.getMethod("getInstance").invoke(null);

            Class<?> mtrScreenClass = Class.forName("org.mtr.mapping.holder.Screen");
            Object mtrScreen = mtrScreenClass.getConstructor(screenObj.getClass().getSuperclass()).newInstance(screenObj);

            Method setScreen = mtrMc.getClass().getMethod("setScreen", mtrScreenClass);
            setScreen.invoke(mtrMc, mtrScreen);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to open MTR screen reflectively:", e);
        }
    }
}