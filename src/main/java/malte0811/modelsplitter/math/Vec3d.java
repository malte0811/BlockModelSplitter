package malte0811.modelsplitter.math;

import com.google.common.base.Preconditions;

public record Vec3d(double x, double y, double z) {
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    public Vec3d {
        Preconditions.checkArgument(Double.isFinite(x));
        Preconditions.checkArgument(Double.isFinite(y));
        Preconditions.checkArgument(Double.isFinite(z));
    }

    public Vec3d(double[] coords) {
        this(coords[0], coords[1], coords[2]);
    }

    public Vec3d(ModelSplitterVec3i vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public double dotProduct(Vec3d other) {
        double ret = 0;
        for (int i = 0; i < 3; ++i) {
            ret += get(i) * other.get(i);
        }
        return ret;
    }

    public double get(int index) {
        return switch (index) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            default -> throw new IllegalStateException("Unexpected index in Vec3d: " + index);
        };
    }

    public Vec3d normalize() {
        var length = length();
        if (length < 1e-4) {
            return this;
        } else {
            return scale(1 / length);
        }
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        double ret = 0;
        for (int i = 0; i < 3; ++i) {
            ret += get(i) * get(i);
        }
        return ret;
    }

    public Vec3d scale(double lambda) {
        return new Vec3d(get(0) * lambda, get(1) * lambda, get(2) * lambda);
    }

    public Vec3d add(Vec3d other) {
        return new Vec3d(
                get(0) + other.get(0),
                get(1) + other.get(1),
                get(2) + other.get(2)
        );
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(
                get(0) - other.get(0),
                get(1) - other.get(1),
                get(2) - other.get(2)
        );
    }
}
