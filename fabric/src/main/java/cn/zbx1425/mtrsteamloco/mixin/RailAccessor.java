package cn.zbx1425.mtrsteamloco.mixin;

import org.mtr.core.data.Rail;
import org.mtr.core.data.RailMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Rail.class, remap = false)
public interface RailAccessor {
    @Accessor("railMath")
    RailMath getRailMath();
}