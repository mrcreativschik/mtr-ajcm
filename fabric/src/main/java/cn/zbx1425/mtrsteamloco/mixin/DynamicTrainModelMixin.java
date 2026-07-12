package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.integration.DynamicTrainModelLoader;
import com.google.gson.JsonObject;
import org.mtr.core.serializer.ReaderBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(org.mtr.mod.resource.VehicleResource.class)
public class VehicleResourceMixin {

    @Inject(method = "<init>*", at = @At("TAIL"), remap = false)
    private void ctorTail(ReaderBase readerBase, CallbackInfo ci) {
        JsonObject dummyJson = new JsonObject();

        // Передаем "this" напрямую как Object.
        // Миксин скомпилируется без единой ошибки, так как Object есть всегда!
        DynamicTrainModelLoader.loadInto(dummyJson, (Object) this);
    }
}