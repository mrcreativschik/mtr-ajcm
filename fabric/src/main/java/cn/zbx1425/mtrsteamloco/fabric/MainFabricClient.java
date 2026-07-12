package cn.zbx1425.mtrsteamloco.fabric;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.gui.ConfigScreen;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MainFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		// Чистая регистрация команд без скриптового мусора
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommandManager.literal("mtrnte")
							.then(ClientCommandManager.literal("config")
									.executes(context -> {
										Minecraft.getInstance().tell(() -> {
											Minecraft.getInstance().setScreen(ConfigScreen.createScreen(Minecraft.getInstance().screen));
										});
										return 1;
									}))
							.then(ClientCommandManager.literal("hideriding")
									.executes(context -> {
										ClientConfig.hideRidingTrain = !ClientConfig.hideRidingTrain;
										return 1;
									}))
							.then(ClientCommandManager.literal("stat")
									.executes(context -> {
										Minecraft.getInstance().tell(() -> {
											String info = RenderUtil.getRenderStatusMessage();
											if (Minecraft.getInstance().player != null) {
												Minecraft.getInstance().player.sendSystemMessage(Component.literal(info));
											}
										});
										return 1;
									}))
			);
		});

		// Инициализируем чистый MainClient
		MainClient.init();
	}
}