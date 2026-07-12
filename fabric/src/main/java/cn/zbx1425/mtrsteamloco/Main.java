package cn.zbx1425.mtrsteamloco;

import com.google.gson.JsonParser;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Main {

	public static final String MOD_ID = "mtrsteamloco";

	public static final Logger LOGGER = LoggerFactory.getLogger("MTR-NTE");
	public static final JsonParser JSON_PARSER = new JsonParser();

	public static final boolean enableRegistry;
	static {
		boolean enableRegistry1;
		try {
			String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI().getPath().toLowerCase(Locale.ROOT);
			enableRegistry1 = !jarPath.endsWith("-client.jar");
		} catch (URISyntaxException ignored) {
			enableRegistry1 = true;
		}
		enableRegistry = enableRegistry1;
	}

	public static Object BLOCK_DEPARTURE_BELL;
	public static Object BLOCK_EYE_CANDY;
	public static Object BLOCK_ENTITY_TYPE_EYE_CANDY;
	public static Object BRIDGE_CREATOR_1;
	public static Object SOUND_EVENT_BELL;
	public static SimpleParticleType PARTICLE_STEAM_SMOKE;

	public static void init(RegistriesWrapper registries) {
		LOGGER.info("MTR-NTE " + BuildConfig.MOD_VERSION + " built at "
				+ DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()).format(BuildConfig.BUILD_TIME));

		if (enableRegistry) {
			try {
				// Динамически загружаем блоки, чтобы компилятор не ругался на импорты
				Class<?> bellClass = Class.forName("cn.zbx1425.mtrsteamloco.block.BlockDepartureBell");
				BLOCK_DEPARTURE_BELL = bellClass.getDeclaredConstructor().newInstance();

				Class<?> candyClass = Class.forName("cn.zbx1425.mtrsteamloco.block.BlockEyeCandy");
				BLOCK_EYE_CANDY = candyClass.getDeclaredConstructor().newInstance();

				// Достаем креативные табы MTR 4
				Class<?> tabsClass = Class.forName("org.mtr.mod.CreativeModeTabs");
				Object railwayFacilitiesTab = tabsClass.getField("RAILWAY_FACILITIES").get(null);
				Object stationBuildingTab = tabsClass.getField("STATION_BUILDING_BLOCKS").get(null);

				// Оборачиваем блоки в холдеры через рефлексию (обходит баг с class_3414 и маппингами)
				Class<?> holderBlockClass = Class.forName("org.mtr.mapping.holder.Block");
				Object bellHolder = holderBlockClass.getConstructors()[0].newInstance(BLOCK_DEPARTURE_BELL);
				Object candyHolder = holderBlockClass.getConstructors()[0].newInstance(BLOCK_EYE_CANDY);

				// Вызываем регистрацию динамически
				java.lang.reflect.Method registerBlockAndItem = registries.getClass().getMethod("registerBlockAndItem", String.class, Object.class, Object.class);
				registerBlockAndItem.invoke(registries, "departure_bell", bellHolder, railwayFacilitiesTab);
				registerBlockAndItem.invoke(registries, "eye_candy", candyHolder, stationBuildingTab);

				// То же самое со звуком (прячем от компилятора)
				net.minecraft.sounds.SoundEvent vanillaSound = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "bell"));
				Class<?> holderSoundClass = Class.forName("org.mtr.mapping.holder.SoundEvent");
				SOUND_EVENT_BELL = holderSoundClass.getConstructors()[0].newInstance(vanillaSound);

				java.lang.reflect.Method registerSoundEvent = registries.getClass().getMethod("registerSoundEvent", String.class, Object.class);
				registerSoundEvent.invoke(registries, "bell", SOUND_EVENT_BELL);

				PARTICLE_STEAM_SMOKE = registries.createParticleType(true);
				registries.registerParticleType("steam_smoke", PARTICLE_STEAM_SMOKE);

			} catch (Exception e) {
				LOGGER.error("Failed to initialize blocks or sounds:", e);
			}

			// Регистрация сети без жестких лямбд
			try {
				Class<?> registryClass = Class.forName("org.mtr.mod.Registry");
				java.lang.reflect.Method registerReceiver = null;
				java.lang.reflect.Method registerJoin = null;

				for (java.lang.reflect.Method m : registryClass.getMethods()) {
					if (m.getName().equals("registerNetworkReceiver")) registerReceiver = m;
					else if (m.getName().equals("registerPlayerJoinEvent")) registerJoin = m;
				}

				if (registerReceiver != null) {
					Class<?> networkReceiverClass = registerReceiver.getParameterTypes()[1];

					registerReceiver.invoke(null, new ResourceLocation(MOD_ID, "update_block_entity"), createNetworkProxy(networkReceiverClass, "cn.zbx1425.mtrsteamloco.network.PacketUpdateBlockEntity"));
					registerReceiver.invoke(null, new ResourceLocation(MOD_ID, "update_rail"), createNetworkProxy(networkReceiverClass, "cn.zbx1425.mtrsteamloco.network.PacketUpdateRail"));
					registerReceiver.invoke(null, new ResourceLocation(MOD_ID, "update_holding_item"), createNetworkProxy(networkReceiverClass, "cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem"));
				}

				if (registerJoin != null) {
					Class<?> joinEventConsumerClass = registerJoin.getParameterTypes()[0];
					Object playerJoinProxy = java.lang.reflect.Proxy.newProxyInstance(
							joinEventConsumerClass.getClassLoader(),
							new Class<?>[]{joinEventConsumerClass},
							(proxy, method, args) -> {
								try {
									Class<?> packetClass = Class.forName("cn.zbx1425.mtrsteamloco.network.PacketVersionCheck");
									for (java.lang.reflect.Method m : packetClass.getMethods()) {
										if (m.getName().equals("sendVersionCheckS2C")) {
											m.invoke(null, unwrapArgs(args));
											break;
										}
									}
								} catch (Exception ignored) {}
								return null;
							}
					);
					registerJoin.invoke(null, playerJoinProxy);
				}

			} catch (Exception e) {
				LOGGER.error("Failed to register MTR 4 Network Receivers via proxy reflection:", e);
			}
		}
	}

	// Утилита для создания прокси-слушателей пакетов
	private static Object createNetworkProxy(Class<?> networkReceiverClass, String targetClassName) {
		return java.lang.reflect.Proxy.newProxyInstance(
				networkReceiverClass.getClassLoader(),
				new Class<?>[]{networkReceiverClass},
				(proxy, method, args) -> {
					try {
						Class<?> targetClass = Class.forName(targetClassName);
						for (java.lang.reflect.Method m : targetClass.getMethods()) {
							if (m.getName().equals("receiveUpdateC2S")) {
								m.invoke(null, unwrapArgs(args));
								break;
							}
						}
					} catch (Exception ignored) {}
					return null;
				}
		);
	}

	// Утилита для распаковки MTR 4 холдеров обратно в ванильные объекты для NTE-пакетов
	private static Object[] unwrapArgs(Object[] args) {
		if (args == null) return null;
		Object[] unwrappedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			try {
				java.lang.reflect.Field dataField = args[i].getClass().getField("data");
				unwrappedArgs[i] = dataField.get(args[i]);
			} catch (Exception e) {
				unwrappedArgs[i] = args[i];
			}
		}
		return unwrappedArgs;
	}
}