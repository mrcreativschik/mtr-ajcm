package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RailModelProperties {

    public Component name;

    // Вместо Sowcer Model используем стандартный ResourceLocation для пути к модели
    public ResourceLocation modelLocation;

    public Long boundingBox;
    public float repeatInterval;
    public float yOffset;

    // Конструктор, принимающий стандартный путь к модели (String или ResourceLocation)
    public RailModelProperties(Component name, String modelPath, float repeatInterval, float yOffset) {
        this.name = name;
        this.repeatInterval = repeatInterval;
        this.yOffset = yOffset;
        this.boundingBox = 0L; // В MTR 4 bounding box рельса обычно считается автоматически

        if (modelPath == null || modelPath.isEmpty()) {
            this.modelLocation = null;
        } else {
            try {
                this.modelLocation = new ResourceLocation(modelPath);
            } catch (Exception e) {
                this.modelLocation = null;
            }
        }
    }
}