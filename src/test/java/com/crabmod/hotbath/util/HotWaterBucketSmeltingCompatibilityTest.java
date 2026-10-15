package com.crabmod.hotbath.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HotWaterBucketSmeltingCompatibilityTest {
    private JsonObject recipe(String name) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/hotbath/recipes/"+name+".json"))).getAsJsonObject();
    }
    @Test void keepsBothHeatingAndPurificationAvailableWithoutPolymorph() throws IOException {
        var ordinary=recipe("hot_water_bucket");var compatible=recipe("hot_water_bucket_thirst");
        var absent=ordinary.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("forge:not",absent.get("type").getAsString());
        assertEquals("thirst",absent.getAsJsonObject("value").get("modid").getAsString());
        var present=compatible.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("forge:mod_loaded",present.get("type").getAsString());
        assertEquals("thirst",present.get("modid").getAsString());
        assertFalse(ordinary.toString().contains("polymorph"));
        assertFalse(compatible.toString().contains("polymorph"));
        assertEquals("minecraft:smelting",compatible.get("type").getAsString());
        var ingredient=compatible.getAsJsonObject("ingredient");
        assertEquals("forge:difference",ingredient.get("type").getAsString());
        assertEquals("minecraft:water_bucket",ingredient.getAsJsonObject("base").get("item").getAsString());
        var excluded=ingredient.getAsJsonArray("subtracted");
        var purities=new HashSet<Integer>();
        for(var entry:excluded) {
            var item=entry.getAsJsonObject();
            assertEquals("forge:nbt",item.get("type").getAsString());
            purities.add(item.getAsJsonObject("nbt").get("Purity").getAsInt());
        }
        assertEquals(Set.of(-1,0,1,2),purities);
        assertTrue(compatible.toString().contains("hotbath:hot_water_bucket"));
    }
}
