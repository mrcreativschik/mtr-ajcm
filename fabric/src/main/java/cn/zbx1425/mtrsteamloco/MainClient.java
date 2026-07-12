package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import cn.zbx1425.mtrsteamloco.render.DrawSchedulerPlaceholder; // <-- ДОБАВЬ ЭТОТ ИМПОРТ

public class MainClient {
	public static RailRenderDispatcher railRenderDispatcher = new RailRenderDispatcher();

	// ДОБАВЬ СЮДА:
	public static final DrawSchedulerPlaceholder drawScheduler = new DrawSchedulerPlaceholder();
	public static final Object drawContext = new Object();

	public static void init() {
		// ... твой код ...
	}
} // <--- ПРОВЕРЬ, ЧТО ЭТА СКОБКА ЗАКРЫВАЕТ КЛАСС!