package cn.zbx1425.mtrsteamloco.render.integration;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.CustomResources;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.mixin.ModelMapperAccessor;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.loader.ObjModelLoader;
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

    private static Map<String, RawModel> cachedModels;
    private static String cachedPath;
    private static long cachedPathMtime = 0;

    // Меням сигнатуру метода: теперь принимаем Object (который является VehicleResource)
    public static void loadInto(JsonObject model, Object vehicleResourceObj) {
        if (!(vehicleResourceObj instanceof VehicleResource)) return;
        VehicleResource vehicleResource = (VehicleResource) vehicleResourceObj;

        // Создаем адаптер, который притворяется старым DynamicTrainModel
        DynamicTrainModelAdapter adapter = new DynamicTrainModelAdapter(vehicleResource);

        if (MtrModelRegistryUtil.isDummyBbData(model)) {
            loadObjInto(model, adapter);
        } else {
            if (!model.has("dummyBbData")) return;
            boolean bbModelPreload = MtrModelRegistryUtil.getBbModelPreloadFromDummyBbData(
                    model.get("dummyBbData").getAsJsonObject());
            if (ClientConfig.enableBbModelPreload || bbModelPreload) {
                loadVanillaModelInto(model, adapter);
            }
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

    public static void loadObjInto(JsonObject model, DynamicTrainModelAdapter target) {
        int bbDataType = MtrModelRegistryUtil.getDummyBbDataType(model);
        String path = MtrModelRegistryUtil.getPathFromDummyBbData(model);
        target.parts.clear();
        try {
            if (target.properties.has("atlasIndex")) {
                MainClient.atlasManager.load(
                        MtrModelRegistryUtil.resourceManager,
                        new ResourceLocation(target.properties.get("atlasIndex").getAsString())
                );
            }

            Map<String, RawModel> models;
            if (bbDataType == 1) {
                String modelLocations = MtrModelRegistryUtil.getPathFromDummyBbData(model);
                if (modelLocations.contains("|")) {
                    models = new HashMap<>();
                    String[] rlListPairs = modelLocations.split("\\|");
                    ArrayList<JsonObject> previousParts = new ArrayList<>();
                    target.properties.get("parts").getAsJsonArray()
                            .forEach(elem -> previousParts.add(elem.getAsJsonObject()));
                    JsonArray newParts = new JsonArray();
                    for (int i = 0; i < rlListPairs.length / 2; i++) {
                        ResourceLocation modelLocation = new ResourceLocation(rlListPairs[i * 2]);
                        String[] extraAttribs = rlListPairs[i * 2 + 1].split(";", -1)[2].split(",");
                        boolean isModelReversed = Arrays.asList(extraAttribs).contains("reversed");
                        String modelLocationName = modelLocation.getPath().substring(modelLocation.getPath().lastIndexOf('/') + 1)
                                + (isModelReversed ? "/reversed" : "");

                        Map<String, RawModel> modelParts = ObjModelLoader.loadModels(
                                MtrModelRegistryUtil.resourceManager,
                                modelLocation,
                                MainClient.atlasManager
                        );
                        for (Map.Entry<String, RawModel> entry : modelParts.entrySet()) {
                            if (isModelReversed) {
                                entry.getValue().sourceLocation = new ResourceLocation(
                                        entry.getValue().sourceLocation.toString().substring(0, entry.getValue().sourceLocation.toString().lastIndexOf("/"))
                                                + "/reversed"
                                                + entry.getValue().sourceLocation.toString().substring(entry.getValue().sourceLocation.toString().lastIndexOf("/"))
                                );
                                entry.getValue().applyRotation(new Vector3f(0f, 1f, 0f), 180f);
                            }
                            models.put(modelLocationName + "/" + entry.getKey(), entry.getValue());
                        }

                        previousParts.forEach(elem -> {
                            if (elem.get("name").getAsString().startsWith(modelLocationName)) {
                                newParts.add(elem);
                            }
                        });
                        previousParts.removeIf(elem -> elem.get("name").getAsString().startsWith(modelLocationName));
                    }
                    for (int i = 0; i < rlListPairs.length / 2; i++) {
                        ResourceLocation modelLocation = new ResourceLocation(rlListPairs[i * 2]);
                        String[] extraAttribs = rlListPairs[i * 2 + 1].split(";", -1)[2].split(",");
                        boolean isModelReversed = Arrays.asList(extraAttribs).contains("reversed");
                        String modelLocationName = modelLocation.getPath().substring(modelLocation.getPath().lastIndexOf('/') + 1)
                                + (isModelReversed ? "/reversed" : "");
                        String whiteList = rlListPairs[i * 2 + 1].split(";", -1)[0];
                        String blackList = rlListPairs[i * 2 + 1].split(";", -1)[1];

                        for (JsonObject part : previousParts) {
                            JsonObject newPartObj = (JsonObject)(new JsonParser()).parse(part.toString());
                            String previousName = newPartObj.get("name").getAsString();
                            newPartObj.remove("name");
                            newPartObj.addProperty("name", modelLocationName + "/" + previousName);
                            newPartObj.remove("whitelisted_cars");
                            newPartObj.addProperty("whitelisted_cars", whiteList);
                            newPartObj.remove("blacklisted_cars");
                            newPartObj.addProperty("blacklisted_cars", blackList);

                            if (isModelReversed) {
                                String newDoorOffset = newPartObj.get("door_offset").getAsString().toUpperCase(Locale.ROOT);
                                newDoorOffset = switch (newDoorOffset) {
                                    case "LEFT_POSITIVE" -> "RIGHT_NEGATIVE";
                                    case "RIGHT_POSITIVE" -> "LEFT_NEGATIVE";
                                    case "LEFT_NEGATIVE" -> "RIGHT_POSITIVE";
                                    case "RIGHT_NEGATIVE" -> "LEFT_POSITIVE";
                                    default -> newDoorOffset;
                                };
                                newPartObj.remove("door_offset");
                                newPartObj.addProperty("door_offset", newDoorOffset);

                                String newRenderCondition = newPartObj.get("render_condition").getAsString().toUpperCase(Locale.ROOT);
                                newRenderCondition = switch (newRenderCondition) {
                                    case "DOOR_LEFT_OPEN" -> "DOOR_RIGHT_OPEN";
                                    case "DOOR_RIGHT_OPEN" -> "DOOR_LEFT_OPEN";
                                    case "DOOR_LEFT_CLOSED" -> "DOOR_RIGHT_CLOSED";
                                    case "DOOR_RIGHT_CLOSED" -> "DOOR_LEFT_CLOSED";
                                    default -> newRenderCondition;
                                };
                                newPartObj.remove("render_condition");
                                newPartObj.addProperty("render_condition", newRenderCondition);

                                JsonArray newPositions = new JsonArray();
                                JsonArray oldPositions = newPartObj.get("positions").getAsJsonArray();
                                for (int j = 0; j < oldPositions.size(); j++) {
                                    JsonArray pos = oldPositions.get(j).getAsJsonArray();
                                    pos.set(1, new JsonPrimitive(-pos.get(1).getAsFloat()));
                                    newPositions.add(pos);
                                }
                                newPartObj.remove("positions");
                                newPartObj.add("positions", newPositions);
                            }
                            newParts.add(newPartObj);
                        }
                    }
                    target.properties.remove("parts");
                    target.properties.add("parts", newParts);
                } else {
                    models = ObjModelLoader.loadModels(
                            MtrModelRegistryUtil.resourceManager,
                            new ResourceLocation(modelLocations),
                            MainClient.atlasManager
                    );
                }
            } else {
                if (cachedModels == null
                        || !path.equals(cachedPath) || new File(path).lastModified() != cachedPathMtime) {
                    MainClient.modelManager.clearNamespace("mtrsteamloco-external");
                    cachedModels = ObjModelLoader.loadExternalModels(
                            MtrModelRegistryUtil.getPathFromDummyBbData(model),
                            MainClient.atlasManager
                    );
                    for (RawModel partModel : cachedModels.values()) {
                        partModel.replaceAllTexture(MtrModelRegistryUtil.PLACEHOLDER_TILE_TEXTURE_LOCATION);
                    }
                    cachedPath = path;
                    cachedPathMtime = new File(path).lastModified();
                }
                models = cachedModels;
            }

            String repaintTexture = MtrModelRegistryUtil.getTextureIdFromDummyBbData(model);
            if (!StringUtils.isEmpty(repaintTexture)) {
                for (RawModel partModel : models.values()) {
                    partModel.replaceTexture("default.png", new ResourceLocation(repaintTexture));
                }
            }
            if (MtrModelRegistryUtil.getFlipVFromDummyBbData(model)) {
                for (RawModel partModel : models.values()) {
                    partModel.applyUVMirror(false, true);
                }
            }

            JsonArray propertyParts = target.properties.getAsJsonArray("parts");
            Map<String, RawModel> finalModels = models;
            propertyParts.forEach(jsonElement -> {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String name = jsonObject.get("name").getAsString();
                RawModel partModel = finalModels.getOrDefault(name, null);

                if (partModel != null && jsonObject.has("stage")) {
                    final String renderStage = jsonObject.get("stage").getAsString().toUpperCase(Locale.ROOT);
                    switch (renderStage) {
                        case "EXTERIOR":
                            partModel.setAllRenderType("reset");
                            break;
                        case "INTERIOR":
                            partModel.setAllRenderType("interior");
                            break;
                        case "INTERIOR_TRANSLUCENT":
                            partModel.setAllRenderType("interiortranslucent");
                            break;
                        case "LIGHTS":
                            partModel.setAllRenderType("light");
                            break;
                        case "ALWAYS_ON_LIGHTS":
                            partModel.setAllRenderType("lighttranslucent");
                            break;
                    }
                }
            });

            if (bbDataType == 1) {
                Map<PartBatch, RawModel> mergedModels = new HashMap<>();
                target.properties.getAsJsonArray("parts").forEach(elem -> {
                    JsonObject partObject = elem.getAsJsonObject();
                    String partName = partObject.get("name").getAsString();
                    RawModel partModel = models.get(partName);
                    if (partModel == null) return;

                    final boolean mirror = partObject.has("mirror") && partObject.get("mirror").getAsBoolean();
                    PartBatch batch = new PartBatch(partObject, mirror);
                    RawModel mergedModel = mergedModels.computeIfAbsent(batch, ignored -> new RawModel());

                    partObject.getAsJsonArray("positions").forEach(positionElement -> {
                        final float x = positionElement.getAsJsonArray().get(0).getAsFloat() / 16f;
                        final float z = positionElement.getAsJsonArray().get(1).getAsFloat() / 16f;
                        if (!mirror && x == 0 && z == 0) {
                            mergedModel.append(partModel);
                        } else {
                            RawModel clonedModel = partModel.copy();
                            if (mirror) {
                                clonedModel.applyRotation(Vector3f.YP, 180);
                                clonedModel.applyTranslation(-x, 0, z);
                            } else {
                                clonedModel.applyTranslation(x, 0, z);
                            }
                            mergedModel.append(clonedModel);
                        }
                    });
                });

                target.parts.clear();
                JsonArray partsPropArray = new JsonArray();
                target.properties.add("parts", partsPropArray);
                boolean isLoadingFromEditor = !GlStateTracker.isStateProtected;
                if (isLoadingFromEditor) GlStateTracker.capture();
                for (Map.Entry<PartBatch, RawModel> entry : mergedModels.entrySet()) {
                    PartBatch batch = entry.getKey();
                    RawModel mergedModel = entry.getValue();
                    target.parts.put(batch.batchId, new SowcerModelAgent(mergedModel, false));
                    partsPropArray.add(batch.getPartObject());
                }
                if (isLoadingFromEditor) GlStateTracker.restore();
            } else {
                boolean isLoadingFromEditor = !GlStateTracker.isStateProtected;
                if (isLoadingFromEditor) GlStateTracker.capture();
                for (Map.Entry<String, RawModel> entry : models.entrySet()) {
                    target.parts.put(entry.getKey(), new SowcerModelAgent(entry.getValue(), false));
                }
                if (isLoadingFromEditor) GlStateTracker.restore();
            }

            // После того как NTE заполнил target.parts, мы можем обратно сериализовать изменения в VehicleResource,
            // если MTR 4 этого ожидает. В данном случае NTE просто кэширует модели в карту частей.

        } catch (Exception e) {
            Main.LOGGER.error("Failed loading OBJ into VehicleResource via Adapter", e);
            MtrModelRegistryUtil.recordLoadingError("Failed loading OBJ model " + path, e);
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