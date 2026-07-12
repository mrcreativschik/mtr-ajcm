package cn.zbx1425.mtrsteamloco.mixin;

import org.mtr.core.generated.data.RailSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RailSchema.class, remap = false)
public interface RailSchemaAccessor {
    @Accessor("verticalRadius")
    double getVerticalRadius();
}