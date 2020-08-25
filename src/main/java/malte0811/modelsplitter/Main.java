package malte0811.modelsplitter;

import malte0811.modelsplitter.model.OBJModel;
import net.minecraft.util.math.Vec3i;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class Main
{
    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("arc_furnace.obj");
        OBJModel<Void> model = OBJModel.readFromStream(fis);
        SplitModel<Void> split = new SplitModel<>(model);
        for (Map.Entry<Vec3i, OBJModel<Void>> e : split.getParts().entrySet()) {
            e.getValue().write(new FileOutputStream("arc_furnace_" + e.getKey() + ".obj"));
        }
    }
}
