package cn.zbx1425.mtrsteamloco.fabric;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.RegistriesWrapper;

// Импорты MTR 4
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mapping.registry.EntityTypeRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.holder.SoundEvent;

// Ванильные регистраторы Minecraft / Fabric
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;

import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public class RegistriesWrapperImpl implements RegistriesWrapper {

    // Достаем поле "data" через рефлексию, полностью скрывая типы от компилятора Loom
    private static <T> T getInternalData(Object holder) {
        try {
            Field field = holder.getClass().getField("data");
            field.setAccessible(true);
            return (T) field.get(holder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract vanilla object from MTR holder: " + holder, e);
        }
    }

    @Override
    public void registerBlock(String id, BlockRegistryObject block) {
        Block vanillaBlock = getInternalData(block.get());
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Main.MOD_ID, id), vanillaBlock);
    }

    @Override
    public void registerBlockAndItem(String id, BlockRegistryObject block, CreativeModeTabHolder tab) {
        Block vanillaBlock = getInternalData(block.get());
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Main.MOD_ID, id), vanillaBlock);

        BlockItem blockItem = new BlockItem(vanillaBlock, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Main.MOD_ID, id), blockItem);
    }

    @Override
    public void registerItem(String id, ItemRegistryObject item) {
        Item vanillaItem = getInternalData(item.get());
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Main.MOD_ID, id), vanillaItem);
    }

    @Override
    public void registerBlockEntityType(String id, BlockEntityTypeRegistryObject blockEntityType) {
        BlockEntityType<?> vanillaType = getInternalData(blockEntityType.get());
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(Main.MOD_ID, id), vanillaType);
    }

    @Override
    public void registerEntityType(String id, EntityTypeRegistryObject entityType) {
        EntityType<?> vanillaType = getInternalData(entityType.get());
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(Main.MOD_ID, id), vanillaType);
    }

    @Override
    public void registerSoundEvent(String id, SoundEvent soundEvent) {
        net.minecraft.sounds.SoundEvent vanillaSound = getInternalData(soundEvent);
        Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(Main.MOD_ID, id), vanillaSound);
    }

    @Override
    public void registerParticleType(String id, ParticleType<?> particleType) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation(Main.MOD_ID, id), particleType);
    }

    @Override
    public SimpleParticleType createParticleType(boolean overrideLimiter) {
        return FabricParticleTypes.simple(overrideLimiter);
    }
}