package eutros.multiblocktweaker.gregtech.cuberenderer;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.google.common.base.Preconditions;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.LightMapOperation;
import gregtech.api.render.Textures;
import gregtech.common.ConfigHolder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SidedCubeRenderer implements ICubeRenderer {

    private Map<EnumFacing, String> paths;
    private Map<EnumFacing, TextureAtlasSprite> sprites;
    private Map<EnumFacing, TextureAtlasSprite> spritesEmissive;
    private TextureAtlasSprite particles;

    public SidedCubeRenderer(Map<EnumFacing, String> paths) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.paths = paths;
            this.sprites = new EnumMap<>(EnumFacing.class);
            this.spritesEmissive = new EnumMap<>(EnumFacing.class);
            if (MapHolder.map != null) {
                for (EnumFacing facing : EnumFacing.values()) {
                    sprites.put(facing, MapHolder.map.getAtlasSprite(new ResourceLocation(paths.get(facing)).toString()));
                    TextureAtlasSprite emissiveSprite = MapHolder.map.getAtlasSprite(new ResourceLocation(paths.get(facing) + "_emissive").toString());
                    if (emissiveSprite == MapHolder.map.getMissingSprite()) {
                        emissiveSprite = null;
                    }
                    spritesEmissive.put(facing, emissiveSprite);
                }
                particles = sprites.get(EnumFacing.UP);
            } else {
                MinecraftForge.EVENT_BUS.register(this);
            }
        }
    }

    public static <T> EnumMap<EnumFacing, T> fillBlanks(Map<EnumFacing, T> map) {
        Preconditions.checkNotNull(
                map.get(EnumFacing.UP),
                "UP side has no texture! " +
                "Consider using mods.gregtech.multiblock.Builder#withTexture to specify the texture explicitly."
        );
        EnumMap<EnumFacing, T> retMap = new EnumMap<>(map);

        retMap.computeIfAbsent(EnumFacing.DOWN, k -> retMap.get(EnumFacing.UP));

        retMap.computeIfAbsent(EnumFacing.NORTH,
                a -> retMap.computeIfAbsent(EnumFacing.EAST,
                        b -> retMap.computeIfAbsent(EnumFacing.SOUTH,
                                c -> retMap.computeIfAbsent(EnumFacing.WEST, k -> retMap.get(EnumFacing.UP)))));

        retMap.computeIfAbsent(EnumFacing.WEST, k -> retMap.get(EnumFacing.NORTH));
        retMap.computeIfAbsent(EnumFacing.SOUTH, k -> retMap.get(EnumFacing.NORTH));
        retMap.computeIfAbsent(EnumFacing.EAST, k -> retMap.get(EnumFacing.NORTH));
        return retMap;
    }

    public SidedCubeRenderer(IBlockState state) {
        if (FMLCommonHandler.instance().getSide().isServer())
            return;
        BlockRendererDispatcher brd = Minecraft.getMinecraft().getBlockRendererDispatcher();
        IBakedModel model = brd.getModelForState(state);
        long rand = new Random().nextLong();
        particles = model.getParticleTexture();
        sprites = fillBlanks(
                Stream.concat(Stream.of(new EnumFacing[] { null }),
                        Arrays.stream(EnumFacing.values()))
                        .map(f -> model.getQuads(state, f, rand))
                        .map(List::stream)
                        .map(Stream::findFirst)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toMap(BakedQuad::getFace, BakedQuad::getSprite))
        );
        this.spritesEmissive = new EnumMap<>(EnumFacing.class);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void preStitch(TextureStitchEvent.Pre evt) {
        for (EnumFacing facing : EnumFacing.values()) {
            sprites.put(facing, evt.getMap().registerSprite(new ResourceLocation(paths.get(facing))));
            ResourceLocation emissiveLocation = new ResourceLocation(paths.get(facing) + "_emissive");
            if (MapHolder.isTextureExist(emissiveLocation)) {
                spritesEmissive.put(facing, evt.getMap().registerSprite(emissiveLocation));
            }
        }
        particles = sprites.get(EnumFacing.UP);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public TextureAtlasSprite getParticleSprite() {
        return particles;
    }

    @SideOnly(Side.CLIENT)
    public void renderSided(EnumFacing side, Matrix4 translation, Cuboid6 bounds, CCRenderState renderState, IVertexOperation[] pipeline) {
        Textures.renderFace(renderState, translation, pipeline, side, bounds, sprites.get(side));
        if (spritesEmissive.get(side) != null) {
            if (ConfigHolder.client.machinesEmissiveTextures) {
                IVertexOperation[] lightPipeline = ArrayUtils
                        .add(pipeline, new LightMapOperation(240, 240));
                Textures.renderFaceBloom(renderState, translation, lightPipeline, side, bounds, spritesEmissive.get(side));
            } else Textures.renderFace(renderState, translation, pipeline, side, bounds, spritesEmissive.get(side));
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render(CCRenderState state, Matrix4 translate, IVertexOperation[] ops, Cuboid6 cuboid) {
        for (EnumFacing side : EnumFacing.values()) {
            renderSided(side, translate, cuboid, state, ops);
        }
    }

}
