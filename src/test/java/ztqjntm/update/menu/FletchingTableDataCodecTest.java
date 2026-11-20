// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ztqjntm.update.GameTestBootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 存档 codec：v1（共享容器，只有 {@code items}）要能读进来，并且以后一直按 v2 写出去。
 */
class FletchingTableDataCodecTest {

    private static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    private static String roundTrip(String json) {
        var parsed = FletchingTableData.CODEC.parse(ops(), JsonParser.parseString(json));
        assertTrue(parsed.result().isPresent(), () -> "解析失败: " + parsed.error().orElse(null));
        return FletchingTableData.CODEC.encodeStart(ops(), parsed.result().get())
                .result()
                .orElseThrow()
                .toString();
    }

    @BeforeAll
    static void bootstrap() {
        GameTestBootstrap.bootstrap();
    }

    @Test
    void readsV1AndWritesV2() {
        String encoded = roundTrip("""
                {"tables":[{"pos":[1,64,2],"items":[{"id":"minecraft:flint","count":3}]}]}
                """);

        assertTrue(encoded.contains("\"version\":2"), encoded);
        assertTrue(encoded.contains("\"owners\""), encoded);
        // v1 没有归属人，读档时应挂在全零 UUID 下等玩家认领
        assertTrue(encoded.contains("00000000-0000-0000-0000-000000000000"), encoded);
        assertTrue(encoded.contains("\"minecraft:flint\""), encoded);
    }

    @Test
    void readsV2Unchanged() {
        String encoded = roundTrip("""
                {"version":2,"tables":[{"pos":[3,70,4],"owners":[
                  {"owner":"11111111-2222-3333-4444-555555555555",
                   "items":[{"id":"minecraft:feather","count":2}]}
                ]}]}
                """);

        assertTrue(encoded.contains("\"version\":2"), encoded);
        assertTrue(encoded.contains("11111111-2222-3333-4444-555555555555"), encoded);
        assertTrue(encoded.contains("\"minecraft:feather\""), encoded);
    }
}
