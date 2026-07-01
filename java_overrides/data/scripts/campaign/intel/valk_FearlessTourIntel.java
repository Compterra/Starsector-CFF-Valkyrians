/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fs.starfarer.api.EveryFrameScript
 *  com.fs.starfarer.api.Global
 *  com.fs.starfarer.api.campaign.CampaignFleetAPI
 *  com.fs.starfarer.api.campaign.FactionAPI
 *  com.fs.starfarer.api.campaign.FleetAssignment
 *  com.fs.starfarer.api.campaign.InteractionDialogAPI
 *  com.fs.starfarer.api.campaign.LocationAPI
 *  com.fs.starfarer.api.campaign.PlanetAPI
 *  com.fs.starfarer.api.campaign.SectorEntityToken
 *  com.fs.starfarer.api.campaign.StarSystemAPI
 *  com.fs.starfarer.api.campaign.comm.IntelInfoPlugin$IntelSortTier
 *  com.fs.starfarer.api.campaign.comm.IntelInfoPlugin$ListInfoMode
 *  com.fs.starfarer.api.campaign.econ.MarketAPI
 *  com.fs.starfarer.api.campaign.rules.MemoryAPI
 *  com.fs.starfarer.api.characters.FullName$Gender
 *  com.fs.starfarer.api.characters.PersonAPI
 *  com.fs.starfarer.api.combat.BattleCreationContext
 *  com.fs.starfarer.api.combat.ShipVariantAPI
 *  com.fs.starfarer.api.fleet.FleetMemberAPI
 *  com.fs.starfarer.api.fleet.FleetMemberType
 *  com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl$BaseFIDDelegate
 *  com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl$FIDConfig
 *  com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl$FIDConfigGen
 *  com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent
 *  com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent$SkillPickPreference
 *  com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
 *  com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
 *  com.fs.starfarer.api.impl.campaign.ids.Ranks
 *  com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
 *  com.fs.starfarer.api.loading.VariantSource
 *  com.fs.starfarer.api.ui.SectorMapAPI
 *  com.fs.starfarer.api.ui.TooltipMakerAPI
 *  com.fs.starfarer.api.util.Misc
 *  com.fs.starfarer.api.util.Misc$Token
 *  com.fs.starfarer.api.util.WeightedRandomPicker
 *  org.lwjgl.util.vector.Vector2f
 */
package data.scripts.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.utils.valk.I18nUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

