package data.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import data.campaign.industry.ValkWarCoordinationBureau;

public class ValkProcurementDesk extends BaseSubmarketPlugin {

    @Override
    public void init(SubmarketAPI submarket) {
        this.submarket = submarket;
        this.market = submarket.getMarket();
    }

    @Override
    public float getTariff() {
        return 0.25f;
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;
            pruneWeapons(0f);
            pruneShips(0f);

            int tier = getStockTier();
            int size = Math.max(3, market.getSize());
            boolean bureau = isBureau();
            boolean improved = isImproved();

            int weaponMaxTier = Math.min(5, bureau ? 2 + tier : 1 + tier);
            int fighterMaxTier = Math.min(5, bureau ? 2 + tier / 2 : 1 + tier / 2);
            int weapons = (bureau ? 8 : 4) + size * 2 + tier * 2;
            int fighters = (bureau ? 2 : 1) + tier / 2;
            int hullmods = (bureau ? 2 : 1) + tier / 2;
            if (improved) {
                weapons += 2;
                fighters += 1;
                hullmods += 1;
            }

            addWeapons(weapons, weapons + 3 + tier, weaponMaxTier, ValkWarCoordinationBureau.FACTION_ID);
            addFighters(fighters, fighters + (bureau ? 2 : 1), fighterMaxTier, ValkWarCoordinationBureau.FACTION_ID);
            addHullMods(hullmods, hullmods + 1, ValkWarCoordinationBureau.FACTION_ID);

            getCargo().getMothballedShips().clear();
            FactionDoctrineAPI doctrine = submarket.getFaction().getDoctrine().clone();
            doctrine.setShipSize(Math.min(4, bureau ? 3 + tier / 3 : 2 + tier / 4));
            doctrine.setCombatFreighterProbability(bureau ? 0.15f : 0.30f);

            float combat = bureau ? 45f + size * 12f + tier * 8f : 20f + size * 5f + tier * 3f;
            float logistics = bureau ? 20f + size * 4f : 25f + size * 6f;
            addShips(ValkWarCoordinationBureau.FACTION_ID,
                    combat,
                    logistics,
                    10f + tier * 2f,
                    10f + tier * 2f,
                    bureau ? 4f : 8f,
                    12f + tier * 4f,
                    null,
                    bureau ? 0.10f : 0f,
                    ShipPickMode.PRIORITY_THEN_ALL,
                    doctrine);
        }

        getCargo().sort();
    }

    protected int getStockTier() {
        Object tier = market.getMemoryWithoutUpdate().get(ValkWarCoordinationBureau.MEM_STOCK_TIER);
        if (tier instanceof Number) {
            return Math.max(1, Math.min(5, ((Number) tier).intValue()));
        }
        return 1;
    }

    protected boolean isBureau() {
        return Boolean.TRUE.equals(market.getMemoryWithoutUpdate().get(ValkWarCoordinationBureau.MEM_BUREAU));
    }

    protected boolean isImproved() {
        return Boolean.TRUE.equals(market.getMemoryWithoutUpdate().get(ValkWarCoordinationBureau.MEM_IMPROVED));
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return action == TransferAction.PLAYER_BUY && !hasAccess();
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return action == TransferAction.PLAYER_BUY && !hasAccess();
    }

    protected boolean hasAccess() {
        if (Global.getSector() == null) return false;
        FactionAPI player = Global.getSector().getPlayerFaction();
        FactionAPI valk = Global.getSector().getFaction(ValkWarCoordinationBureau.FACTION_ID);
        return player != null && valk != null && valk.getRelationshipLevel(player).isAtWorst(RepLevel.WELCOMING);
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return "The procurement desk does not accept outside stock.";
        if (!hasAccess()) return "Procurement clearance requires Welcoming relations with Valkyria.";
        return "Valkyrian procurement clearance denied.";
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return "The procurement desk does not accept outside hulls.";
        if (!hasAccess()) return "Procurement clearance requires Welcoming relations with Valkyria.";
        return "Valkyrian procurement clearance denied.";
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
