package cn.zbx1425.mtrsteamloco.mixin;

import org.mtr.core.data.Data;
import org.mtr.core.data.Rail;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(value = Data.class, remap = false)
public interface DataAccessor {
    @Accessor("rails")
    ObjectArrayList<Rail> getRails();
}