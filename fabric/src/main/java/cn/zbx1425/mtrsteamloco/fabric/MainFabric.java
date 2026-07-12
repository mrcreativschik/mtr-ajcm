package cn.zbx1425.mtrsteamloco.fabric;

import cn.zbx1425.mtrsteamloco.Main;
import net.fabricmc.api.ModInitializer;

public class MainFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		// Запускаем общую инициализацию мода, передавая обертку реестров Fabric
		Main.init(new RegistriesWrapperImpl());
	}
}