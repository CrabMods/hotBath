package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Separates native behaviour overrides from new custom fluid identities on clients. */
public final class BathCatalogSync {
    private BathCatalogSync() {}

    public static boolean apply(List<String> jsons) {
        try {
            Map<ResourceLocation,BuiltinBathDefinition> builtins=new HashMap<>();
            List<String> builtinJsons=new ArrayList<>();
            List<CustomFluidDefinition> custom=new ArrayList<>();
            for(String text:jsons) {
                JsonObject json=JsonParser.parseString(text).getAsJsonObject();
                ResourceLocation id=new ResourceLocation(json.get("id").getAsString());
                if(BuiltinBathDefinitions.isBuiltin(id)) {
                    if(builtins.put(id,BuiltinBathDefinition.parse(json))!=null)
                        throw new IllegalArgumentException("Duplicate native bath: "+id);
                    builtinJsons.add(text);
                } else {
                    custom.add(CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE,json).result()
                            .orElseThrow(()->new IllegalArgumentException("Invalid custom bath: "+id)));
                }
            }
            var nextBuiltin=BuiltinBathDefinitions.catalog(builtins,builtinJsons);
            // Publish only after the complete packet has been decoded and validated.
            BuiltinBathDefinitions.replaceClient(nextBuiltin);
            CustomFluidRegistry.clearClientSide();
            for(var definition:custom) {
                CustomFluidRegistry.registerClientSide(definition);
                definition.registerTranslations();
            }
            HotBath.LOGGER.info("Client registered {} custom baths and {} native bath profiles",custom.size(),builtins.size());
            return true;
        } catch(RuntimeException error) {
            HotBath.LOGGER.error("Rejected invalid server bath catalog: {}",error.getMessage());
            return false;
        }
    }
}
