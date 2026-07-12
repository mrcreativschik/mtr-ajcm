package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.MainClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 100)
public class LevelRendererMixin {

    @Shadow @Final private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=destroyProgress", ordinal = 0))
    private void afterBlockEntities(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Minecraft.getInstance().level.getProfiler().popPush("NTEBlockEntities");

        // ВАЖНО: Если BufferSourceProxy был частью Sowcer, его нужно заменить
        // на нативный MultiBufferSource.bufferSource() или твой новый движок отрисовки.
        // Пока оставляем как есть, но проверь, существует ли этот класс без Sowcer!
        var bufferSource = renderBuffers.bufferSource();
        MainClient.drawScheduler.commit(bufferSource, MainClient.drawContext);
        // vertexConsumersProxy.commit(); // Убери это, если прокси больше нет
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void renderLevelLast(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        ((cn.zbx1425.mtrsteamloco.render.DrawSchedulerPlaceholder) (Object) MainClient.drawScheduler).resetFrameProfiler();
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        for (int i = sectionZ - 1; i <= sectionZ + 1; ++i) {
            for (int j = sectionX - 1; j <= sectionX + 1; ++j) {
                MainClient.railRenderDispatcher.registerLightUpdate(j, sectionY - 1, sectionY + 1, i);
            }
        }
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
        MainClient.railRenderDispatcher.registerLightUpdate(sectionX, sectionY, sectionY, sectionZ);
    }

    @Inject(method = "setSectionDirty(III)V", at = @At("HEAD"))
    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        MainClient.railRenderDispatcher.registerLightUpdate(sectionX, sectionY, sectionY, sectionZ);
    }
}