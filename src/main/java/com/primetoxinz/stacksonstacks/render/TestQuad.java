package com.primetoxinz.stacksonstacks.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

/**
 * Created by tyler on 7/14/16.
 */
public class TestQuad extends UnpackedBakedQuad {
    public TestQuad(float[][][] unpackedData, int tint, EnumFacing orientation, TextureAtlasSprite texture, boolean applyDiffuseLighting, VertexFormat format) {
        super(unpackedData, tint, orientation, texture, applyDiffuseLighting, format);
    }

    public static class Builder implements IVertexConsumer
    {
        private final VertexFormat format;
        private final float[][][] unpackedData;
        private int tint = -1;
        private EnumFacing orientation;
        private TextureAtlasSprite texture;
        private boolean applyDiffuseLighting = true;

        private int vertices = 0;
        private int elements = 0;
        private boolean full = false;
        private boolean contractUVs = false;

        public Builder(VertexFormat format)
        {
            this.format = format;
            unpackedData = new float[4][format.getElementCount()][4];
        }

        public VertexFormat getVertexFormat()
        {
            return format;
        }

        public void setContractUVs(boolean value)
        {
            this.contractUVs = value;
        }
        public void setQuadTint(int tint)
        {
            this.tint = tint;
        }

        public void setQuadOrientation(EnumFacing orientation)
        {
            this.orientation = orientation;
        }

        public void setTexture(TextureAtlasSprite texture)
        {
            this.texture = texture;
        }

        public void setApplyDiffuseLighting(boolean diffuse)
        {
            this.applyDiffuseLighting = diffuse;
        }

        public int getTint() {
            return tint;
        }
        public void put(int element, float... data)
        {
            for(int i = 0; i < 4; i++)
            {
                if(i < data.length)
                {
                    unpackedData[vertices][element][i] = data[i];
                }
                else
                {
                    unpackedData[vertices][element][i] = 0;
                }
            }
            elements++;
            if(elements == format.getElementCount())
            {
                vertices++;
                elements = 0;
            }
            if(vertices == 4)
            {
                full = true;
            }
        }

        private final float eps = 1f / 0x100;

        public UnpackedBakedQuad build()
        {
            if(!full)
            {
                throw new IllegalStateException("not enough data");
            }
            if(contractUVs)
            {
                float tX = texture.getOriginX() / texture.getMinU();
                float tY = texture.getOriginY() / texture.getMinV();
                float tS = tX > tY ? tX : tY;
                float ep = 1f / (tS * 0x100);
                int uve = 0;
                while(uve < format.getElementCount())
                {
                    VertexFormatElement e = format.getElement(uve);
                    if(e.getUsage() == VertexFormatElement.EnumUsage.UV && e.getIndex() == 0)
                    {
                        break;
                    }
                    uve++;
                }
                if(uve == format.getElementCount())
                {
                    throw new IllegalStateException("Can't contract UVs: format doesn't contain UVs");
                }
                float[] uvc = new float[4];
                for(int v = 0; v < 4; v++)
                {
                    for(int i = 0; i < 4; i++)
                    {
                        uvc[i] += unpackedData[v][uve][i] / 4;
                    }
                }
                for(int v = 0; v < 4; v++)
                {
                    for (int i = 0; i < 4; i++)
                    {
                        float uo = unpackedData[v][uve][i];
                        float un = uo * (1 - eps) + uvc[i] * eps;
                        float ud = uo - un;
                        float aud = ud;
                        if(aud < 0) aud = -aud;
                        if(aud < ep) // not moving a fraction of a pixel
                        {
                            float udc = uo - uvc[i];
                            if(udc < 0) udc = -udc;
                            if(udc < 2 * ep) // center is closer than 2 fractions of a pixel, don't move too close
                            {
                                un = (uo + uvc[i]) / 2;
                            }
                            else // move at least by a fraction
                            {
                                un = uo + (ud < 0 ? ep : -ep);
                            }
                        }
                        unpackedData[v][uve][i] = un;
                    }
                }
            }
            return new TestQuad(unpackedData, tint, orientation, texture, applyDiffuseLighting, format);
        }
    }

}