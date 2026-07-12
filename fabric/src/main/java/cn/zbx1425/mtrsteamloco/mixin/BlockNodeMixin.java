package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import org.mtr.mod.block.BlockNode;
import org.mtr.core.data.TransportMode;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mod.block.IBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockNode.class)
public abstract class BlockNodeMixin {

    @Unique
    private boolean mtrSteamLoco$isInvisibleMode;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ctorTail(TransportMode transportMode, CallbackInfo ci) {
        // Запоминаем, должен ли этот узел быть невидимым
        this.mtrSteamLoco$isInvisibleMode = (transportMode == TransportMode.TRAIN || transportMode == TransportMode.AIRPLANE);
    }

    // Внедряемся в метод getOutlineShape2, который ТОЧНО есть в декомпиляте и использует MTR-овский BlockState!
    @Inject(method = "getOutlineShape2", at = @At("HEAD"), cancellable = true)
    private void onGetOutlineShape2(BlockState blockState, org.mtr.mapping.holder.BlockView world, org.mtr.mapping.holder.BlockPos pos, org.mtr.mapping.holder.ShapeContext context, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<org.mtr.mapping.holder.VoxelShape> cir) {
        // Проверяем условия скрытия 3D рельса
        if (ClientConfig.enableRail3D && mtrSteamLoco$isInvisibleMode && IBlock.getStatePropertySafe(blockState, BlockNode.IS_CONNECTED)) {
            // Если мы хотим, чтобы блок вообще исчез и не рендерился/не выбирался, возвращаем пустую форму
            cir.setReturnValue(org.mtr.mapping.holder.VoxelShapes.empty());
        }
    }
}