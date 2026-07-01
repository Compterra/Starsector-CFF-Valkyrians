package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.campaign.industry.ValkWarCoordinationBureau;
import data.scripts.ai.BallistaAI;
import data.scripts.campaign.VKSCampaignPlugin;
import data.scripts.campaign.event.bars.valk_FearlessTourBarEventCreator;
import data.scripts.world.valkyrianSystem;
import exerelin.campaign.SectorManager;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;

public class VKSModPlugin extends BaseModPlugin {
    private static final String FACTION_ID = "valkyrian";
    private static final String HOMEWORLD_MARKET_ID = "enigma_i";
    private static final String NEX_PROCGEN_HQ_MEM_KEY = "$nex_procgen_hq";
    private static final String BALLISTA_LRM_ID = "valk_ballista_lrm";

    @Override
    public void onApplicationLoad() {
        ShaderLib.init();
        LightData.readLightDataCSV("data/lights/valk_light_data.csv");
    }

    @Override
    public void onNewGame() {
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction(FACTION_ID);

        boolean nexerelinEnabled = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!nexerelinEnabled || SectorManager.getManager().isCorvusMode()) {
            new valkyrianSystem().generate(Global.getSector());
        }

        ensureHomeworldBureau();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(valk_FearlessTourBarEventCreator.class)) {
            bar.addEventCreator(new valk_FearlessTourBarEventCreator());
        }

        Global.getSector().registerPlugin(new VKSCampaignPlugin());
        ensureHomeworldBureau();
    }

    protected void ensureHomeworldBureau() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getEconomy() == null) return;

        MarketAPI homeworld = getHomeworldMarket(sector);
        if (homeworld == null) return;
        if (homeworld.hasIndustry(ValkWarCoordinationBureau.BUREAU_ID)
                || homeworld.hasIndustry(ValkWarCoordinationBureau.LIAISON_ID)) {
            return;
        }

        homeworld.addIndustry(ValkWarCoordinationBureau.BUREAU_ID);
        sector.getEconomy().forceStockpileUpdate(homeworld);
    }

    protected MarketAPI getHomeworldMarket(SectorAPI sector) {
        MarketAPI fixedHomeworld = sector.getEconomy().getMarket(HOMEWORLD_MARKET_ID);
        if (isValkyrianMarket(fixedHomeworld)) return fixedHomeworld;

        Object nexHq = sector.getFaction(FACTION_ID).getMemoryWithoutUpdate().get(NEX_PROCGEN_HQ_MEM_KEY);
        if (nexHq instanceof MarketAPI && isValkyrianMarket((MarketAPI) nexHq)) {
            return (MarketAPI) nexHq;
        }

        MarketAPI best = null;
        float bestScore = -1f;
        for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
            if (!isValkyrianMarket(market)) continue;

            float score = market.getSize() * 10f;
            if (market.hasIndustry(Industries.HIGHCOMMAND)) score += 6f;
            if (market.hasIndustry(Industries.ORBITALWORKS)) score += 5f;
            if (market.hasIndustry(Industries.MEGAPORT)) score += 3f;

            if (score > bestScore) {
                best = market;
                bestScore = score;
            }
        }

        return best;
    }

    protected boolean isValkyrianMarket(MarketAPI market) {
        return market != null && !market.isHidden() && FACTION_ID.equals(market.getFactionId());
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (BALLISTA_LRM_ID.equals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new BallistaAI(missile, launchingShip), PickPriority.MOD_SET);
        }
        return null;
    }
}
