package malte0811.modelsplitter.model;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Stream;

public record MaterialLibrary(Map<String, OBJMaterial> materials) {

    public static MaterialLibrary parse(Stream<Pair<String, StringTokenizer>> lines) {
        Mutable<String> currentMaterial = new MutableObject<>();
        Mutable<String> currentKdMap = new MutableObject<>();
        Map<String, OBJMaterial> materials = new HashMap<>();
        lines.forEach(p -> {
            switch (p.getKey()) {
                case "newmtl" -> {
                    addMaterial(currentMaterial, currentKdMap, materials);
                    currentKdMap.setValue(null);
                    currentMaterial.setValue(p.getValue().nextToken());
                }
                case "map_Kd" -> currentKdMap.setValue(p.getValue().nextToken());
            }
        });
        addMaterial(currentMaterial, currentKdMap, materials);
        return new MaterialLibrary(materials);
    }

    private static void addMaterial(
            Mutable<String> name,
            Mutable<String> kdMap,
            Map<String, OBJMaterial> out
    ) {
        if (name.getValue() != null) {
            Preconditions.checkState(kdMap.getValue() != null);
            out.put(name.getValue(), new OBJMaterial(name.getValue(), kdMap.getValue()));
        }
    }

    public static record OBJMaterial(String name, String map_Kd) {}
}
