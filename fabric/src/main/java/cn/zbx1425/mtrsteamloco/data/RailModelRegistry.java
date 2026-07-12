package cn.zbx1425.mtrsteamloco.data;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RailModelRegistry {

    public static Map<String, RailModelProperties> elements = new HashMap<>();

    // Вместо ModelCluster используем стандартный ResourceLocation для ноды рельса
    public static ResourceLocation railNodeModelLocation;

    public static void register(String key, RailModelProperties properties) {
        elements.put(key, properties);
    }

    public static void reload(ResourceManager resourceManager) {
        elements.clear();

        // Используем Component вместо Text
        register("", new RailModelProperties(Component.translatable("rail.mtrsteamloco.default"), null, 1f, 0f));
        register("null", new RailModelProperties(Component.translatable("rail.mtrsteamloco.hidden"), null, Float.MAX_VALUE, 0f));

        railNodeModelLocation = new ResourceLocation("mtrsteamloco:models/rail_node.csv");

        List<Pair<ResourceLocation, Resource>> resources =
                MtrModelRegistryUtil.listResources(resourceManager, "mtrsteamloco", "rails", ".json");
        for (Pair<ResourceLocation, Resource> pair : resources) {
            try {
                // Заменили Utilities.getInputStream на pair.getSecond().open()
                try (InputStream is = pair.getSecond().open()) {
                    JsonObject rootObj = (new JsonParser()).parse(IOUtils.toString(is, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (rootObj.has("model")) {
                        String key = FilenameUtils.getBaseName(pair.getFirst().getPath());
                        register(key, loadFromJson(resourceManager, key, rootObj));
                    } else {
                        for (Map.Entry<String, JsonElement> entry : rootObj.entrySet()) {
                            JsonObject obj = entry.getValue().getAsJsonObject();
                            String key = entry.getKey().toLowerCase(Locale.ROOT);
                            register(key, loadFromJson(resourceManager, key, obj));
                        }
                    }
                }
            } catch (Exception ex) {
                Main.LOGGER.error("Failed loading rail: " + pair.getFirst().toString(), ex);
                MtrModelRegistryUtil.recordLoadingError("Failed loading Rail " + pair.getFirst().toString(), ex);
            }
        }

        if (MainClient.railRenderDispatcher != null) {
            MainClient.railRenderDispatcher.clearRail();
        }
    }

    // Заменили Text.literal на Component.literal
    private static final RailModelProperties EMPTY_PROPERTY = new RailModelProperties(
            Component.literal(""), null, 1f, 0f
    );

    public static RailModelProperties getProperty(String key) {
        return elements.getOrDefault(key, EMPTY_PROPERTY);
    }

    private static RailModelProperties loadFromJson(ResourceManager resourceManager, String key, JsonObject obj) throws IOException {
        // Вытаскиваем путь к модели текстом из JSON
        String modelPath = obj.has("model") ? obj.get("model").getAsString() : "";

        float repeatInterval = obj.has("repeatInterval") ? obj.get("repeatInterval").getAsFloat() : 0.5f;
        float yOffset = obj.has("yOffset") ? obj.get("yOffset").getAsFloat() : 0f;

        // Создаем свойства рельса, передавая строку пути напрямую
        return new RailModelProperties(Component.translatable(obj.get("name").getAsString()), modelPath, repeatInterval, yOffset);
    }
}