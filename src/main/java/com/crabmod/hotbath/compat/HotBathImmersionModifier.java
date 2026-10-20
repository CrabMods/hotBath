package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Function;

/**
 * Temperature modifier that applies a fixed temperature when the player is inside a Hot Bath block.
 * Only applies to built-in hot baths and custom fluids with temperature >= 35°C.
 */
public class HotBathImmersionModifier extends TempModifier {

    public HotBathImmersionModifier() {
        super();
    }

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        return CompatManager.safeEventCall("cold_sweat", "HotBathImmersionModifier.calculate", () -> {
            // We only modify the WORLD temperature trait
            if (trait != Temperature.Trait.WORLD) {
                return (Function<Double, Double>) (temp -> temp);
            }

            // Only apply to players (CustomFluidHandler methods expect Player type)
            if (!(entity instanceof Player player)) {
                return (Function<Double, Double>) (temp -> temp);
            }

            Level level = entity.level();

            // Check if the entity is inside any bath block (hot or cold)
            if (com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(player)&&CustomFluidHandler.isPlayerInHotBathBlock(player)) {
                // Get the actual bath temperature (may vary for custom fluids)
                float bathTempC = CustomFluidHandler.getBathTemperature(player);
                if (bathTempC <= 0) {
                    return (Function<Double, Double>) (temp -> temp);
                }

                // Calculate target temperature in Minecraft units
                double targetTempMC = Temperature.convert(bathTempC, Temperature.Units.C, Temperature.Units.MC, true);

                // Get the natural biome temperature at this position
                double worldTempMC = WorldHelper.getBiomeTemperature(level, level.getBiome(entity.blockPosition()));

                double finalTemp;
                if (bathTempC >= 35.0f) {
                    // Hot baths: use the higher of bath or biome temperature (warming effect)
                    finalTemp = Math.max(targetTempMC, worldTempMC);
                } else {
                    // Cold baths: use the bath temperature directly (cooling effect)
                    finalTemp = targetTempMC;
                }

                return (Function<Double, Double>) (temp -> finalTemp);
            }

            // If not inside a hot bath, return identity function (no change)
            return (Function<Double, Double>) (temp -> temp);
        }, temp -> temp);
    }
}
