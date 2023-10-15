import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Transformation;
import malte0811.modelsplitter.ClumpedModel;
import malte0811.modelsplitter.SplitModel;
import malte0811.modelsplitter.math.ModelSplitterVec3i;
import malte0811.modelsplitter.math.Vec3d;
import malte0811.modelsplitter.model.OBJModel;
import malte0811.modelsplitter.model.Polygon;
import malte0811.modelsplitter.model.UVCoords;
import malte0811.modelsplitter.model.Vertex;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Vec3i;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PolygonUtils {
    /**
     * "End-to-end" splitting flow: Converts the given model (=list of quads) to models to render in each of the
     * possible blocks.
     *
     * @param in        The model to split
     * @param parts     The block positions in which part of the model can be rendered. This is in the same coordinates as
     *                  the model itself.
     * @param transform Transformation to apply to the final quads.
     * @return The model to render in each block
     */
    public static Map<Vec3i, List<BakedQuad>> split(List<BakedQuad> in, Set<Vec3i> parts, ModelState transform) {
        // Convert from MC quads to polygons
        List<Polygon<ExtraQuadData>> polys = in.stream()
                .map(PolygonUtils::toPolygon)
                .collect(Collectors.toList());
        // "Plain" splitting, some parts may be in blocks that cannot be used to render
        Map<ModelSplitterVec3i, OBJModel<ExtraQuadData>> splitParts = SplitModel.splitModel(new OBJModel<>(polys));
        // Convert list of possible positions into BlockModelSplitter format
        Set<ModelSplitterVec3i> partsBMS = parts.stream()
                .map(v -> new ModelSplitterVec3i(v.getX(), v.getY(), v.getZ()))
                .collect(Collectors.toSet());
        // Clump models into positions which can be used to render them
        Map<ModelSplitterVec3i, OBJModel<ExtraQuadData>> clumpedParts = ClumpedModel.clumpModel(splitParts, partsBMS);

        // Convert the resulting models back to MC data
        Map<Vec3i, List<BakedQuad>> map = new HashMap<>();
        for (Entry<ModelSplitterVec3i, OBJModel<ExtraQuadData>> e : clumpedParts.entrySet()) {
            List<BakedQuad> subModelFaces = new ArrayList<>(e.getValue().getFaces().size());
            for (Polygon<ExtraQuadData> p : e.getValue().getFaces())
                subModelFaces.add(PolygonUtils.toBakedQuad(p, transform));
            Vec3i mcKey = new Vec3i(e.getKey().x(), e.getKey().y(), e.getKey().z());
            map.put(mcKey, subModelFaces);
        }
        return map;
    }

    /**
     * Converts the given quad into an equivalent BlockModelSplitter polygon
     */
    public static Polygon<ExtraQuadData> toPolygon(BakedQuad quad) {
        List<Vertex> vertices = new ArrayList<>(4);
        final int posOffset = getOffset(DefaultVertexFormat.ELEMENT_POSITION);
        final int uvOffset = getOffset(DefaultVertexFormat.ELEMENT_UV);
        final int normalOffset = getOffset(DefaultVertexFormat.ELEMENT_NORMAL);
        final int colorOffset = getOffset(DefaultVertexFormat.ELEMENT_COLOR);
        final int color = quad.getVertices()[colorOffset];
        for (int v = 0; v < 4; ++v) {
            final int baseOffset = v * DefaultVertexFormat.BLOCK.getVertexSize() / 4;
            int packedNormal = quad.getVertices()[normalOffset + baseOffset];
            final Vec3d normalVec = new Vec3d(
                    (byte) (packedNormal),
                    (byte) (packedNormal >> 8),
                    (byte) (packedNormal >> 16)
            ).normalize();
            final UVCoords uv = new UVCoords(
                    Float.intBitsToFloat(quad.getVertices()[uvOffset + baseOffset]),
                    Float.intBitsToFloat(quad.getVertices()[uvOffset + baseOffset + 1])
            );
            final Vec3d pos = new Vec3d(
                    Float.intBitsToFloat(quad.getVertices()[baseOffset + posOffset]),
                    Float.intBitsToFloat(quad.getVertices()[baseOffset + posOffset + 1]),
                    Float.intBitsToFloat(quad.getVertices()[baseOffset + posOffset + 2])
            );
            vertices.add(new Vertex(pos, normalVec, uv));
            Preconditions.checkState(
                    quad.getVertices()[baseOffset + colorOffset] == color,
                    "All vertices in a quad must have the same color, otherwise we need changes in BMS"
            );
        }
        return new Polygon<>(vertices, new ExtraQuadData(
                quad.getSprite(),
                new Vector4f(
                        (color & 255) / 255f,
                        ((color >> 8) & 255) / 255f,
                        ((color >> 16) & 255) / 255f,
                        (color >> 24) / 255f
                )
        )
        );
    }

    /**
     * Converts the given BlockModelSplitter polygon into a Minecraft quad after applying the given transformation.
     * Note that this function may only be called if poly is a quad, i.e. has 4 vertices.
     */
    public static BakedQuad toBakedQuad(Polygon<ExtraQuadData> poly, ModelState transform) {
        Transformation rotation = transform.getRotation().blockCenterToCorner();
        List<Vertex> points = poly.getPoints();
        ExtraQuadData data = poly.getTexture();
        Preconditions.checkArgument(points.size() == 4);
        Mutable<BakedQuad> quadStorage = new MutableObject<>();
        VertexConsumer consumer = new QuadBakingVertexConsumer(quadStorage::setValue);
        Vector3f normal = new Vector3f();
        for (Vertex v : points) {
            Vector4f pos = new Vector4f();
            pos.set(toArray(v.position(), 4));
            normal.set(toArray(v.normal(), 3));
            rotation.transformPosition(pos);
            rotation.transformNormal(normal);
            pos.mul(1 / pos.w);
            final double epsilon = 1e-5;
            for (int i = 0; i < 2; ++i) {
                if (Math.abs(i - pos.x()) < epsilon) pos.setComponent(0, i);
                if (Math.abs(i - pos.y()) < epsilon) pos.setComponent(1, i);
                if (Math.abs(i - pos.z()) < epsilon) pos.setComponent(2, i);
            }
            consumer.vertex(pos.x, pos.y, pos.z)
                    .normal(normal.x, normal.y, normal.z)
                    .uv((float) v.uv().u(), (float) v.uv().v())
                    .color(data.color.x(), data.color.y(), data.color.z(), data.color.w())
                    .endVertex();
        }
        return Preconditions.checkNotNull(quadStorage.getValue());
    }

    private static int getOffset(VertexFormatElement element) {
        int offset = 0;
        for (VertexFormatElement e : DefaultVertexFormat.BLOCK.getElements())
            if (e == element)
                return offset / 4;
            else
                offset += e.getByteSize();
        throw new IllegalStateException("Did not find element with usage " + element.getUsage().name() + " and type " + element.getType().name());
    }

    private static float[] toArray(Vec3d vec, int length) {
        float[] ret = new float[length];
        for (int i = 0; i < 3; ++i)
            ret[i] = (float) vec.get(i);
        for (int i = 3; i < length; ++i)
            ret[i] = 1;
        return ret;
    }

    public record ExtraQuadData(TextureAtlasSprite sprite, Vector4f color) {
    }
}