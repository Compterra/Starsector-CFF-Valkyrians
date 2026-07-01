package data.scripts.world.industry;

import com.fs.starfarer.api.impl.campaign.ids.Industries;
import data.campaign.industry.ValkWarCoordinationBureau;
import exerelin.world.ExerelinProcGen.ProcGenEntity;
import exerelin.world.industry.IndustryClassGen;

public class ValkWarCoordinationBureauSeed extends IndustryClassGen {

    public ValkWarCoordinationBureauSeed() {
        super(ValkWarCoordinationBureau.BUREAU_ID);
    }

    @Override
    public boolean canAutogen() {
        return false;
    }

    @Override
    public boolean canApply(ProcGenEntity entity) {
        if (entity == null || entity.market == null) return false;
        if (!ValkWarCoordinationBureau.FACTION_ID.equals(entity.market.getFactionId())) return false;
        if (entity.market.hasIndustry(ValkWarCoordinationBureau.BUREAU_ID)) return false;
        if (entity.market.hasIndustry(ValkWarCoordinationBureau.LIAISON_ID)) return false;
        return true;
    }

    @Override
    public float getWeight(ProcGenEntity entity) {
        float weight = entity.market.getSize() * 10f;
        if (entity.isHQ) weight += 100f;
        if (entity.isCapital) weight += 25f;
        if (entity.market.hasIndustry(Industries.HIGHCOMMAND)) weight += 20f;
        if (entity.market.hasIndustry(Industries.ORBITALWORKS)) weight += 15f;
        if (entity.market.hasIndustry(Industries.MEGAPORT)) weight += 10f;
        return weight;
    }
}
