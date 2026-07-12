package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PacketUpdateHoldingItem {

    public static final ResourceLocation PACKET_UPDATE_HOLDING_ITEM = new ResourceLocation(Main.MOD_ID, "update_holding_item");

    public static void sendUpdateC2S() {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        CompoundTag itemTag = new CompoundTag();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getMainHandItem().save(itemTag);
            packet.writeNbt(itemTag);
            // Используем прямой вызов Fabric API вместо старого RegistryClient
            ClientPlayNetworking.send(PACKET_UPDATE_HOLDING_ITEM, packet);
        }
    }

    public static void receiveUpdateC2S(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        CompoundTag itemTag = packet.readNbt();
        if (itemTag != null) {
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.of(itemTag));
        }
    }
}