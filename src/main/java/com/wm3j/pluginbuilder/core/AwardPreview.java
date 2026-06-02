package com.wm3j.pluginbuilder.core;

import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardProgress;

import java.util.stream.Collectors;

/** Formats an engine-computed {@link AwardProgress} for display. */
public final class AwardPreview {
    private AwardPreview() {}

    public static String format(AwardProgress p) {
        AwardPlugin a = p.award;
        int count = p.count();
        int total = p.totalRequired();
        int pct = total > 0 ? (int) Math.round(p.progressRatio() * 100) : 0;
        AwardPlugin.Tier tier = p.currentTier();
        String label = a.getTargetLabel() == null || a.getTargetLabel().isBlank() ? "credits" : a.getTargetLabel();

        StringBuilder b = new StringBuilder();
        b.append(a.getAwardName() == null ? a.getAwardId() : a.getAwardName())
         .append("   (matchOn: ").append(a.getMatchOn()).append(", ")
         .append(a.isSetMatch() ? "set-match" : "count-match").append(")\n\n");
        b.append(label).append(": ").append(count);
        if (total > 0) b.append(" / ").append(total).append("   (").append(pct).append("%)");
        b.append('\n');
        b.append("Current tier: ").append(tier == null ? "none yet"
                : tier.getName() + " (" + tier.getThreshold() + ")").append('\n');
        b.append("Distinct worked values: ").append(p.workedValues.size()).append('\n');

        if (a.isSetMatch() && !p.missingTargets.isEmpty()) {
            String missing = p.missingTargets.stream().limit(20)
                .map(AwardPlugin.Target::getId).collect(Collectors.joining(", "));
            b.append("\nMissing ").append(p.missingTargets.size()).append(": ").append(missing);
            if (p.missingTargets.size() > 20) b.append(", …");
            b.append('\n');
        }
        if (!p.matchedBonus.isEmpty()) b.append("Bonus matched: ").append(p.matchedBonus.size()).append('\n');

        b.append("\n— Computed by the real engine (AwardService) against your normal log\n  (~/.j-log/j-log.db).");
        return b.toString();
    }
}
