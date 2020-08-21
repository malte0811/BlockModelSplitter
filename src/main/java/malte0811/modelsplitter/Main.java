package malte0811.modelsplitter;

import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;
import malte0811.modelsplitter.model.OBJModel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class Main
{
    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("arc_furnace.obj");
        OBJModel model = new OBJModel(fis);
        Map<EpsilonMath.Sign, OBJModel> splitResult = model.split(new Plane(new Vec3d(1, 2, 3), 4));
        for (Map.Entry<EpsilonMath.Sign, OBJModel> e : splitResult.entrySet()) {
            e.getValue().write(new FileOutputStream("arc_furnace_"+e.getKey().name()+".obj"));
        }
    }
}
