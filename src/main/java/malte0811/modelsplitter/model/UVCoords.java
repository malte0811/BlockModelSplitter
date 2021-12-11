package malte0811.modelsplitter.model;

public record UVCoords(double u, double v) {
    public static final UVCoords ZERO = new UVCoords(0, 0);

    public UVCoords(double[] readTokens) {
        this(readTokens[0], readTokens[1]);
    }

    public static UVCoords interpolate(UVCoords a, UVCoords b, double lambda) {
        return new UVCoords(
                lambda * a.u() + (1 - lambda) * b.u(),
                lambda * a.v() + (1 - lambda) * b.v()
        );
    }
}
