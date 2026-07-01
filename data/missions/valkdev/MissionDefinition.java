package data.missions.valkdev;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "VKS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Valkyrian Evaluation Roster");
        api.setFleetTagline(FleetSide.ENEMY, "Hegemony Benchmark Target");

        api.addBriefingItem("Review representative Valkyrian hulls in a controlled combat environment.");

        List<String> seenHulls = new ArrayList<>();
        List<String> frigates = new ArrayList<>();
        List<String> destroyers = new ArrayList<>();
        List<String> cruisers = new ArrayList<>();
        List<String> capitals = new ArrayList<>();

        boolean devMode = Global.getSettings().isDevMode();
        for (String variantId : Global.getSettings().getAllVariantIds()) {
            if (variantId.endsWith("_wing") || !variantId.startsWith("valk")) {
                continue;
            }
            if (!devMode && (variantId.contains("boss") || variantId.startsWith("valk_yoshura_prime"))) {
                continue;
            }

            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
            EnumSet<ShipTypeHints> hints = member.getHullSpec().getHints();
            if (!devMode && hints.contains(ShipTypeHints.HIDE_IN_CODEX)) {
                continue;
            }

            String hullVariant = member.getHullId() + "_Hull";
            if (seenHulls.contains(hullVariant)) {
                continue;
            }
            seenHulls.add(hullVariant);

            HullSize size = member.getHullSpec().getHullSize();
            if (size == HullSize.FRIGATE) {
                frigates.add(hullVariant);
            } else if (size == HullSize.DESTROYER) {
                destroyers.add(hullVariant);
            } else if (size == HullSize.CRUISER) {
                cruisers.add(hullVariant);
            } else if (size == HullSize.CAPITAL_SHIP) {
                capitals.add(hullVariant);
            }
        }

        addAll(api, frigates);
        addAll(api, destroyers);
        addAll(api, cruisers);
        addAll(api, capitals);

        api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);

        float width = 24000f;
        float height = 18000f;
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        float minX = -width / 2f;
        float minY = -height / 2f;

        for (int i = 0; i < 7; i++) {
            float x = (float) Math.random() * width - width / 2f;
            float y = (float) Math.random() * height - height / 2f;
            float radius = 100f + (float) Math.random() * 800f;
            api.addNebula(x, y, radius);
        }

        api.addObjective(minX + width * 0.7f, minY + height * 0.25f, "sensor_array");
        api.addObjective(minX + width * 0.8f, minY + height * 0.75f, "nav_buoy");
        api.addObjective(minX + width * 0.2f, minY + height * 0.25f, "nav_buoy");
    }

    private void addAll(MissionDefinitionAPI api, List<String> variants) {
        for (String variant : variants) {
            api.addToFleet(FleetSide.PLAYER, variant, FleetMemberType.SHIP, false);
        }
    }
}
