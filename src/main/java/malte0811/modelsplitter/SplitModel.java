package malte0811.modelsplitter;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;
import malte0811.modelsplitter.math.Vec3i;
import malte0811.modelsplitter.model.OBJModel;
import malte0811.modelsplitter.model.Polygon;
import malte0811.modelsplitter.model.Vertex;

import java.util.Map;

public class SplitModel {
    private static final EpsilonMath EPS_MATH = new EpsilonMath(1e-5);

    private final Map<Vec3i, OBJModel> submodels;

    public SplitModel(OBJModel input) {
        ImmutableMap.Builder<Vec3i, OBJModel> submodels = ImmutableMap.builder();
        for (Int2ObjectMap.Entry<OBJModel> xSlice : splitInPlanes(input, 0).int2ObjectEntrySet()) {
            Int2ObjectMap<OBJModel> columns = splitInPlanes(xSlice.getValue(), 2);
            for (Int2ObjectMap.Entry<OBJModel> zColumn : columns.int2ObjectEntrySet()) {
                Int2ObjectMap<OBJModel> dices = splitInPlanes(zColumn.getValue(), 1);
                for (Int2ObjectMap.Entry<OBJModel> yDice : dices.int2ObjectEntrySet()) {
                    submodels.put(
                            new Vec3i(xSlice.getIntKey(), yDice.getIntKey(), zColumn.getIntKey()),
                            yDice.getValue()
                    );
                }
            }
        }
        this.submodels = submodels.build();
    }

    public Map<Vec3i, OBJModel> getParts() {
        return submodels;
    }

    private static Int2ObjectMap<OBJModel> splitInPlanes(OBJModel input, int axis) {
        if (input.isEmpty()) {
            return new Int2ObjectOpenHashMap<>();
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Polygon f : input.getFaces()) {
            for (Vertex v : f.getPoints()) {
                double pos = v.getPosition().get(axis);
                min = Math.min(min, pos);
                max = Math.max(max, pos);
            }
        }
        int firstBorder = EPS_MATH.ceil(min);
        int lastBorder = EPS_MATH.floor(max);
        Int2ObjectMap<OBJModel> modelPerSection = new Int2ObjectOpenHashMap<>();
        for (int borderPos = firstBorder; borderPos <= lastBorder; ++borderPos) {
            double[] vecData = new double[3];
            vecData[axis] = 1;
            Vec3d normal = new Vec3d(vecData);
            Plane cut = new Plane(normal, borderPos);
            Map<EpsilonMath.Sign, OBJModel> splitModel = input.split(cut);
            OBJModel sectionModel = OBJModel.union(
                    splitModel.get(EpsilonMath.Sign.NEGATIVE),
                    //TODO sensible treatment of zero
                    splitModel.get(EpsilonMath.Sign.ZERO)
            );
            putModel(modelPerSection, axis, borderPos - 1, sectionModel);
            input = splitModel.get(EpsilonMath.Sign.POSITIVE);
            if (input == null) {
                input = new OBJModel();
            }
        }
        putModel(modelPerSection, axis, lastBorder, input);
        return modelPerSection;
    }

    private static void putModel(
            Int2ObjectMap<OBJModel> sectionModels,
            int axis,
            int section,
            OBJModel baseSectionModel
    ) {
        if (!baseSectionModel.isEmpty()) {
            sectionModels.put(
                    section,
                    baseSectionModel.translate(axis, -section)
                            .quadify()
            );
        }
    }
}
