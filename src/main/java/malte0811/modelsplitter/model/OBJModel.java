package malte0811.modelsplitter.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;
import malte0811.modelsplitter.model.MaterialLibrary.OBJMaterial;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic model implementation which can optionally be split into multiple "groups"/parts. Despite the name this
 * class is not specific to OBJ models and can be used with lists of faces obtained from arbitrary sources; however
 * direct conversion to/from OBJ is also supported.
 *
 * @param <Texture> an object representing additional face-specific data which will be preserved through the splitting
 *                  process.
 */
public class OBJModel<Texture> {
    public static final String DEFAULT_GROUP = "default";
    private final Map<String, Group<Texture>> faces;
    private final List<Polygon<Texture>> allFaces;

    public OBJModel(Map<String, Group<Texture>> faces) {
        this.faces = ImmutableMap.copyOf(faces);
        ImmutableList.Builder<Polygon<Texture>> faceListBuilder = ImmutableList.builder();
        for (Group<Texture> group : faces.values()) {
            Preconditions.checkState(!group.getFaces().isEmpty());
            faceListBuilder.addAll(group.getFaces());
        }
        this.allFaces = faceListBuilder.build();
    }

    /**
     * Creates a model from the given list of faces
     */
    public OBJModel(List<Polygon<Texture>> allFaces) {
        this(Map.of("", new Group<>(allFaces)));
    }

    private OBJModel(Stream<Map.Entry<String, List<Polygon<Texture>>>> faces) {
        this(
                faces.map(e -> Pair.of(e.getKey(), new Group<>(e.getValue())))
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );
    }

