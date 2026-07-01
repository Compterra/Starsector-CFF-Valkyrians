package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.valk.Refs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class valk_CombatPlugin extends BaseEveryFrameCombatPlugin {
    public static final String CRUISE_SPEED_BONUS_KEY = "valk_cruiseSpeedBonus";
    public static final String CRUISE_SPEED_BONUS_VALUE = "CRUISE_SPEED_BONUS_VALUE";

    private static final float GRID_SIZE = 200f;
    private static final float BUILD_RATE = 0.1f;
    private static final float FADE_RATE = 0.2f;
    private static final float REMOVE_THRESHOLD = 0.0001f;
    private static final float MAX_SPEED_BONUS = 20f;

    private static final Set<String> MISSILE_IDS = new HashSet<String>(Arrays.asList(
            "valk_phantasm_cruise_missile",
            "valk_spectre_cruise_missile",
            "valk_shadow_lrm"
    ));

    private CombatEngineAPI engine;
    private final Map<AccelerateData, Float> accelerateMap = new HashMap<AccelerateData, Float>();

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        accelerateMap.clear();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        List<ShipAPI> ships = engine.getShips();
        updateCruiseMissileLanes(amount);
        applyCruiseMissileSpeedBonus(ships, amount);
    }

    private void updateCruiseMissileLanes(float amount) {
        decayAccelerationLanes(amount);

        for (MissileAPI missile : engine.getMissiles()) {
            if (missile.isFizzling() || !MISSILE_IDS.contains(missile.getProjectileSpecId())) {
                continue;
            }

            AccelerateData data = AccelerateData.from(missile.getOwner(), missile.getLocation().x, missile.getLocation().y);
            Float progress = accelerateMap.get(data);
            if (progress == null) {
                progress = 0f;
            }

            progress = Math.min(1f, progress + BUILD_RATE * amount);
            accelerateMap.put(data, progress);
        }
    }

    private void decayAccelerationLanes(float amount) {
        Iterator<Map.Entry<AccelerateData, Float>> iter = accelerateMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<AccelerateData, Float> entry = iter.next();
            float progress = entry.getValue() - FADE_RATE * amount;
            if (progress <= REMOVE_THRESHOLD) {
                iter.remove();
            } else {
                entry.setValue(progress);
            }
        }
    }

    private void applyCruiseMissileSpeedBonus(List<ShipAPI> ships, float amount) {
        for (ShipAPI ship : ships) {
            if (!ship.isAlive() || ship.isShuttlePod()) {
                continue;
            }

            AccelerateData data = AccelerateData.from(ship.getOwner(), ship.getLocation().x, ship.getLocation().y);
            Float laneProgress = accelerateMap.get(data);
            Float currentBonus = (Float) ship.getCustomData().get(CRUISE_SPEED_BONUS_VALUE);

            if (laneProgress != null) {
                if (currentBonus == null) {
                    currentBonus = 0f;
                }
                currentBonus = Misc.interpolate(currentBonus, laneProgress, amount);
            } else if (currentBonus != null) {
                currentBonus = Misc.interpolate(currentBonus, 0f, amount);
                if (currentBonus < REMOVE_THRESHOLD) {
                    currentBonus = 0f;
                }
            }

            if (currentBonus != null && currentBonus > 0f) {
                ship.setCustomData(CRUISE_SPEED_BONUS_VALUE, currentBonus);
                ship.getMutableStats().getMaxSpeed().modifyPercent(CRUISE_SPEED_BONUS_KEY, currentBonus * MAX_SPEED_BONUS);
            } else {
                ship.setCustomData(CRUISE_SPEED_BONUS_VALUE, 0f);
                ship.getMutableStats().getMaxSpeed().unmodify(CRUISE_SPEED_BONUS_KEY);
            }
        }

        maintainPlayerStatus();
    }

    private void maintainPlayerStatus() {
        ShipAPI playerShip = engine.getPlayerShip();
        if (playerShip == null) {
            return;
        }

        Float currentBonus = (Float) playerShip.getCustomData().get(CRUISE_SPEED_BONUS_VALUE);
        if (currentBonus == null || currentBonus <= 0f) {
            return;
        }

        String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
        engine.maintainStatusForPlayerShip(
                CRUISE_SPEED_BONUS_KEY,
                icon,
                Refs.system_i18n.get("cm_title") + " " + (int) (currentBonus * 100f) + "%",
                Refs.system_i18n.get("max_speed") + " + " + (int) (currentBonus * MAX_SPEED_BONUS) + "%",
                false
        );
    }

    private static final class AccelerateData {
        private final int owner;
        private final int x;
        private final int y;

        private AccelerateData(int owner, int x, int y) {
            this.owner = owner;
            this.x = x;
            this.y = y;
        }

        private static AccelerateData from(int owner, float locX, float locY) {
            return new AccelerateData(owner, (int) (locX / GRID_SIZE), (int) (locY / GRID_SIZE));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AccelerateData)) {
                return false;
            }
            AccelerateData that = (AccelerateData) o;
            return owner == that.owner && x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, x, y);
        }
    }
}
