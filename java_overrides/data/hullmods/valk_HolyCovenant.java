package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class valk_HolyCovenant extends BaseHullMod {
    public static final String BUFF_KEY = "valk_HolyCovenant_buff";
    public static final float CAPITAL_EFFECT_RANGE = 2200f;
    public static final float FULL_EFFECT_RANGE = 1000f;

    public static final float MAX_DMG_TO_SMALL_BONUS = 5f;
    public static final float MAX_HARD_FLUX_BONUS = 6f;
    public static final float MAX_RECOIL_BONUS = 25f;
    public static final float MAX_MISSILE_SPEED_BONUS = 15f;
    public static final float MAX_MISSILE_DMG_TO_CAPITAL_BONUS = 6f;
    public static final float MAX_SPEED_BONUS = 8f;
    public static final float MAX_MANEUVERABILITY_BONUS = 15f;

    private static final float COMMON_DMG_PER_SOURCE = 1.5f;
    private static final float BATTLE_HARD_FLUX_PER_SOURCE = 1.5f;
    private static final float BATTLE_RECOIL_PER_SOURCE = 8f;
    private static final float MISSILE_SPEED_PER_SOURCE = 5f;
    private static final float MISSILE_CAPITAL_DAMAGE_PER_SOURCE = 2.5f;
    private static final float PURSUIT_SPEED_PER_SOURCE = 3f;
    private static final float PURSUIT_MANEUVER_PER_SOURCE = 6f;

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
        if (!ship.hasListenerOfClass(HolyCovenantSourceListener.class)) {
            ship.addListener(new HolyCovenantSourceListener(ship));
        }
        ensureReceiver(ship);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize,
                                          ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("Command Aura", Alignment.MID, pad);
        tooltip.addPara("The host capital ship receives the full value of its own covenant. Allied ships and "
                + "fighter wings inside %s range receive scaled bonuses based on deployment cost or fighter OP "
                + "cost. The signal is strongest within %s range and fades with distance.",
                pad, highlight, "2200", "1000");
        tooltip.addPara("Non-Valkyrian ships count as half value for scaling. Multiple covenant ships can "
                + "contribute, but the bonuses are tightly capped.", pad);

        tooltip.addSectionHeading("Doctrine Effects", Alignment.MID, pad);
        tooltip.setBulletedListMode(" -");
        tooltip.addPara("All covenant sources: damage to fighters, frigates, and destroyers increased by %s, "
                + "capped at %s.", pad, highlight, "1.5%", "5%");

        String baseHullId = getBaseHullId(ship);
        if (baseHullId == null) {
            tooltip.addPara("Battle covenant sources: hard-flux dissipation while shields are up +%s, capped at %s; "
                    + "energy and ballistic recoil reduced by %s, capped at %s.",
                    pad, highlight, "1.5%", "6%", "8%", "25%");
            tooltip.addPara("Missile covenant sources: missile flight speed +%s, capped at %s; missile damage to "
                    + "capital ships +%s, capped at %s.", pad, highlight, "5%", "15%", "2.5%", "6%");
            tooltip.addPara("Pursuit covenant sources: maximum combat speed +%s, capped at %s; maneuverability +%s, "
                    + "capped at %s.", pad, highlight, "3%", "8%", "6%", "15%");
        } else if (isBattleCovenant(baseHullId)) {
            tooltip.addPara("This hull projects the battle covenant: hard-flux dissipation while shields are up +%s, "
                    + "capped at %s; energy and ballistic recoil reduced by %s, capped at %s.",
                    pad, highlight, "1.5%", "6%", "8%", "25%");
        } else if (isMissileCovenant(baseHullId)) {
            tooltip.addPara("This hull projects the missile covenant: missile flight speed +%s, capped at %s; "
                    + "missile damage to capital ships +%s, capped at %s.",
                    pad, highlight, "5%", "15%", "2.5%", "6%");
        } else if (isPursuitCovenant(baseHullId)) {
            tooltip.addPara("This hull projects the pursuit covenant: maximum combat speed +%s, capped at %s; "
                    + "maneuverability +%s, capped at %s.",
                    pad, highlight, "3%", "8%", "6%", "15%");
        }
        tooltip.setBulletedListMode(null);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "2200";
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

    private static void ensureReceiver(ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(HolyCovenantReceiverListener.class)) {
            ship.addListener(new HolyCovenantReceiverListener(ship));
        }
    }

    private static String getBaseHullId(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return null;
        return ship.getHullSpec().getBaseHullId();
    }

    private static boolean isValkyrian(ShipAPI ship) {
        String id = getBaseHullId(ship);
        return id != null && id.startsWith("valk");
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

    private static boolean isValidShip(CombatEngineAPI engine, ShipAPI ship) {
        return ship != null && !ship.isHulk() && engine != null && engine.isEntityInPlay(ship);
    }

    private static boolean isCovenantSource(CombatEngineAPI engine, ShipAPI ship) {
        return isValidShip(engine, ship)
                && ship.isCapital()
                && ship.getVariant() != null
                && ship.getVariant().hasHullMod("valk_holycovenant");
    }

    private static boolean isAlly(ShipAPI source, ShipAPI target) {
        return source != null && target != null && source.getOwner() == target.getOwner();
    }

    private static float getScaling(ShipAPI source, ShipAPI target) {
        if (source == target) return 1f;

        float value = 0.5f;
        if (target.isFighter()) {
            if (target.getWing() != null && target.getWing().getSpec() != null) {
                value = target.getWing().getSpec().getOpCost(target.getMutableStats()) / DP_THRESHOLD_FIGHTER;
            }
        } else if (target.getFleetMember() != null) {
            Float threshold = DP_THRESHOLD_MAP.get(target.getHullSize());
            if (threshold != null && threshold > 0f) {
                value = target.getFleetMember().getDeploymentPointsCost() / threshold;
            }
        }

        if (value < 0.25f) value = 0.25f;
        if (value > 1f) value = 1f;
        if (!isValkyrian(target)) value *= 0.5f;
        return value;
    }

    private static float getRangeFactor(ShipAPI source, ShipAPI target) {
        if (source == target) return 1f;
        float distance = Misc.getDistance(source.getLocation(), target.getLocation());
        if (distance > CAPITAL_EFFECT_RANGE) return 0f;
        if (distance <= FULL_EFFECT_RANGE) return 1f;
        float fade = (CAPITAL_EFFECT_RANGE - distance) / (CAPITAL_EFFECT_RANGE - FULL_EFFECT_RANGE);
        return 0.4f + 0.6f * fade;
    }

    private static void unmodify(MutableShipStatsAPI stats) {
        stats.getDamageToFighters().unmodify(BUFF_KEY);
        stats.getDamageToFrigates().unmodify(BUFF_KEY);
        stats.getDamageToDestroyers().unmodify(BUFF_KEY);
        stats.getHardFluxDissipationFraction().unmodify(BUFF_KEY);
        stats.getMaxRecoilMult().unmodify(BUFF_KEY);
        stats.getRecoilPerShotMult().unmodify(BUFF_KEY);
        stats.getMissileMaxSpeedBonus().unmodify(BUFF_KEY);
        stats.getMaxSpeed().unmodify(BUFF_KEY);
        stats.getAcceleration().unmodify(BUFF_KEY);
        stats.getDeceleration().unmodify(BUFF_KEY);
        stats.getMaxTurnRate().unmodify(BUFF_KEY);
        stats.getTurnAcceleration().unmodify(BUFF_KEY);
    }

    public static class HolyCovenantSourceListener implements AdvanceableListener {
        private final ShipAPI ship;

        public HolyCovenantSourceListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (!isCovenantSource(engine, ship)) return;

            List<ShipAPI> ships = engine.getShips();
            for (ShipAPI other : ships) {
                if (isValidShip(engine, other) && isAlly(ship, other)) {
                    ensureReceiver(other);
                }
            }
        }
    }

    public static class HolyCovenantReceiverListener implements AdvanceableListener, DamageDealtModifier {
        private final ShipAPI ship;
        private float missileDmgToCapitalBonus = 0f;

        public HolyCovenantReceiverListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (!isValidShip(engine, ship)) {
                if (ship != null) unmodify(ship.getMutableStats());
                missileDmgToCapitalBonus = 0f;
                return;
            }

            float smallDamage = 0f;
            float hardFlux = 0f;
            float recoil = 0f;
            float missileSpeed = 0f;
            float missileCapitalDamage = 0f;
            float speed = 0f;
            float maneuver = 0f;

            List<ShipAPI> ships = engine.getShips();
            for (ShipAPI source : ships) {
                if (!isCovenantSource(engine, source) || !isAlly(source, ship)) continue;

                float factor = getScaling(source, ship) * getRangeFactor(source, ship);
                if (factor <= 0f) continue;

                smallDamage += COMMON_DMG_PER_SOURCE * factor;

                String sourceHull = getBaseHullId(source);
                if (isBattleCovenant(sourceHull)) {
                    hardFlux += BATTLE_HARD_FLUX_PER_SOURCE * factor;
                    recoil += BATTLE_RECOIL_PER_SOURCE * factor;
                } else if (isMissileCovenant(sourceHull)) {
                    missileSpeed += MISSILE_SPEED_PER_SOURCE * factor;
                    missileCapitalDamage += MISSILE_CAPITAL_DAMAGE_PER_SOURCE * factor;
                } else if (isPursuitCovenant(sourceHull)) {
                    speed += PURSUIT_SPEED_PER_SOURCE * factor;
                    maneuver += PURSUIT_MANEUVER_PER_SOURCE * factor;
                }
            }

            smallDamage = Math.min(smallDamage, MAX_DMG_TO_SMALL_BONUS);
            hardFlux = Math.min(hardFlux, MAX_HARD_FLUX_BONUS);
            recoil = Math.min(recoil, MAX_RECOIL_BONUS);
            missileSpeed = Math.min(missileSpeed, MAX_MISSILE_SPEED_BONUS);
            missileCapitalDamage = Math.min(missileCapitalDamage, MAX_MISSILE_DMG_TO_CAPITAL_BONUS);
            speed = Math.min(speed, MAX_SPEED_BONUS);
            maneuver = Math.min(maneuver, MAX_MANEUVERABILITY_BONUS);

            MutableShipStatsAPI stats = ship.getMutableStats();
            unmodify(stats);
            missileDmgToCapitalBonus = missileCapitalDamage;

            if (smallDamage > 0f) {
                stats.getDamageToFighters().modifyPercent(BUFF_KEY, smallDamage);
                stats.getDamageToFrigates().modifyPercent(BUFF_KEY, smallDamage);
                stats.getDamageToDestroyers().modifyPercent(BUFF_KEY, smallDamage);
            }
            if (hardFlux > 0f) {
                stats.getHardFluxDissipationFraction().modifyFlat(BUFF_KEY, hardFlux * 0.01f);
            }
            if (recoil > 0f) {
                float mult = Math.max(0.01f, 1f - recoil * 0.01f);
                stats.getMaxRecoilMult().modifyMult(BUFF_KEY, mult);
                stats.getRecoilPerShotMult().modifyMult(BUFF_KEY, mult);
            }
            if (missileSpeed > 0f) {
                stats.getMissileMaxSpeedBonus().modifyPercent(BUFF_KEY, missileSpeed);
            }
            if (speed > 0f) {
                stats.getMaxSpeed().modifyPercent(BUFF_KEY, speed);
            }
            if (maneuver > 0f) {
                stats.getAcceleration().modifyPercent(BUFF_KEY, maneuver);
                stats.getDeceleration().modifyPercent(BUFF_KEY, maneuver);
                stats.getMaxTurnRate().modifyPercent(BUFF_KEY, maneuver);
                stats.getTurnAcceleration().modifyPercent(BUFF_KEY, maneuver);
            }

            if (engine.getPlayerShip() == ship && smallDamage + hardFlux + recoil + missileSpeed
                    + missileCapitalDamage + speed + maneuver > 0f) {
                String icon = Global.getSettings().getSpriteName("ui", "valk_holycovenant_icon");
                engine.maintainStatusForPlayerShip(BUFF_KEY, icon, "Holy Covenant",
                        "command signal stabilized", false);
            }
        }

        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (missileDmgToCapitalBonus <= 0f) return null;
            if (!(param instanceof MissileAPI)) return null;
            if (!(target instanceof ShipAPI)) return null;
            if (!((ShipAPI) target).isCapital()) return null;

            damage.getModifier().modifyPercent(BUFF_KEY, missileDmgToCapitalBonus);
            return BUFF_KEY;
        }
    }
}
