package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class valk_BrynCore extends BaseHullMod {
    public static final float WEAPON_RANGE_BONUS_PERCENT = 10f;
    public static final float PD_RANGE_BONUS_PERCENT = 5f;
    private static final float WEAPON_FLUX_MULTIPLIER = 0.95f;
    private static final String ENIGMA_MANUFACTURER = "Enigma Company";

    private static final Map<WeaponAPI.WeaponSize, Integer> VALK_WEAPON_OP_REDUCTION_MAP =
            new EnumMap<WeaponAPI.WeaponSize, Integer>(WeaponAPI.WeaponSize.class);

    static {
        VALK_WEAPON_OP_REDUCTION_MAP.put(WeaponAPI.WeaponSize.SMALL, 1);
        VALK_WEAPON_OP_REDUCTION_MAP.put(WeaponAPI.WeaponSize.MEDIUM, 2);
        VALK_WEAPON_OP_REDUCTION_MAP.put(WeaponAPI.WeaponSize.LARGE, 3);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, WEAPON_FLUX_MULTIPLIER);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, WEAPON_FLUX_MULTIPLIER);
        stats.addListener(new BrynWeaponOPModifier());
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        if (!ship.hasListenerOfClass(BrynCoreRangeModifier.class)) {
            ship.addListener(new BrynCoreRangeModifier());
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "5%";
        if (index == 1) return "10% for non-PD weapons; 5% for point-defense weapons; missiles excluded";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize,
                                          ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 8f;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("Bryn Integration", Alignment.MID, pad);
        tooltip.addPara("The core now favors precision integration over raw output. Ballistic and energy weapon "
                + "flux cost is reduced by %s, and only Enigma Company non-missile weapons receive the range bonus.",
                pad, highlight, "5%");
        tooltip.addPara("Enigma Company non-PD, non-missile weapon range is increased by %s. Point-defense weapons receive "
                + "a smaller %s range bonus.", pad, highlight, "10%", "5%");
        tooltip.addPara("Valkyrian non-missile weapon OP costs are reduced by %s/%s/%s for small, medium, and large mounts. "
                + "Non-Valkyrian weapons and missile weapons receive no OP discount.",
                pad, highlight, "1", "2", "3");
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    public static boolean isValkWeapon(WeaponSpecAPI spec) {
        return spec != null && spec.getWeaponId() != null && spec.getWeaponId().startsWith("valk");
    }

    public static boolean isEnigmaWeapon(WeaponSpecAPI spec) {
        return spec != null && ENIGMA_MANUFACTURER.equals(spec.getManufacturer());
    }

    public static class BrynCoreRangeModifier implements WeaponBaseRangeModifier {
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon == null || weapon.getType() == WeaponAPI.WeaponType.MISSILE) return 0f;
            if (!isEnigmaWeapon(weapon.getSpec())) return 0f;
            if (weapon.hasAIHint(WeaponAPI.AIHints.PD) || weapon.hasAIHint(WeaponAPI.AIHints.PD_ALSO)) {
                return PD_RANGE_BONUS_PERCENT;
            }
            return WEAPON_RANGE_BONUS_PERCENT;
        }

        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }
    }

    public static class BrynWeaponOPModifier implements WeaponOPCostModifier {
        public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
            if (!isValkWeapon(weapon)) return currCost;
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) return currCost;
            Integer reduction = VALK_WEAPON_OP_REDUCTION_MAP.get(weapon.getSize());
            if (reduction == null) return currCost;
            return Math.max(0, currCost - reduction);
        }
    }
}
