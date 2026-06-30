package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.Map;

public class valk_HolyCovenant extends BaseHullMod {
    public static final String BUFF_KEY = "valk_HolyCovenant_buff";
    public static final float CAPITAL_EFFECT_RANGE = 3000f;
    public static final float CAPITAL_EFFECT_THRESHOLD_RANGE = 2700f;
    public static final float MAX_DMG_TO_NON_CAPITAL_BONUS = 10f;
    public static final float MAX_HARD_FLUX_BONUS = 10f;
    public static final float MAX_RECOIL_BONUS = 60f;
    public static final float MAX_MISSILE_SPEED_BONUS = 25f;
    public static final float MAX_MISSILE_DMG_TO_CAPITAL_BONUS = 10f;
    public static final float MAX_SPEED_BONUS = 15f;
    public static final float MAX_MANEUVERABILITY_BONUS = 25f;
    public static final Map<ShipAPI.HullSize, Float> DP_THRESHOLD_MAP =
            new EnumMap<ShipAPI.HullSize, Float>(ShipAPI.HullSize.class);
    public static final float DP_THRESHOLD_FIGHTER = 20f;

    static {
        DP_THRESHOLD_MAP.put(ShipAPI.HullSize.FRIGATE, 10f);
        DP_THRESHOLD_MAP.put(ShipAPI.HullSize.DESTROYER, 20f);
        DP_THRESHOLD_MAP.put(ShipAPI.HullSize.CRUISER, 40f);
        DP_THRESHOLD_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 60f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        try {
            Class<?> listenerClass = Class.forName("data.hullmods.valk_HolyCovenant$HolyCovenantListener");
            if (!ship.hasListenerOfClass(listenerClass)) {
                Constructor<?> constructor = listenerClass.getConstructor(ShipAPI.class);
                ship.addListener(constructor.newInstance(ship));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to attach Holy Covenant listener", ex);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize,
                                          ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("Bonus Scaling", Alignment.MID, pad);
        tooltip.addPara("The host capital ship always receives the full value of its own command aura. "
                + "Allied ships and fighter wings inside %s range receive scaled bonuses based on deployment cost "
                + "or fighter OP cost; non-Valkyrian ships count as half value for this calculation.",
                pad, highlight, "3000");
        tooltip.addPara("Maximum scaling thresholds: frigates %s DP, destroyers %s DP, cruisers %s DP, "
                + "capital ships %s DP, fighter wings %s OP.", pad, highlight,
                "10", "20", "40", "60", "20");

        tooltip.addSectionHeading("Doctrine Effects", Alignment.MID, pad);
        tooltip.setBulletedListMode(" -");
        tooltip.addPara("All Holy Covenant ships: damage to fighters, frigates, destroyers, and cruisers increased "
                + "by %s, capped at %s.", pad, highlight, "3%", "10%");

        String baseHullId = getBaseHullId(ship);
        if (baseHullId == null) {
            tooltip.addPara("Battle covenant hulls: hard-flux dissipation while shields are up +%s, capped at %s; "
                    + "energy and ballistic recoil reduced by %s, capped at %s.", pad, highlight,
                    "2.5%", "10%", "15%", "60%");
            tooltip.addPara("Missile covenant hulls: missile flight speed +%s, capped at %s; missile damage to "
                    + "capital ships +%s, capped at %s.", pad, highlight, "10%", "25%", "5%", "10%");
            tooltip.addPara("Pursuit covenant hulls: maximum combat speed +%s, capped at %s; maneuverability +%s, "
                    + "capped at %s.", pad, highlight, "5%", "15%", "10%", "25%");
        } else if (isBattleCovenant(baseHullId)) {
            tooltip.addPara("This hull follows the battle covenant: hard-flux dissipation while shields are up +%s, "
                    + "capped at %s; energy and ballistic recoil reduced by %s, capped at %s.",
                    pad, highlight, "2.5%", "10%", "15%", "60%");
        } else if (isMissileCovenant(baseHullId)) {
            tooltip.addPara("This hull follows the missile covenant: missile flight speed +%s, capped at %s; "
                    + "missile damage to capital ships +%s, capped at %s.",
                    pad, highlight, "10%", "25%", "5%", "10%");
        } else if (isPursuitCovenant(baseHullId)) {
            tooltip.addPara("This hull follows the pursuit covenant: maximum combat speed +%s, capped at %s; "
                    + "maneuverability +%s, capped at %s.",
                    pad, highlight, "5%", "15%", "10%", "25%");
        }
        tooltip.setBulletedListMode(null);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "3000";
        return super.getDescriptionParam(index, hullSize);
    }

    @Override
    public Color getBorderColor() {
        return new Color(255, 89, 39);
    }

    @Override
    public Color getNameColor() {
        return new Color(255, 173, 0);
    }

    private static String getBaseHullId(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return null;
        return ship.getHullSpec().getBaseHullId();
    }

    private static boolean isBattleCovenant(String baseHullId) {
        return "valk_yoshura".equals(baseHullId)
                || "valk_yoshura_prime".equals(baseHullId)
                || "valk_radiance".equals(baseHullId);
    }

    private static boolean isMissileCovenant(String baseHullId) {
        return "valk_yoshura_VI".equals(baseHullId)
                || "valk_vatican_I".equals(baseHullId);
    }

    private static boolean isPursuitCovenant(String baseHullId) {
        return "valk_almire_III".equals(baseHullId)
                || "valk_cherberos".equals(baseHullId);
    }
}