    /**
     * Loads a model from an OBJ-formatted input
     * @param source a stream of OBJ data
     * @param getMTLInput Given the name of an MTL library, returns a stream of the corresponding MTL file
     * @return the parsed model
     */
    public static OBJModel<OBJMaterial> readFromStream(InputStream source, Function<String, InputStream> getMTLInput) {
        record ParserVertex(int pos, int normal, int uv) {}
        record ParserFace(List<ParserVertex> vertices, OBJMaterial material) {}
        List<Vec3d> points = new ArrayList<>();
        List<Vec3d> normals = new ArrayList<>();
        List<UVCoords> uvs = new ArrayList<>();
        Map<String, List<ParserFace>> parserGroups = new HashMap<>();
        MutableObject<String> currentGroup = new MutableObject<>(DEFAULT_GROUP);
        MutableObject<MaterialLibrary> currentMTL = new MutableObject<>(null);
        MutableObject<OBJMaterial> currentMat = new MutableObject<>(null);
        getRelevantLines(source).forEach(p -> {
            StringTokenizer tokenizer = p.getValue();
            switch (p.getKey()) {
                case "mtllib" -> {
                    var mtlInput = getMTLInput.apply(tokenizer.nextToken());
                    var mtl = MaterialLibrary.parse(getRelevantLines(mtlInput));
                    currentMTL.setValue(mtl);
                }
                case "v" -> points.add(new Vec3d(readTokens(tokenizer, 3)));
                case "vn" -> normals.add(new Vec3d(readTokens(tokenizer, 3)));
                case "vt" -> uvs.add(new UVCoords(readTokens(tokenizer, 2)));
                case "f" -> {
                    List<ParserVertex> vertices = new ArrayList<>();
                    while (tokenizer.hasMoreTokens()) {
                        final String vertex = tokenizer.nextToken();
                        String[] parts = vertex.split("/");
                        int posId = Integer.parseInt(parts[0]) - 1;
                        int vt = !parts[1].isEmpty() ? Integer.parseInt(parts[1]) - 1 : -1;
                        int vn = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : -1;
                        vertices.add(new ParserVertex(posId, vn, vt));
                    }
                    parserGroups.computeIfAbsent(currentGroup.getValue(), s -> new ArrayList<>())
                            .add(new ParserFace(vertices, currentMat.getValue()));
                }
                case "o" -> currentGroup.setValue(tokenizer.nextToken());
                case "s" -> {
                } // NOP, Forge parses this and then ignores it
                case "usemtl" -> {
                    var materialMap = currentMTL.getValue().materials();
                    var materialName = tokenizer.nextToken();
                    var newMaterial = materialMap.get(materialName);
                    currentMat.setValue(Objects.requireNonNull(newMaterial, "No material " + materialName));
                }
                default -> System.out.println("Ignoring line with token " + p.getKey());
            }
        });
        Map<String, List<Polygon<OBJMaterial>>> groups = new HashMap<>();
        for (var parserEntry : parserGroups.entrySet()) {
            List<Polygon<OBJMaterial>> group = new ArrayList<>(parserEntry.getValue().size());
            for (var parserPoly : parserEntry.getValue()) {
                List<Vertex> vertices = new ArrayList<>(parserPoly.vertices().size());
                for (var point : parserPoly.vertices()) {
                    vertices.add(new Vertex(
                            points.get(point.pos),
                            point.normal >= 0 ? normals.get(point.normal) : Vec3d.ZERO,
                            point.uv >= 0 ? uvs.get(point.uv) : UVCoords.ZERO
                    ));
                }
                group.add(new Polygon<>(vertices, parserPoly.material()));
            }
            groups.put(parserEntry.getKey(), group);
        }
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

    private static Stream<Pair<String, StringTokenizer>> getRelevantLines(InputStream in) {
        return new BufferedReader(new InputStreamReader(in))
                .lines()
                .filter(l -> !l.trim().isEmpty() && l.charAt(0) != '#')
                .map(s -> {
                    StringTokenizer tokenizer = new StringTokenizer(s);
                    return Pair.of(tokenizer.nextToken(), tokenizer);
                });
    }

    private static double[] readTokens(StringTokenizer tokenizer, int tokens) {
        double[] data = new double[tokens];
        for (int i = 0; i < tokens; ++i) {
            data[i] = Double.parseDouble(tokenizer.nextToken());
        }
        return data;
    }

    /**
     * Splits/cuts the model along the given plane
     * @return The parts of the model that lie "below", "on", and "above" the plane
     */
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

    /**
     * Exports the model in OBJ format
     * @param outRaw the stream to write OBJ data to
     */
    public void write(OutputStream outRaw) {
        PrintStream out = new PrintStream(outRaw);
        Object2IntMap<Vec3d> points = new Object2IntOpenHashMap<>();
        Object2IntMap<UVCoords> uvs = new Object2IntOpenHashMap<>();
        for (Map.Entry<String, Group<Texture>> group : faces.entrySet()) {
            out.println("o " + group.getKey());
            for (Polygon<Texture> f : group.getValue().getFaces()) {
                StringJoiner line = new StringJoiner(" ", "f ", "");
                for (Vertex v : f.getPoints()) {
                    final int vIndex = points.computeIfAbsent(v.position(), (Vec3d pos) -> {
                        out.printf("v %.4f %.4f %.4f\n", pos.get(0), pos.get(1), pos.get(2));
                        return points.size();
                    }) + 1;
                    final int uvIndex = uvs.computeIfAbsent(v.uv(), (UVCoords uv) -> {
                        out.printf("vt %.6f %.6f\n", uv.u(), uv.v());
                        return uvs.size();
                    }) + 1;
                    line.add(vIndex + "/" + uvIndex);
                }
                out.println(line);
            }
        }
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    /**
     * @return all faces of the model
     */
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

    /**
     * Creates a model equivalent to this one in which all faces have exactly 4 vertices.
     * @return the new model
     */
    public OBJModel<Texture> quadify() {
        return mapGroups(Group::quadify);
    }

    /**
     * Creates a model equivalent to this one except that the normals on all faces with zero normals are set to be
     * orthogonal to the face.
     * @return the new model
     */
    public OBJModel<Texture> recomputeZeroNormals() {
        return mapGroups(Group::recomputeZeroNormals);
    }
}
