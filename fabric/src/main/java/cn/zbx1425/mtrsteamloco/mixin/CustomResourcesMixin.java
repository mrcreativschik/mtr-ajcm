package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.CustomResources; // Оставляем только этот импорт!
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import cn.zbx1425.sowcer.ContextCapability;
import cn.zbx1425.sowcer.util.GlStateTracker;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.mod.resource.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(org.mtr.mod.resource.CustomResources.class) // Полный путь переносим сюда
public class CustomResourcesMixin {

    // Внедряемся в главный конструктор десериализации ресурсов MTR 4
    @Inject(at = @At("TAIL"), method = "<init>(Lorg/mtr/core/serializer/ReaderBase;Lorg/mtr/mod/resource/ResourceProvider;)V", remap = false)
    private void onConstructorTail(ReaderBase readerBase, ResourceProvider resourceProvider, CallbackInfo ci) {
        ContextCapability.checkContextVersion();
        String glVersionStr = "OpenGL " + ContextCapability.contextVersion / 10 + "."
                + ContextCapability.contextVersion % 10;
        Main.LOGGER.info("NTE detected " + glVersionStr + (ContextCapability.isGL4ES ? " (GL4ES)." : "."));

        GlStateTracker.capture();
        MtrModelRegistryUtil.loadingErrorList.clear();

        // Передаем провайдер ресурсов для NTE
        if (resourceProvider != null) {
            // Если в MtrModelRegistryUtil или вашем CustomResources всё ещё требуется ванильный ResourceManager,
            // мы можем получить его через обёртки Fabric/Minecraft, не ломая типы маппингов
            net.minecraft.server.packs.resources.ResourceManager vanillaManager =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager();

            MtrModelRegistryUtil.resourceManager = vanillaManager;
            cn.zbx1425.mtrsteamloco.CustomResources.reset(vanillaManager);
        }

        // Запускаем обработку кастомных компонентов моделей NTE
        cn.zbx1425.mtrsteamloco.CustomResources.resetComponents();

        GlStateTracker.restore();
        Main.LOGGER.info("MTR-NTE has successfully hooked into MTR 4 Resource Loading.");
    }
}