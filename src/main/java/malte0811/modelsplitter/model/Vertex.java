package malte0811.modelsplitter.model;

import malte0811.modelsplitter.math.Vec3d;

public record Vertex(Vec3d position, Vec3d normal, UVCoords uv) {

    // lambda * a + (1 - lambda) * b
    public static Vertex interpolate(Vertex a, Vertex b, double lambda) {
        return new Vertex(
                a.position.scale(lambda).add(b.position.scale(1 - lambda)),
                a.normal.scale(lambda).add(b.normal.scale(1 - lambda)),
                UVCoords.interpolate(a.uv(), b.uv(), lambda)
        );
    }

    public Vertex translate(int axis, double amount) {
        double[] offsetData = new double[3];
        offsetData[axis] = amount;
        return translate(new Vec3d(offsetData));
    }

    public Vertex translate(Vec3d offset) {
        return new Vertex(position.add(offset), normal, uv);
    }
}
