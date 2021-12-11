package eutros.multiblocktweaker.gregtech.cuberenderer;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.resources.ResourceHelper;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.LightMapOperation;
import gregtech.api.render.Textures;
import gregtech.common.ConfigHolder;
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

public class BasicCubeRenderer implements ICubeRenderer {

    private String path;
    private TextureAtlasSprite sprite = null;
    private TextureAtlasSprite spriteEmissive = null;

    public BasicCubeRenderer(String path) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.path = path;
            if (MapHolder.map != null) {
                sprite = MapHolder.map.getAtlasSprite(new ResourceLocation(path).toString());
                spriteEmissive = MapHolder.map.getAtlasSprite(new ResourceLocation(path + "_emissive").toString());
                if (spriteEmissive == MapHolder.map.getMissingSprite()) {
                    spriteEmissive = null;
                }
            } else {
                MinecraftForge.EVENT_BUS.register(this);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public TextureAtlasSprite getParticleSprite() {
        return sprite;
    }

    @SideOnly(Side.CLIENT)
    @Override // why why why why why
    public void render(CCRenderState state, Matrix4 translate, IVertexOperation[] ops, Cuboid6 cuboid) {
        for (EnumFacing side : EnumFacing.values()) {
            renderSided(side, translate, cuboid, state, ops);
        }
    }

    @SideOnly(Side.CLIENT)
    public void renderSided(EnumFacing side, Matrix4 translation, Cuboid6 bounds, CCRenderState renderState, IVertexOperation[] pipeline) {
        Textures.renderFace(renderState, translation, pipeline, side, bounds, sprite);
        if (spriteEmissive != null) {
            if (ConfigHolder.client.machinesEmissiveTextures) {
                IVertexOperation[] lightPipeline = ArrayUtils
                        .add(pipeline, new LightMapOperation(240, 240));
                Textures.renderFaceBloom(renderState, translation, lightPipeline, side, bounds, spriteEmissive);
            } else Textures.renderFace(renderState, translation, pipeline, side, bounds, spriteEmissive);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void preStitch(TextureStitchEvent.Pre evt) {
        if (sprite == null) {
            sprite = evt.getMap().registerSprite(new ResourceLocation(path));
            ResourceLocation emissiveLocation = new ResourceLocation(path + "_emissive");
            if (MapHolder.isTextureExist(emissiveLocation))  {
                spriteEmissive = evt.getMap().registerSprite(emissiveLocation);
            }
        }
    }

}
