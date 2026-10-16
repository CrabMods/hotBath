package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidManager;
import com.crabmod.hotbath.custom_fluid.CustomFluidNetworking;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CustomFluidSyncSnapshotTest {
    @GameTest(template = "empty_1x1", batch = "custom_fluid_sync", timeoutTicks = 40)
    public static void test_server_snapshot_ignores_client_cache(GameTestHelper helper) throws Exception {
        var source = new ArrayList<>(CustomFluidManager.getInstance().getAllFluidDefinitions());
        source.sort(Comparator.comparing(d -> d.id().toString()));
        check(source.size() >= 3, "Original server definition fixtures are missing");
        var previous = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        var expected = content(source.stream().map(d -> json(d).toString()).toList());
        try {
            // Every prefix models a client packet paused during cache replacement.
            for (int count = 0; count <= source.size(); count++) {
                CustomFluidRegistry.clearClientSide();
                source.subList(0, count).forEach(CustomFluidRegistry::registerClientSide);
                verifySnapshot(expected);
            }
            CustomFluidRegistry.onFluidsReloaded(source);
            var stale = json(source.get(0));
            stale.addProperty("color", source.get(0).color() ^ 0xffffff);
            CustomFluidRegistry.registerClientSide(CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, stale)
                    .result().orElseThrow());
            verifySnapshot(expected);

            CustomFluidRegistry.onFluidsReloaded(source);
            var foreign = json(source.get(0));
            foreign.addProperty("id", "hotbath:sync_client_only_test");
            CustomFluidRegistry.registerClientSide(CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, foreign)
                    .result().orElseThrow());
            verifySnapshot(expected);
            check(content(CustomFluidManager.getInstance().getAllFluidDefinitions().stream()
                    .map(d -> json(d).toString()).toList()).equals(expected), "Server definitions changed");
        } finally {
            CustomFluidRegistry.onFluidsReloaded(previous);
            check(new HashSet<>(CustomFluidRegistry.getAllDefinitions()).equals(new HashSet<>(previous)),
                    "Runtime cache restoration failed");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void verifySnapshot(Map<String, JsonObject> expected) throws Exception {
        var cache = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        // Invoke the production serializer without widening the production API for tests.
        var method = CustomFluidNetworking.class.getDeclaredMethod("serializeAllFluids");
        method.setAccessible(true);
        var payload = (List<String>) method.invoke(null);
        check(content(payload).equals(expected), "Server snapshot used partial or stale client data");
        check(roundTrip(payload).equals(payload), "Native packet codec changed the snapshot");
        check(new HashSet<>(CustomFluidRegistry.getAllDefinitions()).equals(new HashSet<>(cache)),
                "Serialization changed the runtime cache");
    }

    private static List<String> roundTrip(List<String> values) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new CustomFluidNetworking.SyncCustomFluidsPayload(values);
            CustomFluidNetworking.SyncCustomFluidsPayload.STREAM_CODEC.encode(buf, packet);
            var decoded = CustomFluidNetworking.SyncCustomFluidsPayload.STREAM_CODEC.decode(buf);
            check(buf.readableBytes() == 0, "Trailing payload bytes");
            return decoded.fluidJsons();
        } finally {
            buf.release();
        }
    }

    private static Map<String, JsonObject> content(List<String> values) {
        var result = new TreeMap<String, JsonObject>();
        for (var value : values) {
            var json = JsonParser.parseString(value).getAsJsonObject();
            check(result.put(json.get("id").getAsString(), json) == null, "Duplicate sync ID");
        }
        return result;
    }

    private static JsonObject json(CustomFluidDefinition definition) {
        return CustomFluidDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .result().orElseThrow().getAsJsonObject();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
