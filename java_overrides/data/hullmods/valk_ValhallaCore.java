package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

public class valk_ValhallaCore extends BaseHullMod {
    private static final float LIMIT_FLUX_LEVEL = 0.70f;
    private static final float DAMAGE_INCREASE = 12f;
    private static final float DAMAGE_MORE_TAKEN = 15f;
    private static final float SYSTEM_DAMAGE_MORE_TAKEN = 25f;
    private static final float MAX_SPEED_PERCENT = 12f;
    private static final String ID = "valk_ValhallaCore";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;
        MutableShipStatsAPI stats = ship.getMutableStats();

        float fluxLevel = ship.getFluxLevel();
        if (fluxLevel <= LIMIT_FLUX_LEVEL) {
            unmodify(stats);
            return;
        }

        float factor = (fluxLevel - LIMIT_FLUX_LEVEL) / (1f - LIMIT_FLUX_LEVEL);
        if (factor < 0f) factor = 0f;
        if (factor > 1f) factor = 1f;

        float damageBonus = DAMAGE_INCREASE * factor;
        stats.getBallisticWeaponDamageMult().modifyPercent(ID, damageBonus);
        stats.getEnergyWeaponDamageMult().modifyPercent(ID, damageBonus);
        stats.getMissileWeaponDamageMult().modifyPercent(ID, damageBonus);

        float damageTaken = DAMAGE_MORE_TAKEN * factor;
        stats.getShieldDamageTakenMult().modifyPercent(ID, damageTaken);
        stats.getArmorDamageTakenMult().modifyPercent(ID, damageTaken);
        stats.getHullDamageTakenMult().modifyPercent(ID, damageTaken);

        float systemDamageTaken = SYSTEM_DAMAGE_MORE_TAKEN * factor;
        stats.getWeaponDamageTakenMult().modifyPercent(ID, systemDamageTaken);
        stats.getEngineDamageTakenMult().modifyPercent(ID, systemDamageTaken);
        stats.getEmpDamageTakenMult().modifyPercent(ID, systemDamageTaken);

        float straightFactor = getStraightLineFactor(ship);
        stats.getMaxSpeed().modifyPercent(ID, MAX_SPEED_PERCENT * factor * straightFactor);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "70%";
        if (index == 1) return "12%";
        if (index == 2) return "15%";
        if (index == 3) return "12%";
        return null;
    }

    private static float getStraightLineFactor(ShipAPI ship) {
        if (ship.getVelocity() == null || ship.getVelocity().lengthSquared() <= 1f) return 0f;
        float velocityFacing = Misc.getAngleInDegrees(ship.getVelocity());
        float angle = Math.abs(Misc.getAngleDiff(ship.getFacing(), velocityFacing));
        if (angle > 15f) return 0f;
        return (15f - angle) / 15f;
    }

    private static void unmodify(MutableShipStatsAPI stats) {
        stats.getBallisticWeaponDamageMult().unmodify(ID);
        stats.getEnergyWeaponDamageMult().unmodify(ID);
        stats.getMissileWeaponDamageMult().unmodify(ID);
        stats.getShieldDamageTakenMult().unmodify(ID);
        stats.getArmorDamageTakenMult().unmodify(ID);
        stats.getHullDamageTakenMult().unmodify(ID);
        stats.getWeaponDamageTakenMult().unmodify(ID);
        stats.getEngineDamageTakenMult().unmodify(ID);
        stats.getEmpDamageTakenMult().unmodify(ID);
        stats.getMaxSpeed().unmodify(ID);
    }
}
