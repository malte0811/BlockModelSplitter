package malte0811.modelsplitter.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OBJModel<Texture> {
    public static final String DEFAULT_GROUP = "default";
    private final Map<String, Group<Texture>> faces;
    private final List<Polygon<Texture>> allFaces;

    @Deprecated
    public OBJModel(List<Polygon<Texture>> faces) {
        this(new Group<>(ImmutableList.copyOf(faces)));
    }

    @Deprecated
    public OBJModel(Group<Texture> onlyGroup) {
        this(ImmutableMap.of(DEFAULT_GROUP, onlyGroup));
    }

    public OBJModel(Map<String, Group<Texture>> faces) {
        this.faces = ImmutableMap.copyOf(faces);
        ImmutableList.Builder<Polygon<Texture>> faceListBuilder = ImmutableList.builder();
        for (Group<Texture> group : faces.values()) {
            Preconditions.checkState(!group.getFaces().isEmpty());
            faceListBuilder.addAll(group.getFaces());
        }
        this.allFaces = faceListBuilder.build();
    }

    private OBJModel(Stream<Map.Entry<String, List<Polygon<Texture>>>> faces) {
        this(
                faces.map(e -> Pair.of(e.getKey(), new Group<>(e.getValue())))
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );
    }

    @Deprecated
    public OBJModel() {
        this(ImmutableList.of());
    }

    public static OBJModel<Void> readFromStream(InputStream source) {
        List<Vec3d> points = new ArrayList<>();
        List<Vec3d> normals = new ArrayList<>();
        List<double[]> uvs = new ArrayList<>();
        Map<String, List<Polygon<Void>>> groups = new HashMap<>();
        MutableObject<String> currentGroup = new MutableObject<>(DEFAULT_GROUP);
        new BufferedReader(new InputStreamReader(source))
                .lines()
                .forEach(line -> {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    if (!tokenizer.hasMoreTokens())
                        return;
                    String type = tokenizer.nextToken();
                    switch (type) {
                        case "v":
                            points.add(new Vec3d(readTokens(tokenizer, 3)));
                            break;
                        case "vn":
                            normals.add(new Vec3d(readTokens(tokenizer, 3)));
                            break;
                        case "vt":
                            uvs.add(readTokens(tokenizer, 2));
                            break;
                        case "f":
                            List<Vertex> vertices = new ArrayList<>();
                            while (tokenizer.hasMoreTokens()) {
                                final String vertex = tokenizer.nextToken();
                                String[] parts = vertex.split("/");
                                int v = Integer.parseInt(parts[0]) - 1;
                                double[] vertexUVs;
                                if (!parts[1].isEmpty()) {
                                    int vt = Integer.parseInt(parts[1]) - 1;
                                    vertexUVs = uvs.get(vt);
                                } else {
                                    vertexUVs = new double[]{0, 0};
                                }
                                Vec3d normal;
                                if (parts.length > 2) {
                                    int vn = Integer.parseInt(parts[2]) - 1;
                                    normal = normals.get(vn);
                                } else {
                                    normal = new Vec3d(0, 1, 0);
                                }
                                vertices.add(new Vertex(points.get(v), normal, vertexUVs));
                            }
                            groups.computeIfAbsent(currentGroup.getValue(), s -> new ArrayList<>())
                                    .add(new Polygon<>(vertices, null));
                            break;
                        case "o":
                            currentGroup.setValue(tokenizer.nextToken());
                            break;
                        default:
                            System.out.println("Ignoring line " + line);
                            break;
                    }
                });
        return new OBJModel<>(groups.entrySet().stream());
    }

    public static <Texture> OBJModel<Texture> union(@Nullable OBJModel<Texture> a, @Nullable OBJModel<Texture> b) {
        Stream<Map.Entry<String, Group<Texture>>> unionGroups = Stream.empty();
        if (a != null) {
            unionGroups = Stream.concat(unionGroups, a.getFacesByGroup().entrySet().stream());
        }
        if (b != null) {
            unionGroups = Stream.concat(unionGroups, b.getFacesByGroup().entrySet().stream());
        }
        return new OBJModel<>(unionGroups.collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Group::merge
        )));
    }

    private static double[] readTokens(StringTokenizer tokenizer, int tokens) {
        double[] data = new double[tokens];
        for (int i = 0; i < tokens; ++i) {
            data[i] = Double.parseDouble(tokenizer.nextToken());
        }
        return data;
    }

    public Map<EpsilonMath.Sign, OBJModel<Texture>> split(Plane splitPlane) {
        Map<EpsilonMath.Sign, Map<String, Group<Texture>>> resultFaces = new EnumMap<>(EpsilonMath.Sign.class);
        for (Map.Entry<String, Group<Texture>> group : this.faces.entrySet()) {
            group.getValue().split(splitPlane).forEach(
                    p -> resultFaces.computeIfAbsent(p.getKey(), s -> new HashMap<>())
                            .merge(group.getKey(), p.getValue(), Group::merge)
            );
        }
        return resultFaces.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OBJModel<>(e.getValue())));
    }

    public void write(OutputStream outRaw) {
        PrintStream out = new PrintStream(outRaw);
        Object2IntMap<Vec3d> points = new Object2IntOpenHashMap<>();
        Object2IntMap<double[]> uvs = new Object2IntOpenCustomHashMap<>(DoubleArrays.HASH_STRATEGY);
        for (Map.Entry<String, Group<Texture>> group : faces.entrySet()) {
            out.println("o " + group.getKey());
            for (Polygon<Texture> f : group.getValue().getFaces()) {
                StringJoiner line = new StringJoiner(" ", "f ", "");
                for (Vertex v : f.getPoints()) {
                    final int vIndex = points.computeIntIfAbsent(v.getPosition(), pos -> {
                        out.printf("v %.4f %.4f %.4f\n", pos.get(0), pos.get(1), pos.get(2));
                        return points.size();
                    }) + 1;
                    final int uvIndex = uvs.computeIntIfAbsent(v.getUV(), uv -> {
                        out.printf("vt %.6f %.6f\n", uv[0], uv[1]);
                        return uvs.size();
                    }) + 1;
                    line.add(vIndex + "/" + uvIndex);
                }
                out.println(line.toString());
            }
        }
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    public List<Polygon<Texture>> getFaces() {
        return allFaces;
    }

    public Map<String, Group<Texture>> getFacesByGroup() {
        return faces;
    }

    private OBJModel<Texture> mapGroups(UnaryOperator<Group<Texture>> map) {
        Map<String, Group<Texture>> translatedGroups = new HashMap<>();
        for (Map.Entry<String, Group<Texture>> p : faces.entrySet()) {
            translatedGroups.put(p.getKey(), map.apply(p.getValue()));
        }
        return new OBJModel<>(translatedGroups);
    }

    public OBJModel<Texture> translate(int axis, double amount) {
        return mapGroups(g -> g.translate(axis, amount));
    }

    public OBJModel<Texture> translate(Vec3d offset) {
        return mapGroups(g -> g.translate(offset));
    }

    public OBJModel<Texture> quadify() {
        return mapGroups(Group::quadify);
    }
}
