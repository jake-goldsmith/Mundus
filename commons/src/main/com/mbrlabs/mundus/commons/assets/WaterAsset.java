package com.mbrlabs.mundus.commons.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.mbrlabs.mundus.commons.assets.meta.Meta;
import com.mbrlabs.mundus.commons.water.Water;

import java.util.Map;

public class WaterAsset extends Asset {

    public Water water;
    public TextureAsset waterTexture;
    public TextureAsset dudvTexture;
    public TextureAsset normalMapTexture;

    public WaterAsset(Meta meta, FileHandle assetFile) {
        super(meta, assetFile);
    }

    @Override
    public void load() {
        water = new Water(meta.getWater().getSize());
        water.init();
    }

    @Override
    public void resolveDependencies(Map<String, Asset> assets) {
        String id = meta.getWater().getDudvMap();
        if (id != null && assets.containsKey(id)) {
            dudvTexture = (TextureAsset) assets.get(id);
        }

        id = meta.getWater().getNormalMap();
        if (id != null && assets.containsKey(id)) {
            normalMapTexture = (TextureAsset) assets.get(id);
        }
    }

    @Override
    public void applyDependencies() {
        if (waterTexture != null) {
            water.setWaterReflection(waterTexture.getTexture());
        }

        if (dudvTexture != null) {
            water.setDudvTexture(dudvTexture.getTexture());
        }

        if (normalMapTexture != null) {
            water.setNormalMap(normalMapTexture.getTexture());
        }
    }

    public void setWaterReflectionTexture(Texture texture){
       water.setWaterReflection(texture);
    }

    public void setWaterRefractionTexture(Texture texture){
        water.setWaterRefractionTexture(texture);
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean usesAsset(Asset assetToCheck) {
        return false;
    }

}
