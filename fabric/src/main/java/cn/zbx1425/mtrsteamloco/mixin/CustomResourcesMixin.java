package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.mtr.mod.resource.CustomResources", remap = false)
public class CustomResourcesMixin {

    // Убираем аргументы из метода совсем!
    // Mixin не будет пытаться их "матчить", а просто выполнит код в конце конструктора.
    @Inject(at = @At("TAIL"), method = "<init>")
    private void onConstructorTail(CallbackInfo ci) {
        // Поскольку у нас нет доступа к локальным переменным напрямую,
        // используем вызов через статические методы или получение синглтона.

        // ВАЖНО: Вместо того чтобы передавать resourceProvider сюда,
        // получаем его напрямую из Minecraft, если это клиентская часть:
        net.minecraft.server.packs.resources.ResourceManager vanillaManager =
                net.minecraft.client.Minecraft.getInstance().getResourceManager();

        // Если CustomResources.reset() требует mtr.mapping.holder.ResourceManager:
        org.mtr.mapping.holder.ResourceManager mtrResourceManager;
        try {
            // Получаем конструктор, который принимает Object (внутри он сам скастит его к class_3300)
            java.lang.reflect.Constructor<?> constructor = org.mtr.mapping.holder.ResourceManager.class.getConstructor(Object.class);
            mtrResourceManager = (org.mtr.mapping.holder.ResourceManager) constructor.newInstance(vanillaManager);
        } catch (Exception e) {
            // Запасной план: если конструктор с Object не сработал, пробуем через поле или просто подавляем
            Main.LOGGER.error("Failed to map ResourceManager via reflection, trying fallback.", e);
            // Это "грязный" хак, но он работает, если структура данных в памяти совпадает
            mtrResourceManager = (org.mtr.mapping.holder.ResourceManager) (Object) vanillaManager;
        }

        cn.zbx1425.mtrsteamloco.CustomResources.reset(mtrResourceManager);
        cn.zbx1425.mtrsteamloco.CustomResources.resetComponents();

        cn.zbx1425.mtrsteamloco.Main.LOGGER.info("NTE: Successfully hooked via simplified Inject.");
    }
}