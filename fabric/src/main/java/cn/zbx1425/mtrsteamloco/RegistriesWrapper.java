package cn.zbx1425.mtrsteamloco;

// Объекты MTR 4
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mapping.registry.EntityTypeRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.holder.SoundEvent;

// Ванильные частицы (так как MTR 4 их не оборачивает)
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;

public interface RegistriesWrapper {

    void registerBlock(String id, BlockRegistryObject block);

    // Заменили Wrapper на CreativeModeTabHolder
    void registerBlockAndItem(String id, BlockRegistryObject block, CreativeModeTabHolder tab);

    void registerItem(String id, ItemRegistryObject item);

    void registerBlockEntityType(String id, BlockEntityTypeRegistryObject blockEntityType);

    void registerEntityType(String id, EntityTypeRegistryObject entityType);

    void registerSoundEvent(String id, SoundEvent soundEvent);

    // Ванильные частицы
    void registerParticleType(String id, ParticleType<?> particleType);

    SimpleParticleType createParticleType(boolean overrideLimiter);
}