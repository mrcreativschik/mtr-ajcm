package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.Map;

public class PacketUpdateRail {

    public static final ResourceLocation PACKET_UPDATE_RAIL = new ResourceLocation(Main.MOD_ID, "update_rail");

    public static void sendUpdateC2S(Object railState, BlockPos posStart, BlockPos posEnd) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeResourceKey(Minecraft.getInstance().level.dimension());
        packet.writeBlockPos(posStart);
        packet.writeBlockPos(posEnd);
        try {
            railState.getClass().getMethod("writePacket", FriendlyByteBuf.class).invoke(railState, packet);
            ClientPlayNetworking.send(PACKET_UPDATE_RAIL, packet);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to send rail update:", e);
        }
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        ResourceKey<Level> levelKey = packet.readResourceKey(Registries.DIMENSION);
        BlockPos posStart = packet.readBlockPos();
        BlockPos posEnd = packet.readBlockPos();

        // Читаем Rail через рефлексию конструктора
        Object extraTarget;
        try {
            Class<?> railClass = Class.forName("mtr.data.Rail");
            extraTarget = railClass.getConstructor(FriendlyByteBuf.class).newInstance(packet);
        } catch (Exception e) { return; }

        server.execute(() -> {
            ServerLevel level = server.getLevel(levelKey);
            if (level == null) return;
            try {
                Object railwayData = Class.forName("mtr.data.RailwayData").getMethod("getInstance", Level.class).invoke(null, level);
                Map<?, ?> rails = (Map<?, ?>) Class.forName("cn.zbx1425.mtrsteamloco.mixin.RailwayDataAccessor").getMethod("getRails").invoke(railwayData);

                Object railForward = ((Map<?, ?>) rails.get(posStart)).get(posEnd);
                Object railBackward = ((Map<?, ?>) rails.get(posEnd)).get(posStart);

                // Используем рефлексию для установки значений RailExtraSupplier
                updateRailExtra(railForward, extraTarget);
                updateRailExtra(railBackward, extraTarget);

                final FriendlyByteBuf outboundPacket = new FriendlyByteBuf(Unpooled.buffer());
                // Рефлексивно пишем пакет
                railForward.getClass().getMethod("writePacket", FriendlyByteBuf.class).invoke(railForward, outboundPacket);
                railBackward.getClass().getMethod("writePacket", FriendlyByteBuf.class).invoke(railBackward, outboundPacket);

                ResourceLocation packetId = (ResourceLocation) Class.forName("mtr.packet.IPacket").getField("PACKET_CREATE_RAIL").get(null);
                for (ServerPlayer levelPlayer : level.players()) {
                    ServerPlayNetworking.send(levelPlayer, packetId, outboundPacket);
                }
            } catch (Exception e) {
                Main.LOGGER.error("Failed to update rail data:", e);
            }
        });
    }

    private static void updateRailExtra(Object rail, Object target) throws Exception {
        Method setterKey = rail.getClass().getMethod("setModelKey", String.class);
        setterKey.invoke(rail, target.getClass().getMethod("getModelKey").invoke(target));
        // Добавь остальные сеттеры по аналогии, если они есть в RailExtraSupplier
    }
}