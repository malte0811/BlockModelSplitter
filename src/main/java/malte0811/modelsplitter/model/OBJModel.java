package malte0811.modelsplitter.model;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import malte0811.modelsplitter.math.EpsilonMath;
import malte0811.modelsplitter.math.Plane;
import malte0811.modelsplitter.math.Vec3d;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OBJModel {
    private final List<Polygon> faces;

    public OBJModel(List<Polygon> faces) {
        this.faces = ImmutableList.copyOf(faces);
    }

    public OBJModel(InputStream source) {
        List<Vec3d> points = new ArrayList<>();
        List<double[]> uvs = new ArrayList<>();
        List<Polygon> faces = new ArrayList<>();
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
                        case "vt":
                            uvs.add(readTokens(tokenizer, 2));
                            break;
                        case "f":
                            List<Vertex> vertices = new ArrayList<>();
                            while (tokenizer.hasMoreTokens()) {
                                final String vertex = tokenizer.nextToken();
                                final int slash = vertex.indexOf('/');
                                int v = Integer.parseInt(vertex.substring(0, slash)) - 1;
                                int vt = Integer.parseInt(vertex.substring(slash + 1)) - 1;
                                vertices.add(new Vertex(
                                        points.get(v),
                                        //TODO
                                        new Vec3d(0, 1, 0),
                                        uvs.get(vt)
                                ));
                            }
                            faces.add(new Polygon(vertices));
                            break;
                        default:
                            System.out.println("Ignoring line " + line);
                            break;
                    }
                });
        this.faces = ImmutableList.copyOf(faces);
    }

    public OBJModel() {
        this(ImmutableList.of());
    }

    public static OBJModel union(@Nullable OBJModel a, @Nullable OBJModel b) {
        List<Polygon> result = new ArrayList<>();
        if (a != null) {
            result.addAll(a.getFaces());
        }
        if (b != null) {
            result.addAll(b.getFaces());
        }
        return new OBJModel(result);
    }

    private double[] readTokens(StringTokenizer tokenizer, int tokens) {
        double[] data = new double[tokens];
        for (int i = 0; i < tokens; ++i) {
            data[i] = Double.parseDouble(tokenizer.nextToken());
        }
        return data;
    }

    public Map<EpsilonMath.Sign, OBJModel> split(Plane p) {
        Map<EpsilonMath.Sign, List<Polygon>> resultFaces = new EnumMap<>(EpsilonMath.Sign.class);
        for (Polygon f : this.faces) {
            Map<EpsilonMath.Sign, Polygon> splitResult = f.splitAlong(p);
            for (Map.Entry<EpsilonMath.Sign, Polygon> e : splitResult.entrySet()) {
                resultFaces.computeIfAbsent(e.getKey(), s -> new ArrayList<>())
                        .add(e.getValue());
            }
        }
        return resultFaces.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OBJModel(e.getValue())));
    }

    public void write(OutputStream outRaw) {
        PrintStream out = new PrintStream(outRaw);
        Object2IntMap<Vec3d> points = new Object2IntOpenHashMap<>();
        Object2IntMap<double[]> uvs = new Object2IntOpenCustomHashMap<>(DoubleArrays.HASH_STRATEGY);
        for (Polygon f : faces) {
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

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    public List<Polygon> getFaces() {
        return faces;
    }

    public OBJModel translate(int axis, double amount) {
        List<Polygon> translatedFaces = new ArrayList<>(faces.size());
        for (Polygon p : faces) {
            translatedFaces.add(p.translate(axis, amount));
        }
        return new OBJModel(translatedFaces);
    }

    public OBJModel quadify() {
        List<Polygon> translatedFaces = new ArrayList<>(faces.size());
        for (Polygon p : faces) {
            translatedFaces.addAll(p.quadify());
        }
        return new OBJModel(translatedFaces);
    }
}
