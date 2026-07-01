package data.campaign.industry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public class ValkWarCoordinationBureau extends BaseIndustry {
    public static final String LIAISON_ID = "valk_logistics_liaison";
    public static final String BUREAU_ID = "valk_war_coordination_bureau";
    public static final String SUBMARKET_ID = "valk_coordination_storefront";
    public static final String FACTION_ID = "valkyrian";

    public static final String MEM_STOCK_TIER = "$valk_coordinationStockTier";
    public static final String MEM_BUREAU = "$valk_coordinationBureau";
    public static final String MEM_IMPROVED = "$valk_coordinationImproved";

    public static final float GAMMA_UPKEEP_REDUCTION = 5f;
    public static final float BETA_UPKEEP_REDUCTION = 10f;
    public static final float ALPHA_UPKEEP_REDUCTION = 20f;

    public static final float LIAISON_ACCESS = 0.05f;
    public static final float BUREAU_ACCESS = 0.10f;
    public static final float BUREAU_IMPROVE_ACCESS = 0.05f;

    public static final float LIAISON_FLEET_QUALITY = 0.03f;
    public static final float BUREAU_FLEET_QUALITY = 0.08f;
    public static final float LIAISON_OFFICER_PROB = 0.05f;
    public static final float BUREAU_OFFICER_PROB = 0.10f;
    public static final float LIAISON_PRODUCTION_QUALITY = 0.02f;
    public static final float BUREAU_PRODUCTION_QUALITY = 0.05f;

    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) {
            unapply();
            return;
        }

        ensureStorefront();
        applyCoreUpkeep();
        applyBaseModifiers();
        applyImproveModifiers();
        updateStorefrontMemory();
    }

    @Override
    public void unapply() {
        super.unapply();

        if (market != null) {
            market.removeSubmarket(SUBMARKET_ID);
            market.getStability().unmodifyFlat(getModId(0));
            market.getStability().unmodifyFlat(getModId(1));
            market.getAccessibilityMod().unmodifyFlat(getModId(0));
            market.getAccessibilityMod().unmodifyFlat(getModId(1));
            market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(getModId(0));
            market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).unmodifyFlat(getModId(0));
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(0));
            market.getMemoryWithoutUpdate().unset(MEM_STOCK_TIER);
            market.getMemoryWithoutUpdate().unset(MEM_BUREAU);
            market.getMemoryWithoutUpdate().unset(MEM_IMPROVED);
        }

        getUpkeep().unmodifyMult("ind_core");
    }

    protected void ensureStorefront() {
        if (market == null || (!market.isPlayerOwned() && !FACTION_ID.equals(market.getFactionId()))) return;
        if (Global.getSector() == null) return;

        SubmarketAPI open = market.getSubmarket(SUBMARKET_ID);
        if (open == null) {
            market.addSubmarket(SUBMARKET_ID);
            open = market.getSubmarket(SUBMARKET_ID);
            if (open != null) {
                open.setFaction(Global.getSector().getFaction(FACTION_ID));
            }
            Global.getSector().getEconomy().forceStockpileUpdate(market);
        }
    }

    protected void applyCoreUpkeep() {
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - ALPHA_UPKEEP_REDUCTION / 100f, "Alpha Core assigned");
        } else if (Commodities.BETA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - BETA_UPKEEP_REDUCTION / 100f, "Beta Core assigned");
        } else if (Commodities.GAMMA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - GAMMA_UPKEEP_REDUCTION / 100f, "Gamma Core assigned");
        } else {
            getUpkeep().unmodifyMult("ind_core");
        }
    }

    protected void applyBaseModifiers() {
        market.getStability().modifyFlat(getModId(0), 1f, getNameForModifier());
        market.getAccessibilityMod().modifyFlat(getModId(0), isBureau() ? BUREAU_ACCESS : LIAISON_ACCESS,
                getNameForModifier());

        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD)
                .modifyFlat(getModId(0), isBureau() ? BUREAU_FLEET_QUALITY : LIAISON_FLEET_QUALITY,
                        getNameForModifier());
        market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD)
                .modifyFlat(getModId(0), isBureau() ? BUREAU_OFFICER_PROB : LIAISON_OFFICER_PROB,
                        getNameForModifier());

        if (hasHeavyIndustry()) {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                    .modifyFlat(getModId(0), isBureau() ? BUREAU_PRODUCTION_QUALITY : LIAISON_PRODUCTION_QUALITY,
                            getNameForModifier());
        } else {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(0));
        }
    }

    protected boolean hasHeavyIndustry() {
        return market != null
                && (market.hasIndustry(Industries.HEAVYINDUSTRY) || market.hasIndustry(Industries.ORBITALWORKS));
    }

    protected void updateStorefrontMemory() {
        if (market == null) return;

        int tier = isBureau() ? 3 : 1;
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) tier += 2;
        else if (Commodities.BETA_CORE.equals(aiCoreId)) tier += 1;
        if (isImproved()) tier += 1;

        market.getMemoryWithoutUpdate().set(MEM_STOCK_TIER, Math.min(5, tier));
        market.getMemoryWithoutUpdate().set(MEM_BUREAU, isBureau());
        market.getMemoryWithoutUpdate().set(MEM_IMPROVED, isImproved());
    }

    protected boolean isBureau() {
        return BUREAU_ID.equals(getId());
    }

    @Override
    public float getPatherInterest() {
        float interest = isBureau() ? 2f : 1f;
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) interest += isBureau() ? 2f : 1f;
        return interest + super.getPatherInterest();
    }

    @Override
    public boolean isAvailableToBuild() {
        if (!market.hasSpaceport()) return false;
        if (Global.getSector() == null) return true;

        FactionAPI player = Global.getSector().getPlayerFaction();
        FactionAPI valk = Global.getSector().getFaction(FACTION_ID);
        if (player != null && (player.knowsIndustry(LIAISON_ID) || player.knowsIndustry(BUREAU_ID))) return true;
        return player != null && valk != null
                && valk.getRelationshipLevel(player).isAtWorst(isBureau() ? RepLevel.FRIENDLY : RepLevel.WELCOMING);
    }

    @Override
    public boolean showWhenUnavailable() {
        return true;
    }

    @Override
    public String getUnavailableReason() {
        if (!market.hasSpaceport()) return "Requires an operational spaceport";
        return "Requires a Valkyrian coordination charter, or strong relations with Valkyria";
    }

    @Override
    public String getCurrentImage() {
        return getSpec().getImageName();
    }

    @Override
    protected void applyAICoreToIncomeAndUpkeep() {
        applyCoreUpkeep();
    }

    @Override
    protected void applyAlphaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyBetaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyGammaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyNoAICoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f;
        if (isBureau()) {
            tooltip.addPara("Coordinates royal logistics, Orken military production audits, and Enigma Company procurement. The Bureau improves existing military and industrial systems; it does not create commodity output of its own.", opad);
        } else {
            tooltip.addPara("Opens a disciplined Valkyrian liaison office and a limited procurement desk. Its value lies in screened contracts, clean dispatch schedules, and predictable military access.", opad);
        }
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
            Color h = Misc.getHighlightColor();
            float opad = 10f;
            tooltip.addPara("Stability: %s", opad, h, "+1");
            tooltip.addPara("Accessibility: %s", opad, h,
                    "+" + Math.round((isBureau() ? BUREAU_ACCESS : LIAISON_ACCESS) * 100f) + "%");
            tooltip.addPara("Defense fleet quality: %s", opad, h,
                    "+" + Math.round((isBureau() ? BUREAU_FLEET_QUALITY : LIAISON_FLEET_QUALITY) * 100f) + "%");
            tooltip.addPara("Officer availability: %s", opad, h,
                    "+" + Math.round((isBureau() ? BUREAU_OFFICER_PROB : LIAISON_OFFICER_PROB) * 100f) + "%");
            if (hasHeavyIndustry()) {
                tooltip.addPara("Ship quality support from existing heavy industry: %s", opad, h,
                        "+" + Math.round((isBureau() ? BUREAU_PRODUCTION_QUALITY : LIAISON_PRODUCTION_QUALITY) * 100f) + "%");
            }
        }
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Alpha", ALPHA_UPKEEP_REDUCTION,
                isBureau()
                        ? "the Bureau becomes a tireless audit intelligence that sees production slippage before human staff admit it exists"
                        : "sealed routing, procurement vetting, and royal dispatch queues begin answering before requests are formally filed");
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Beta", BETA_UPKEEP_REDUCTION,
                isBureau()
                        ? "predictive maintenance and screened ordnance scheduling improve stock reliability"
                        : "convoy routing and contract triage become faster without handing the office full autonomy");
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Gamma", GAMMA_UPKEEP_REDUCTION,
                isBureau()
                        ? "routine ledger checks and personnel queues run cleanly while officers keep final authority"
                        : "a disciplined scheduling assistant keeps the queues honest and the procurement desk staffed");
    }

    protected void addCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode, String coreName,
                                      float upkeepReduction, String detail) {
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();
        String pre = coreName + "-level AI core. ";
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST) {
            pre = "Currently allocated " + coreName + "-level AI cores. ";
        }

        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
            text.addPara(pre + "Reduces upkeep by %s; " + detail + ".", 0f, highlight,
                    "" + (int) upkeepReduction + "%");
            tooltip.addImageWithText(opad);
            return;
        }

        tooltip.addPara(pre + "Reduces upkeep by %s; " + detail + ".", opad, highlight,
                "" + (int) upkeepReduction + "%");
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    protected void applyImproveModifiers() {
        if (isImproved()) {
            if (isBureau()) {
                market.getAccessibilityMod().modifyFlat(getModId(1), BUREAU_IMPROVE_ACCESS,
                        getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
                market.getStability().unmodifyFlat(getModId(1));
            } else {
                market.getStability().modifyFlat(getModId(1), 1f,
                        getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
                market.getAccessibilityMod().unmodifyFlat(getModId(1));
            }
        } else {
            market.getStability().unmodifyFlat(getModId(1));
            market.getAccessibilityMod().unmodifyFlat(getModId(1));
        }
        updateStorefrontMemory();
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        Color highlight = Misc.getHighlightColor();
        if (isBureau()) {
            info.addPara("Expands secure dispatch halls and protected procurement channels, increasing accessibility by %s and improving storefront quality.",
                    0f, highlight, "+5%");
        } else {
            info.addPara("Makes liaison staff, screening rooms, and dispatch contracts permanent, increasing stability by %s and improving storefront quantity.",
                    0f, highlight, "+1");
        }
        info.addSpacer(10f);
        super.addImproveDesc(info, mode);
    }
}
