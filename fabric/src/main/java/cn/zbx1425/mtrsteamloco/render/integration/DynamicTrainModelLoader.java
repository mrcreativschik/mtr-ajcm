package cn.zbx1425.mtrsteamloco.render.integration;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.CustomResources;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.mixin.ModelMapperAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mod.resource.VehicleResource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class DynamicTrainModelLoader {

    private static Map<String, Object> cachedModels;
    private static String cachedPath;
    private static long cachedPathMtime = 0;

    // Меням сигнатуру метода: теперь принимаем Object (который является VehicleResource)
    public static void loadObjInto(JsonObject model, Object vehicleResourceObj) {
        // 1. Приводим объект к нужному типу один раз в начале
        if (!(vehicleResourceObj instanceof DynamicTrainModelAdapter)) {
            return; // Или кинь ошибку, если это критично
        }
        DynamicTrainModelAdapter target = (DynamicTrainModelAdapter) vehicleResourceObj;

        // 2. Теперь переменная target снова существует, и код ниже будет работать
        String path = MtrModelRegistryUtil.getPathFromDummyBbData(model);
        target.parts.clear();

        try {
            ResourceLocation modelLocation = new ResourceLocation(path);
            target.parts.put("main", modelLocation);
        } catch (Exception e) {
            Main.LOGGER.error("Failed loading model into VehicleResource", e);
        }
    }

    // Вспомогательный класс-адаптер, заменяющий mtr.client.DynamicTrainModel
    public static class DynamicTrainModelAdapter {
        public final VehicleResource src;
        public final com.google.gson.JsonObject properties;
        public final Map<String, Object> parts = new HashMap<>();
        public final Map<String, Object> partsInfo = new HashMap<>();

        public DynamicTrainModelAdapter(VehicleResource src) {
            this.src = src;
            this.properties = new com.google.gson.JsonObject();
            // Нам критически важно иметь тут пустой массив деталей, чтобы NTE не упал
            this.properties.add("parts", new com.google.gson.JsonArray());
        }
    }


    public static void loadVanillaModelInto(JsonObject model, DynamicTrainModelAdapter target) {
        // Метод оптимизации ванильных BBModel. Так как mtr.mappings.ModelMapper больше не существует в MTR 4,
        // этот метод временно оставляем пустым, чтобы не вызывать критических ошибок маппингов геометрии,
        // либо NTE будет использовать стандартный рендер MTR 4 для .bbmodel.
    }

    public static class PartBatch {
        public final String doorOffset;
        public final String renderCondition;
        public final String whitelistedCars;
        public final String blacklistedCars;
        public final boolean skipRenderingIfTooFar;
        public final String batchId;

        public PartBatch(JsonObject partObject, boolean mirror) {
            String rawDoorOffset = partObject.has("door_offset") ? partObject.get("door_offset").getAsString() : "NONE";
            if (mirror) {
                doorOffset = switch (rawDoorOffset) {
                    case "NONE" -> "NONE";
                    case "LEFT_NEGATIVE" -> "LEFT_POSITIVE";
                    case "LEFT_POSITIVE" -> "LEFT_NEGATIVE";
                    case "RIGHT_NEGATIVE" -> "RIGHT_POSITIVE";
                    case "RIGHT_POSITIVE" -> "RIGHT_NEGATIVE";
                    default -> "NONE";
                };
            } else {
                doorOffset = rawDoorOffset;
            }
            this.renderCondition = partObject.has("render_condition") ? partObject.get("render_condition").getAsString() : "ALL";
            this.whitelistedCars = partObject.has("whitelisted_cars") ? partObject.get("whitelisted_cars").getAsString() : "";
            this.blacklistedCars = partObject.has("blacklisted_cars") ? partObject.get("blacklisted_cars").getAsString() : "";

            final String renderStage = partObject.has("stage") ? partObject.get("stage").getAsString().toUpperCase(Locale.ROOT) : "EXTERIOR";
            this.skipRenderingIfTooFar = (partObject.has("skip_rendering_if_too_far") && partObject.get("skip_rendering_if_too_far").getAsBoolean())
                    || renderStage.equals("INTERIOR_TRANSLUCENT");

            this.batchId = String.format("$NTEPart:%s:%s:%s:%s:%s", doorOffset, renderCondition, whitelistedCars, blacklistedCars, skipRenderingIfTooFar);
        }

        public JsonObject getPartObject() {
            JsonObject result = new JsonObject();
            result.addProperty("name", batchId);
            result.addProperty("stage", "EXTERIOR");
            result.addProperty("mirror", false);
            result.addProperty("skip_rendering_if_too_far", skipRenderingIfTooFar);
            result.addProperty("door_offset", doorOffset);
            result.addProperty("render_condition", renderCondition);
            result.add("positions", new JsonParser().parse("[[0, 0]]"));
            result.addProperty("whitelisted_cars", whitelistedCars);
            result.addProperty("blacklisted_cars", blacklistedCars);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PartBatch partBatch = (PartBatch) o;
            return batchId.equals(partBatch.batchId);
        }

        @Override
        public int hashCode() {
            return batchId.hashCode();
        }
    }
}