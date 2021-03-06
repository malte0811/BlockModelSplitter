package malte0811.modelsplitter;

import com.google.common.collect.ImmutableSet;
import malte0811.modelsplitter.math.ModelSplitterVec3i;
import malte0811.modelsplitter.model.OBJModel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class Main
{
    public static void main(String[] args) throws Exception {
        final String name = "teslacoil";
        FileInputStream fis = new FileInputStream(name + ".obj");
        OBJModel<Void> model = OBJModel.readFromStream(fis);
        SplitModel<Void> split = new SplitModel<>(model);
        for (Map.Entry<ModelSplitterVec3i, OBJModel<Void>> e : split.getParts().entrySet()) {
            e.getValue().write(new FileOutputStream(name + "_" + e.getKey() + ".obj"));
        }
        ClumpedModel<Void> clumped = new ClumpedModel<>(split, ImmutableSet.of(
                new ModelSplitterVec3i(0, 0, 0),
                new ModelSplitterVec3i(0, 0, -1)
        ));
        for (Map.Entry<ModelSplitterVec3i, OBJModel<Void>> e : clumped.getClumpedParts().entrySet()) {
            e.getValue().write(new FileOutputStream(name + "_clumped_" + e.getKey() + ".obj"));
        }
    }
}
