package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1. Используем targets для обхода проблем с импортами mtr.*
@Mixin(targets = "mtr.render.JonModelTrainRenderer", remap = false)
public abstract class JonModelTrainRendererMixin {

    @Shadow(remap = false) protected abstract Object getTrain();

    // 2. Убираем 'method = "renderCar"', чтобы Mixin перестал ругаться на несуществующий метод.
    // Если метод в классе всё же есть, просто замени его имя обратно.
    // Если ошибки не исчезают, используй @At(value = "HEAD") без указания метода.
    @Inject(at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void onRender(CallbackInfo ci) {
        Object trainObj = getTrain();

        // 3. Используем рефлексию или прямой каст внутри,
        // но сначала убедимся, что это вообще TrainClient.
        if (trainObj != null && trainObj.getClass().getName().contains("TrainClient")) {
            // Приводим тип к TrainClient из mtr (если он найден),
            // либо через явный каст, если RenderUtil это позволяет.
            if (RenderUtil.shouldSkipRenderTrain(trainObj)) {
                ci.cancel();
            }
        }
    }
}