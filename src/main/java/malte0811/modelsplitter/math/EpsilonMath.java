package malte0811.modelsplitter.math;

public record EpsilonMath(double epsilon) {

    public Sign sign(double firstProduct) {
        if (firstProduct < -epsilon) {
            return Sign.NEGATIVE;
        } else if (firstProduct > epsilon) {
            return Sign.POSITIVE;
        } else {
            return Sign.ZERO;
        }
    }

    public boolean areSame(Vec3d a, Vec3d b) {
        Vec3d diff = a.subtract(b);
        return diff.lengthSquared() < epsilon * epsilon;
    }

    public int floor(double in) {
        return (int) Math.floor(in + epsilon);
    }

    public int ceil(double in) {
        return (int) Math.ceil(in - epsilon);
    }

    public enum Sign {
        POSITIVE,
        ZERO,
        NEGATIVE;

        public Sign invert() {
            return switch (this) {
                case POSITIVE -> NEGATIVE;
                case ZERO -> ZERO;
                case NEGATIVE -> POSITIVE;
            };
        }
    }
}
