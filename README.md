## BlockModelSplitter

This library is intended to be used in the context of Minecraft modding: Some mods add "multiblocks", structures larger
than a single block which are rendered as a single model. The most common approach to this is to have a single block of
the structure render the entire model and use empty models for all other blocks. This approach however causes issues
with chunk culling and block lighting. As an alternative, the model can be split into block-sized "submodels", which are
then rendered by the block physically containing them. This library is meant to perform the conversion from a "full"
model to the submodels automatically at runtime. Additionally, it provides a "hackable" OBJ loader which can be used
when direct access to e.g. obj groups is required.  
While the library is mainly intended for use with Minecraft, it does not depend on Minecraft by itself and can be used
in other contexts.

### Usage

The general flow when using this library in a Minecraft context is as follows:

1. Create an `OBJModel<T>` of the model to be split up. The type `T` is used to represent application-specific
   additional data attached to each face which has to be propagated to the faces obtained from it by splitting. In
   Minecraft modding this would e.g. be the `TextureAtlasSprite` and the color of a `BakedQuad`. Despite the name this
   class can be created from an arbitrary list of `Polygon<T>`s. Alternatively it can be created from an OBJ-format file
   using `OBJModel::readFromStream`.
2. Split the `OBJModel<T>` into the individual blocks using `ModelSplitter::splitModel`. This splits the original faces
   into the subfaces contained within each unit cube in the bounding box of the model.
3. Group together certain blocks using `ModelClumper::clumpModel`: In many cases parts of the original model pass
   through blocks that are not part of the multiblock structure (e.g. on diagonals/inner corners). If the data from
   the `SplitModel<T>` is used directly these parts of the model would simply not be rendered, resulting in holes in the
   model. To fix this the `ClumpedModel<T>` also accepts the set of blocks (in the same coordinates as the original
   model) present in the multiblock. Quads that are contained in blocks that are not present are reassigned to the
   closest block present in the multiblock.
4. If required by the engine (as in the case in Minecraft), the faces of the computed submodels can be converted into
   quads (4-vertex faces) using `ObjModel<T>::quadify`.

Example code for usage with Minecraft (using official Mojang mappings) is provided
in [`minecraft-example/MinecraftExample.java`](minecraft-example/MinecraftExample.java). Note that this only covers the
actual splitting process; you will need additional, mod-loader-dependant code to use these split models for your blocks.
It is recommended that the splitting is only performed once and then cached to minimize the performance impact.
