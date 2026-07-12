package cn.zbx1425.mtrsteamloco.network;

import cn.zbx1425.mtrsteamloco.BuildConfig;
import cn.zbx1425.mtrsteamloco.Main;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketVersionCheck {

    public static final ResourceLocation PACKET_VERSION_CHECK = new ResourceLocation(Main.MOD_ID, "version_check");

    public static void sendVersionCheckS2C(ServerPlayer player) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUtf(BuildConfig.MOD_VERSION);
        packet.writeInt(BuildConfig.MOD_PROTOCOL_VERSION);

        // Используем современный Fabric API для отправки
        ServerPlayNetworking.send(player, PACKET_VERSION_CHECK, packet);
    }

    public static void receiveVersionCheckS2C(FriendlyByteBuf packet) {
        final String remoteVersion = packet.readUtf();
        final int remoteProtocolVersion = packet.readableBytes() >= 4 ? packet.readInt() : 0;

        boolean protocolMatches = (remoteProtocolVersion == BuildConfig.MOD_PROTOCOL_VERSION)
                || (BuildConfig.MOD_PROTOCOL_VERSION == 1 && remoteProtocolVersion == 0 &&
                remoteVersion.contains("-0.3."));

        Minecraft minecraftClient = Minecraft.getInstance();
        minecraftClient.execute(() -> {
            if (!protocolMatches) {
                final ClientPacketListener connection = minecraftClient.getConnection();
                if (connection != null) {
                    String serverVersion = remoteVersion + " (" + remoteProtocolVersion + ")";
                    String localVersion = BuildConfig.MOD_VERSION + " (" + BuildConfig.MOD_PROTOCOL_VERSION + ")";

                    // Используем нативный Component вместо mtr.mappings.Text
                    Component errorMsg = Component.literal("")
                            .append(Component.translatable("gui.mtr.mismatched_versions_your_version", localVersion))
                            .append("\n")
                            .append(Component.translatable("gui.mtr.mismatched_versions_server_version", serverVersion))
                            .append("\n\n")
                            .append(Component.translatable("gui.mtr.mismatched_versions").getString()
                                    .replace("Minecraft Transit Railway", "NTE (Nemo's Transit Expansion)"));

                    connection.getConnection().disconnect(errorMsg);
                }
            }
        });
    }
}