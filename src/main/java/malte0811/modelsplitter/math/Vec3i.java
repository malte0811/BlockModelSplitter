package malte0811.modelsplitter.math;

import java.util.Objects;

public class Vec3i {
    private final int x, y, z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3i vec3i = (Vec3i) o;
        return x == vec3i.x &&
                y == vec3i.y &&
                z == vec3i.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return x + "_" + y + "_" + z;
    }
}