public class valk_FearlessTourIntel
extends BaseIntelPlugin {
    private static final I18nUtils.I18nSection i18n = new I18nUtils.I18nSection("valk_fearlessTour", "event", true);
    private static final int REQUIRED_YOSHURA_COUNT = 1;
    private static final float SHIP_DELIVERY_DAYS = 180.0f;
    private PersonAPI admiral = this.createAdmiral();
    private PersonAPI he;
    private MarketAPI market = this.pickMarket();
    private Stage stage = Stage.ASK_ADMIRAL;
    private int yoshuraPrimeType = -1;
    private SectorEntityToken hideoutLocation;
    private CampaignFleetAPI fleet;
    private float remainDays;

    public valk_FearlessTourIntel() {
        this.market.addPerson(this.admiral);
        this.market.getCommDirectory().addPerson(this.admiral);
        Global.getSector().addScript((EveryFrameScript)this);
        Misc.makeImportant((PersonAPI)this.admiral, (String)"$valkft");
    }

    protected void advanceImpl(float amount) {
        if (this.isEnded() || this.isEnding()) {
            return;
        }
        if (this.stage == Stage.FIND_SHIPS) {
            float days = Global.getSector().getClock().convertToDays(amount);
            this.remainDays -= days;
            if (this.remainDays < 0.0f) {
                this.failed();
            }
        }
        if (this.stage == Stage.FIND_THE_GUY) {
            if (this.fleet == null) {
                return;
            }
            if (this.fleet.isInCurrentLocation() && !this.fleet.getFaction().getId().equals("pirates")) {
                this.fleet.setFaction("pirates", true);
            } else if (!this.fleet.isInCurrentLocation() && !this.fleet.getFaction().getId().equals("neutral")) {
                this.fleet.setFaction("neutral", true);
            }
        }
    }

    protected void notifyEnded() {
        this.sendUpdateIfPlayerHasIntel(null, false);
        Global.getSector().removeScript((EveryFrameScript)this);
        super.notifyEnded();
    }

    private PersonAPI createAdmiral() {
        PersonAPI person = Global.getSector().getFaction("valkyrian").createRandomPerson(FullName.Gender.FEMALE);
        person.setRankId(Ranks.SPACE_ADMIRAL);
        person.setPostId(Ranks.POST_BASE_COMMANDER);
        person.getMemoryWithoutUpdate().set("$valkft_isAdmiral", (Object)true);
        person.getMemoryWithoutUpdate().set("$valkft_eventRef", (Object)this);
        person.getMemoryWithoutUpdate().set("$valkft_stage", (Object)0);
        return person;
    }

    private MarketAPI pickMarket() {
        WeightedRandomPicker picker = new WeightedRandomPicker();
        for (MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!marketAPI.getFactionId().equals("valkyrian")) continue;
            picker.add((Object)marketAPI);
        }
        if (picker.isEmpty()) {
            for (MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
                if (!marketAPI.getFactionId().equals("independent")) continue;
                picker.add((Object)marketAPI);
            }
        }
        return (MarketAPI)picker.pick();
    }

    public boolean callEvent(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String action = params.get(0).getString(memoryMap);
        if (action.equals("nextStage")) {
            this.nextStage();
            this.admiral.getMemoryWithoutUpdate().set("$valkft_stage", (Object)this.stage.ordinal());
            if (dialog.getTextPanel() != null) {
                this.sendUpdate(null, dialog.getTextPanel());
                this.sendUpdateIfPlayerHasIntel(null, false);
            }
        }
        if (action.equals("checkCondition")) {
            MemoryAPI memory = memoryMap.get("local");
            if (this.stage == Stage.FIND_SHIPS) {
                memory.set("$meetCondition", (Object)(this.getYoshuraMembers().size() >= REQUIRED_YOSHURA_COUNT ? 1 : 0), 0.0f);
            }
            return true;
        }
        if (action.equals("withdrawYoshura")) {
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            List<FleetMemberAPI> yoshuras = this.getYoshuraMembers();
            int removed = 0;
            for (FleetMemberAPI member : yoshuras) {
                if (removed >= REQUIRED_YOSHURA_COUNT) break;
                playerFleet.getFleetData().removeFleetMember(member);
                ++removed;
            }
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addPara(i18n.get("s1_r1"), Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), new String[]{String.valueOf(removed)});
            dialog.getTextPanel().setFontInsignia();
        }
        if (action.equals("chooseType")) {
            String type = params.get(1).getString(memoryMap);
            this.yoshuraPrimeType = Integer.parseInt(type);
        }
        if (action.equals("succeed")) {
            Misc.makeUnimportant((SectorEntityToken)this.fleet, (String)"$valkft");
            this.fleet.getMemoryWithoutUpdate().unset("$valkft_eventRef");
            this.nextStage();
            if (dialog.getTextPanel() != null) {
                this.sendUpdate(null, dialog.getTextPanel());
                this.sendUpdateIfPlayerHasIntel(null, false);
            }
        }
        return super.callEvent(ruleId, dialog, params, memoryMap);
    }

    protected String getName() {
        if (this.stage == Stage.END) {
            return i18n.get("title") + " - " + i18n.get("end");
        }
        if (this.stage == Stage.FAILED) {
            return i18n.get("title") + " - " + i18n.get("failed");
        }
        if (this.stage == Stage.OTHER_END) {
            return i18n.get("title") + " - " + i18n.get("otherEnd");
        }
        return i18n.get("title");
    }

    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        Color c = this.getTitleColor(mode);
        info.setParaSmallInsignia();
        info.addPara(this.getName(), c, 0.0f);
        info.setParaFontDefault();
        this.addBulletPoints(info, 3.0f);
    }

    public String getSmallDescriptionTitle() {
        return this.getName();
    }

    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3.0f;
        float opad = 10.0f;
        switch (this.stage) {
            case ASK_ADMIRAL: {
                info.addPara(i18n.get("s0"), opad);
                this.addBulletPoints(info, opad);
                break;
            }
            case FIND_SHIPS: {
                info.addPara(i18n.get("s1"), opad);
                info.addPara(i18n.get("s1_tip"), h, opad);
                this.addBulletPoints(info, opad);
                break;
            }
            case FIND_THE_GUY: {
                info.addPara(i18n.get("s2"), opad);
                this.addBulletPoints(info, opad);
                break;
            }
            case END: {
                info.addPara(i18n.get("end_s0"), opad);
                break;
            }
            case FAILED: {
                info.addPara(i18n.get("failed_s0"), opad);
                break;
            }
            case OTHER_END: {
                info.addPara(i18n.get("otherEnd_s0"), opad);
            }
        }
    }

    private void addBulletPoints(TooltipMakerAPI info, float startPad) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3.0f;
        float opad = 10.0f;
        float initPad = startPad;
        this.bullet(info);
        switch (this.stage) {
            case ASK_ADMIRAL: {
                info.addPara(i18n.get("s0_l0"), initPad, h, new String[]{this.market.getContainingLocation().getName(), this.market.getPrimaryEntity().getName()});
                break;
            }
            case FIND_SHIPS: {
                info.addPara(i18n.get("s1_l0"), initPad, h, new String[]{(int)this.remainDays + ""});
                info.addPara(i18n.get("s1_l1"), initPad, h, new String[]{Math.min(this.getYoshuraMembers().size(), REQUIRED_YOSHURA_COUNT) + " / " + REQUIRED_YOSHURA_COUNT});
                info.addPara(i18n.get("s1_l2"), initPad, h, new String[]{this.market.getContainingLocation().getName(), this.market.getPrimaryEntity().getName()});
                break;
            }
            case FIND_THE_GUY: {
                info.addPara(i18n.get("s2_l0"), initPad, h, new String[]{this.hideoutLocation.getContainingLocation().getName(), this.hideoutLocation.getName()});
                break;
            }
            case END: {
                info.addPara(i18n.get("end_s0"), initPad);
                break;
            }
        }
        this.unindent(info);
    }

    private List<FleetMemberAPI> getYoshuraMembers() {
        ArrayList<FleetMemberAPI> yoshuras = new ArrayList<FleetMemberAPI>();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (!member.isMothballed() || member.getCaptain() != null && member.getFleetCommander() == member.getCaptain() || !this.isAcceptedYoshuraDelivery(member)) continue;
            yoshuras.add(member);
        }
        return yoshuras;
    }

    private boolean isAcceptedYoshuraDelivery(FleetMemberAPI member) {
        String baseHullId = member.getHullSpec().getBaseHullId();
        return "valk_yoshura".equals(baseHullId) || "valk_yoshura_VI".equals(baseHullId);
    }

    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        switch (this.stage) {
            case ASK_ADMIRAL: 
            case FIND_SHIPS: 
            case END: 
            case FAILED: {
                break;
            }
            case FIND_THE_GUY: {
                if (this.hideoutLocation == null) break;
                return this.hideoutLocation;
            }
        }
        if (this.market != null) {
            return this.market.getPrimaryEntity();
        }
        return super.getMapLocation(map);
    }

    public Set<String> getIntelTags(SectorMapAPI map) {
        Set tags = super.getIntelTags(map);
        tags.add("Story");
        tags.add("valkyrian");
        return tags;
    }

    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_0;
    }

    public String getSortString() {
        return "Fearless Tour";
    }

    private void nextStage() {
        switch (this.stage) {
            case ASK_ADMIRAL: {
                this.stage = Stage.FIND_SHIPS;
                this.remainDays = SHIP_DELIVERY_DAYS;
                break;
            }
            case FIND_SHIPS: {
                this.stage = Stage.FIND_THE_GUY;
                this.removeAdmiral();
                this.createHe();
                if (this.he == null) {
                    this.stage = Stage.OTHER_END;
                    this.endImmediately();
                }
                this.pickHideoutLocation();
                if (this.hideoutLocation == null) {
                    this.stage = Stage.OTHER_END;
                    this.endImmediately();
                }
                this.spawnFleet();
                if (this.fleet != null) break;
                this.stage = Stage.OTHER_END;
                this.endImmediately();
                break;
            }
            case FIND_THE_GUY: {
                this.stage = Stage.END;
                break;
            }
        }
    }

    private void createHe() {
        this.he = OfficerManagerEvent.createOfficer((FactionAPI)Global.getSector().getFaction("valkyrian"), (int)10, (OfficerManagerEvent.SkillPickPreference)OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE, (boolean)false, null, (boolean)true, (boolean)true, (int)-1, null);
        this.he.setGender(FullName.Gender.MALE);
        this.he.setRankId(Ranks.FACTION_LEADER);
        this.he.setPostId(Ranks.POST_PATROL_COMMANDER);
        this.he.setPersonality("aggressive");
        this.he.setPortraitSprite("graphics/portraits/portrait_mercenary01.png");
        this.he.getMemoryWithoutUpdate().set("$valkft_type", (Object)this.yoshuraPrimeType);
        this.he.getMemoryWithoutUpdate().set("$valkft_isHe", (Object)true);
    }

    private void pickHideoutLocation() {
        WeightedRandomPicker systemPicker = new WeightedRandomPicker();
        int check = 20;
        while (this.hideoutLocation == null && check > 0) {
            --check;
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                float noSpawnRange;
                float distToPlayer;
                float mult = 0.0f;
                if (system.hasPulsar()) continue;
                if (system.hasTag("theme_misc_skip")) {
                    mult = 1.0f;
                } else if (system.hasTag("theme_misc")) {
                    mult = 3.0f;
                } else if (system.hasTag("theme_remnant_no_fleets")) {
                    mult = 3.0f;
                } else if (system.hasTag("theme_ruins")) {
                    mult = 5.0f;
                } else if (system.hasTag("theme_remnant_destroyed")) {
                    mult = 3.0f;
                } else if (system.hasTag("theme_core_unpopulated")) {
                    mult = 1.0f;
                }
                for (MarketAPI market : Misc.getMarketsInLocation((LocationAPI)system)) {
                    if (market.isHidden()) continue;
                    mult = 0.0f;
                    break;
                }
                if ((distToPlayer = Misc.getDistanceToPlayerLY((Vector2f)system.getLocation())) < (noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY"))) {
                    mult = 0.0f;
                }
                if (mult <= 0.0f) continue;
                float weight = system.getPlanets().size();
                for (PlanetAPI planet : system.getPlanets()) {
                    if (planet.isStar() || planet.getMarket() == null) continue;
                    float h = planet.getMarket().getHazardValue();
                    if (h <= 0.0f) {
                        weight += 5.0f;
                        continue;
                    }
                    if (h <= 0.25f) {
                        weight += 3.0f;
                        continue;
                    }
                    if (!(h <= 0.5f)) continue;
                    weight += 1.0f;
                }
                float dist = system.getLocation().length();
                float distMult = Math.max(0.0f, 50000.0f - dist);
                systemPicker.add((Object)system, weight * mult * distMult);
            }
            StarSystemAPI system = (StarSystemAPI)systemPicker.pick();
            if (system == null) continue;
            WeightedRandomPicker picker = new WeightedRandomPicker();
            for (SectorEntityToken planet : system.getPlanets()) {
                if (planet.isStar() || planet.getMarket() != null && !planet.getMarket().isPlanetConditionMarketOnly()) continue;
                picker.add((Object)planet);
            }
            this.hideoutLocation = (SectorEntityToken)picker.pick();
        }
        if (this.hideoutLocation == null && check == 0) {
            try {
                throw new Exception("There is something wrong with valks generate");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateYoshuraPrime(FleetMemberAPI yoshura_prime) {
        ShipVariantAPI v;
        if (yoshura_prime.getVariant().isStockVariant()) {
            v = yoshura_prime.getVariant().clone();
            v.setSource(VariantSource.REFIT);
            yoshura_prime.setVariant(v, false, false);
        } else {
            v = yoshura_prime.getVariant();
        }
        v.setHullVariantId(yoshura_prime.getId() + "story" + this.yoshuraPrimeType);
        if (this.yoshuraPrimeType == 0) {
            v.addPermaMod("valk_yoshuraSkinLove");
        } else if (this.yoshuraPrimeType == 1) {
            v.addPermaMod("valk_yoshuraSkinHate");
        } else {
            v.addPermaMod("valk_yoshuraSkinNormal");
        }
    }

    private void spawnFleet() {
        FleetParamsV3 params = new FleetParamsV3(null, this.hideoutLocation.getLocationInHyperspace(), "valkyrian", Float.valueOf(1.25f), "personBounty", 400.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        params.officerLevelBonus = 2;
        params.officerLevelLimit = Global.getSettings().getInt("officerMaxLevel") + 2;
        params.ignoreMarketFleetSizeMult = true;
        params.commander = this.he;
        this.fleet = FleetFactoryV3.createFleet((FleetParamsV3)params);
        FleetFactoryV3.addCommanderSkills((PersonAPI)this.he, (CampaignFleetAPI)this.fleet, (FleetParamsV3)params, null);
        String segment = this.yoshuraPrimeType == 0 ? "love" : (this.yoshuraPrimeType == 1 ? "hate" : "normal");
        final FleetMemberAPI yoshura_prime = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "valk_yoshura_prime_" + segment + "_Standard");
        yoshura_prime.getRepairTracker().setCR(1.0f);
        yoshura_prime.getVariant().addTag("always_recoverable");
        yoshura_prime.updateStats();
        this.fleet.getFleetData().addFleetMember(yoshura_prime);
        this.fleet.getFleetData().setFlagship(yoshura_prime);
        yoshura_prime.setCaptain(this.he);
        this.fleet.getFleetData().sort();
        this.fleet.getFleetData().setSyncNeeded();
        this.fleet.getFleetData().syncIfNeeded();
        this.fleet.forceSync();
        this.fleet.getMemoryWithoutUpdate().set("$core_fleetNoMilitaryResponse", (Object)true);
        this.fleet.getMemoryWithoutUpdate().set("$valkft_isHisFleet", (Object)true);
        this.fleet.getMemoryWithoutUpdate().set("$valkft_eventRef", (Object)this);
        this.fleet.getMemoryWithoutUpdate().set("$fidConifgGen", (Object)new FearlessFleetInteractionConfigGen());
        this.fleet.setNoFactionInName(true);
        this.fleet.setFaction("neutral", true);
        this.fleet.setName(i18n.format("fleet_name", this.hideoutLocation.getName()));
        LocationAPI location = this.hideoutLocation.getContainingLocation();
        location.addEntity((SectorEntityToken)this.fleet);
        this.fleet.setLocation(this.hideoutLocation.getLocation().x - 500.0f, this.hideoutLocation.getLocation().y + 500.0f);
        this.fleet.getAI().addAssignment(FleetAssignment.ORBIT_PASSIVE, this.hideoutLocation, 1000000.0f, null);
        Misc.makeImportant((SectorEntityToken)this.fleet, (String)"$valkft");
        this.fleet.addScript(new EveryFrameScript(){
            boolean shouldEnd = false;

            public boolean isDone() {
                return this.shouldEnd;
            }

            public boolean runWhilePaused() {
                return false;
            }

            public void advance(float amount) {
                if (this.shouldEnd) {
                    return;
                }
                if (yoshura_prime != valk_FearlessTourIntel.this.fleet.getFlagship()) {
                    this.shouldEnd = true;
                } else if (valk_FearlessTourIntel.this.fleet.getCommander() != valk_FearlessTourIntel.this.he) {
                    this.shouldEnd = true;
                }
                if (this.shouldEnd) {
                    valk_FearlessTourIntel.this.fleet.clearAssignments();
                    if (valk_FearlessTourIntel.this.hideoutLocation != null) {
                        valk_FearlessTourIntel.this.fleet.getAI().addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, valk_FearlessTourIntel.this.hideoutLocation, 1000000.0f, null);
                    } else {
                        valk_FearlessTourIntel.this.fleet.despawn();
                    }
                }
            }
        });
    }

    private void failed() {
        this.stage = Stage.FAILED;
        this.removeAdmiral();
        this.sendUpdateIfPlayerHasIntel(null, false);
        this.endAfterDelay();
    }

    private void removeAdmiral() {
        Misc.makeUnimportant((PersonAPI)this.admiral, (String)"$valkft");
        if (this.market != null) {
            this.market.removePerson(this.admiral);
            this.market.getCommDirectory().removePerson(this.admiral);
        }
    }

    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "valk_fearlesstour");
    }

    public static class FearlessFleetInteractionConfigGen
    implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
            FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
            config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate(){

                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false;
                    bcc.enemyDeployAll = true;
                    bcc.fightToTheLast = true;
                }
            };
            return config;
        }
    }

    private static enum Stage {
        ASK_ADMIRAL,
        FIND_SHIPS,
        FIND_THE_GUY,
        END,
        FAILED,
        OTHER_END;

    }
}
