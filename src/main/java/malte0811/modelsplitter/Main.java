package malte0811.modelsplitter;

import malte0811.modelsplitter.model.OBJModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        final String name = "bucket_wheel";
        FileInputStream fis = new FileInputStream(name + ".obj.ie");
        var model = OBJModel.readFromStream(fis, f -> {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        model.write(new FileOutputStream(name + "_rewrite.obj"));
    }
}
