package cn.zbx1425.mtrsteamloco.data;

import org.mtr.core.data.Rail;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface RailExtraSupplier {

    // Глобальное хранилище для обхода ограничений MTR 4 Core
    // Оно будет ассоциировать уникальный ключ рельса (или сам рельс) с его высотой
    Map<String, Integer> RAIL_HEIGHT_MAP = new ConcurrentHashMap<>();

    String getModelKey();

    void setModelKey(String key);

    boolean getRenderReversed();

    void setRenderReversed(boolean value);

    float getVerticalCurveRadius();

    void setVerticalCurveRadius(float value);

    int getHeight();

    static float getVTheta(Rail rail, double verticalCurveRadius) {
        // Получаем высоту из нашей мапы по уникальному строковому ключу рельса,
        // чтобы не делать невозможный кастинг. Если данных нет - берем 0.
        // Ключом обычно выступает rail.getHexId() или аналогичный метод MTR 4
        String railKey = rail.getHexId();
        double H = Math.abs(RAIL_HEIGHT_MAP.getOrDefault(railKey, 0));

        // Проверяем актуальный метод длины в MTR 4 Core.
        // Если getLength() не работал, в MTR 4 Core используется метод getDistance()
        double L = rail.railMath.getLength();

        double R = verticalCurveRadius;
        return 2 * (float) Mth.atan2(Math.sqrt(H * H - 4 * R * H + L * L) - L, H - 4 * R);
    }
}