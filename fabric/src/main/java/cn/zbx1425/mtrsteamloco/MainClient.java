package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;

public class MainClient {

	// Оставляем диспетчер рендера рельсов, так как мы его переписали под MTR 4 Core
	public static RailRenderDispatcher railRenderDispatcher = new RailRenderDispatcher();

	public static void init() {
		// Загружаем клиентский конфиг мода
		ClientConfig.load();

		// Инициализируем шейдерный хендлер (если он адаптирован под Vanilla/Iris рендер)
		try {
			cn.zbx1425.mtrsteamloco.render.ShadersModHandler.init();
		} catch (Throwable ignored) {}

		// В MTR 4 кастомные ресурсы и эвенты входа регистрируются через стандартный Fabric API
		// или через встроенный Init Client в JCM.
		// Сетевые пакеты (Packets) теперь регистрируются на стороне платформ-специфичных классов.
	}

	public static void incrementGameTick() {
		// Пустой метод на случай, если его кто-то ищет из других модулей,
		// так как логику тиков мы убрали
	}
}