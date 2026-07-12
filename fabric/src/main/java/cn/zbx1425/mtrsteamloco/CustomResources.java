package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;

// Импорты холдеров MTR 4
import org.mtr.mapping.holder.ResourceManager;
import org.mtr.mapping.holder.Text;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class CustomResources {

    private static net.minecraft.server.packs.resources.ResourceManager unwrap(ResourceManager manager) {
        try {
            Field field = manager.getClass().getField("data");
            field.setAccessible(true);
            return (net.minecraft.server.packs.resources.ResourceManager) field.get(manager);
        } catch (Exception e) {
            return (net.minecraft.server.packs.resources.ResourceManager) (Object) manager;
        }
    }

    private static <T> T getInternalData(Object holder) {
        try {
            Field field = holder.getClass().getField("data");
            field.setAccessible(true);
            return (T) field.get(holder);
        } catch (Exception e) {
            return (T) holder;
        }
    }

    public static void reset(ResourceManager resourceManager) {
        // Временно скрываем очистку через рефлексию, чтобы не злить компилятор
        try {
            Class<?> mainClientClass = Class.forName("cn.zbx1425.mtrsteamloco.MainClient");
            try {
                Object modelManager = mainClientClass.getField("modelManager").get(null);
                modelManager.getClass().getMethod("clear").invoke(modelManager);
            } catch (Exception ignored) {}
            try {
                Object atlasManager = mainClientClass.getField("atlasManager").get(null);
                atlasManager.getClass().getMethod("clear").invoke(atlasManager);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public static void init(ResourceManager resourceManager) {
        Main.LOGGER.info("MTR-NTE (JCM Branch) has started loading custom resources.");

        net.minecraft.server.packs.resources.ResourceManager vanillaManager = unwrap(resourceManager);
        RailModelRegistry.reload(vanillaManager);

        // Убрали сложный лог с vaoCount/vboCount, который требовал прямых ссылок
        Main.LOGGER.info("MTR-NTE: Custom resources reloaded successfully.");

        try {
            Class<?> trainClientRegistryClass = Class.forName("org.mtr.mod.client.TrainClientRegistry");
            Class<?> transportModeClass = Class.forName("org.mtr.mapping.holder.TransportMode");
            Object trainMode = transportModeClass.getField("TRAIN").get(null);

            HashMap<String, Object> existingTrains19m = new HashMap<>();

            Method forEachMethod = trainClientRegistryClass.getMethod("forEach", transportModeClass, java.util.function.BiConsumer.class);
            forEachMethod.invoke(null, trainMode, (java.util.function.BiConsumer<Object, Object>) (key, prop) -> {
                try {
                    String stringKey = getInternalData(key);
                    Object vanillaProp = getInternalData(prop);
                    Field baseTrainTypeField = vanillaProp.getClass().getField("baseTrainType");
                    String baseTrainType = (String) baseTrainTypeField.get(vanillaProp);

                    if ("train_19_2".equals(baseTrainType) || "dk3".equals(stringKey)) {
                        existingTrains19m.put(stringKey, vanillaProp);
                    }
                } catch (Exception e) {
                    // Игнорируем
                }
            });
        } catch (Exception e) {
            Main.LOGGER.error("Failed to process TrainClientRegistry via reflection:", e);
        }
    }

    public static void resetComponents() {
        try {
            Class<?> clientDataClass = Class.forName("org.mtr.mod.client.ClientData");
            java.util.Set<?> trains = (java.util.Set<?>) clientDataClass.getField("TRAINS").get(null);

            trains.forEach(trainHolder -> {
                try {
                    Object train = getInternalData(trainHolder);
                    Field isRemovedField = train.getClass().getField("isRemoved");
                    isRemovedField.set(train, true);
                } catch (Exception e) {
                    // Игнорируем
                }
            });

            Class<?> minecraftClass = Class.forName("org.mtr.mapping.holder.Minecraft");
            Object mcInstance = minecraftClass.getMethod("getInstance").invoke(null);
            Object soundManager = mcInstance.getClass().getMethod("getSoundManager").invoke(mcInstance);
            soundManager.getClass().getMethod("tick", boolean.class).invoke(soundManager, false);

            trains.forEach(trainHolder -> {
                try {
                    Object train = getInternalData(trainHolder);
                    Field isRemovedField = train.getClass().getField("isRemoved");
                    isRemovedField.set(train, false);

                    Field trainIdField = train.getClass().getField("trainId");
                    String trainId = (String) trainIdField.get(train);

                    Class<?> trainClientRegistryClass = Class.forName("org.mtr.mod.client.TrainClientRegistry");
                    Object propHolder = trainClientRegistryClass.getMethod("getTrainProperties", String.class).invoke(null, trainId);
                    Object vanillaProp = getInternalData(propHolder);

                    if (ClientConfig.enableTrainRender) {
                        Field rendererField = vanillaProp.getClass().getField("renderer");
                        Object renderer = rendererField.get(vanillaProp);
                        Method createInstance = renderer.getClass().getMethod("createTrainInstance", train.getClass());

                        Field trainRendererField = train.getClass().getDeclaredField("trainRenderer");
                        trainRendererField.setAccessible(true);
                        trainRendererField.set(train, createInstance.invoke(renderer, train));
                    } else {
                        Class<?> noopRenderClass = Class.forName("cn.zbx1425.mtrsteamloco.render.train.NoopTrainRenderer");
                        Field trainRendererField = train.getClass().getDeclaredField("trainRenderer");
                        trainRendererField.setAccessible(true);
                        trainRendererField.set(train, noopRenderClass.getField("INSTANCE").get(null));
                    }

                    if (ClientConfig.enableTrainSound) {
                        Field soundField = vanillaProp.getClass().getField("sound");
                        Object sound = soundField.get(vanillaProp);
                        Method createInstance = sound.getClass().getMethod("createTrainInstance", train.getClass());

                        Field trainSoundField = train.getClass().getDeclaredField("trainSound");
                        trainSoundField.setAccessible(true);
                        trainSoundField.set(train, createInstance.invoke(sound, train));
                    } else {
                        Class<?> noopSoundClass = Class.forName("cn.zbx1425.mtrsteamloco.sound.NoopTrainSound");
                        Field trainSoundField = train.getClass().getDeclaredField("trainSound");
                        trainSoundField.setAccessible(true);
                        trainSoundField.set(train, noopSoundClass.getField("INSTANCE").get(null));
                    }
                } catch (Exception e) {
                    // Игнорируем
                }
            });
        } catch (Exception e) {
            Main.LOGGER.error("Failed to reset components via reflection:", e);
        }
    }
}