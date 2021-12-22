package malte0811.modelsplitter.model;


import com.google.common.collect.ImmutableList;
import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public record Group<Texture>(List<Polygon<Texture>> faces) {
    public Group(List<Polygon<Texture>> faces) {
        this.faces = ImmutableList.copyOf(faces);
    }

    public List<Polygon<Texture>> getFaces() {
        return faces;
    }

    public Stream<Pair<EpsilonMath.Sign, Group<Texture>>> split(Plane p) {
        Map<EpsilonMath.Sign, List<Polygon<Texture>>> splitFaces = new EnumMap<>(EpsilonMath.Sign.class);
        for (Polygon<Texture> f : getFaces()) {
            Map<EpsilonMath.Sign, Polygon<Texture>> splitResult = f.splitAlong(p);
            for (Map.Entry<EpsilonMath.Sign, Polygon<Texture>> e : splitResult.entrySet()) {
                splitFaces.computeIfAbsent(e.getKey(), s -> new ArrayList<>())
                        .add(e.getValue());
            }
        }
        return splitFaces.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), new Group<>(e.getValue())));
    }

    public Group<Texture> merge(Group<Texture> other) {
        return new Group<>(
                ImmutableList.<Polygon<Texture>>builder()
                        .addAll(getFaces())
                        .addAll(other.getFaces())
                        .build()
        );
    }

    private Group<Texture> mapFaces(UnaryOperator<Polygon<Texture>> map) {
        return flatmapFaces(map.andThen(Stream::of));
    }

    private Group<Texture> flatmapFaces(Function<Polygon<Texture>, Stream<Polygon<Texture>>> map) {
        return new Group<>(faces.stream()
                .flatMap(map)
                .collect(ImmutableList.toImmutableList())
        );
    }

    public Group<Texture> translate(int axis, double amount) {
        return mapFaces(p -> p.translate(axis, amount));
    }

    public Group<Texture> translate(Vec3d offset) {
        return mapFaces(p -> p.translate(offset));
    }

    public Group<Texture> quadify() {
        return flatmapFaces(p -> p.quadify().stream());
    }

    public Group<Texture> recomputeZeroNormals() {
        return mapFaces(Polygon::recomputeZeroNormals);
    }
}
