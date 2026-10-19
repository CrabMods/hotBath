package com.crabmod.hotbath.custom_fluid;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Behaviour definitions only; never replaces registry objects or creates duplicate buckets. */
public final class BuiltinBathDefinitions {
    private static final Set<String> NAMES=Set.of("hot_water_fluid","milk_bath_fluid","herbal_bath_fluid",
            "honey_bath_fluid","peony_bath_fluid","rose_bath_fluid");
    public record Catalog(Map<ResourceLocation,BuiltinBathDefinition> definitions,List<String> jsons) {
        public Catalog {definitions=Map.copyOf(definitions);jsons=List.copyOf(jsons);}
    }
    private static final Catalog BUNDLED=loadDefaults();
    private static volatile Catalog server=BUNDLED;
    private static volatile Catalog client=BUNDLED;
    private BuiltinBathDefinitions() {}

    public static boolean isBuiltin(ResourceLocation id) {
        return id.getNamespace().equals("hotbath")&&NAMES.contains(id.getPath());
    }
    public static BuiltinBathDefinition get(String path) {
        return Objects.requireNonNull(server.definitions().get(new ResourceLocation("hotbath:"+path)),"Unknown built-in bath: "+path);
    }
    public static Optional<BuiltinBathDefinition> forFluid(Fluid fluid) {
        return forFluid(fluid,false);
    }
    public static Optional<BuiltinBathDefinition> forFluid(Fluid fluid,boolean clientSide) {
        if(fluid==null)return Optional.empty();
        if(fluid instanceof FlowingFluid flowing)fluid=flowing.getSource();
        ResourceLocation key=BuiltInRegistries.FLUID.getKey(fluid);
        return key==null?Optional.empty():Optional.ofNullable((clientSide?client:server).definitions().get(key));
    }
    public static float splashCleaning(net.minecraft.world.item.Item item) {
        ResourceLocation key=BuiltInRegistries.ITEM.getKey(item);
        BuiltinBathDefinition definition=null;
        if(key!=null&&key.getNamespace().equals("hotbath")&&key.getPath().startsWith("splash_")&&key.getPath().endsWith("_bottle")) {
            String name=key.getPath().substring(7,key.getPath().length()-7)+"_fluid";
            definition=server.definitions().get(new ResourceLocation("hotbath:"+name));
        }
        // Preserve the base splash item's existing behaviour for third-party subclasses.
        if(definition==null)definition=BUNDLED.definitions().get(new ResourceLocation("hotbath:hot_water_fluid"));
        return definition.splashDirtinessReduction();
    }
    public static Catalog catalog(Map<ResourceLocation,BuiltinBathDefinition> next,List<String> jsons) {
        if(next.size()!=NAMES.size()||next.keySet().stream().anyMatch(id->!isBuiltin(id)))
            throw new IllegalArgumentException("All six built-in bath definitions are required");
        return new Catalog(next,jsons);
    }
    public static void replace(Catalog next) {server=next;}
    public static void replaceClient(Catalog next) {client=next;}
    public static void resetClient() {client=BUNDLED;}
    public static List<String> serverJsons() {return server.jsons();}
    private static Catalog loadDefaults() {
        Map<ResourceLocation,BuiltinBathDefinition> result=new HashMap<>();
        List<String> jsons=new ArrayList<>();
        for(String name:NAMES) {
            String path="/data/hotbath/hotbath/custom_fluids/"+name+".json";
            try(InputStream stream=BuiltinBathDefinitions.class.getResourceAsStream(path)) {
                if(stream==null)throw new IllegalStateException("Missing bundled bath definition: "+path);
                var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
                var definition=BuiltinBathDefinition.parse(json);
                result.put(definition.id(),definition);
                jsons.add(json.toString());
            }catch(IOException error){throw new UncheckedIOException(error);}
        }
        return catalog(result,jsons);
    }
}
