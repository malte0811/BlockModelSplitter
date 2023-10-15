package malte0811.modelsplitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import malte0811.modelsplitter.math.ModelSplitterVec3i;
import malte0811.modelsplitter.math.Vec3d;
import malte0811.modelsplitter.model.OBJModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelClumper {
    /**
     * Assigns parts of the model contained in cells/blocks which cannot render them to nearby cells which can render
     * them.
     *
     * @param splitModel the parts of the model by the cell/block position physically containing them
     * @param parts      the cell/block coordinates which can be used to render parts of the model
     * @param <Texture>  See {@link OBJModel}
     * @return the model to render for each of the coordinates passed in "parts"
     */
    public static <Texture> Map<ModelSplitterVec3i, OBJModel<Texture>> clumpModel(
            Map<ModelSplitterVec3i, OBJModel<Texture>> splitModel, Set<ModelSplitterVec3i> parts
    ) {
        Preconditions.checkArgument(!parts.isEmpty());
        Map<ModelSplitterVec3i, OBJModel<Texture>> clumpedParts = new HashMap<>();
        for (Map.Entry<ModelSplitterVec3i, OBJModel<Texture>> splitPart : splitModel.entrySet()) {
            final ModelSplitterVec3i originalTarget = splitPart.getKey();
            ModelSplitterVec3i target = originalTarget;
            OBJModel<Texture> translatedModel = splitPart.getValue();
            if (!parts.contains(target)) {
                int optDist = Integer.MAX_VALUE;
                for (ModelSplitterVec3i candidate : parts) {
                    int currentDist = candidate.distanceSq(originalTarget);
                    if (currentDist < optDist) {
                        optDist = currentDist;
                        target = candidate;
                    }
                }
                ModelSplitterVec3i translatedBy = originalTarget.subtract(target);
                translatedModel = translatedModel.translate(new Vec3d(translatedBy));
            }
            clumpedParts.merge(target, translatedModel, OBJModel::union);
        }
        return ImmutableMap.copyOf(clumpedParts);
    }
}
