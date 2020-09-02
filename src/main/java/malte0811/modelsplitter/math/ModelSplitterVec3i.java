package malte0811.modelsplitter.math;

import java.util.Objects;

public class ModelSplitterVec3i {
    private final int x, y, z;

    public ModelSplitterVec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelSplitterVec3i modelSplitterVec3I = (ModelSplitterVec3i) o;
        return x == modelSplitterVec3I.x &&
                y == modelSplitterVec3I.y &&
                z == modelSplitterVec3I.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return x + "_" + y + "_" + z;
    }

    public int distanceSq(ModelSplitterVec3i other) {
        return this.subtract(other).lengthSq();
    }

    public ModelSplitterVec3i subtract(ModelSplitterVec3i other) {
        return new ModelSplitterVec3i(
                this.x - other.x,
                this.y - other.y,
                this.z - other.z
        );
    }

    public int lengthSq() {
        return x * x + y * y + z * z;
    }
}
