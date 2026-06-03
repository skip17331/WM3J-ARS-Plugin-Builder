package com.wm3j.pluginbuilder.core;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.ContestScorer;
import com.jlog.scoring.StationContext;
import com.jlog.util.BandPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Real contest score/multiplier/dupe preview, now that scoring lives in the
 * engine ({@link ContestScorer}). The builder feeds a handful of sample QSOs and
 * the operator's own station facts; this runs the exact engine path the cockpit
 * uses — {@code points()} per QSO, {@code isDupe()} against earlier same-call
 * QSOs, then {@code score()} over the whole list — and renders a per-QSO +
 * totals breakdown. Pure (no JavaFX); the same numbers a real log would show.
 */
public final class ContestScorePreview {
    private ContestScorePreview() {}

    /** Entry-field ids the controller skips when assigning field1..field5 slots. */
    private static final Set<String> NON_SLOT = Set.of(
        "callsign", "serial_sent", "serial_rcvd", "band", "mode",
        "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent");

    // ---- compute -------------------------------------------------------

    /**
     * Score the sample log exactly as the cockpit would: set each QSO's dupe
     * flag (against earlier same-call QSOs) and points, then aggregate. Mutates
     * the records' dupe/points so {@link #format} can show them.
     */
    public static ContestScore compute(ContestPlugin plugin, List<QsoRecord> qsos, StationContext ctx) {
        String multCol = plugin.computeMultiplierDbColumn();
        List<String> bands = bands(plugin);
        for (int i = 0; i < qsos.size(); i++) {
            QsoRecord q = qsos.get(i);
            List<QsoRecord> priors = new ArrayList<>();
            for (int j = 0; j < i; j++)
                if (sameCall(qsos.get(j), q)) priors.add(qsos.get(j));
            q.setDupe(ContestScorer.isDupe(plugin, q, priors));
            q.setPoints(ContestScorer.points(plugin, q, ctx));
        }
        return ContestScorer.score(plugin, qsos, ctx, multCol, bands, 0);
    }

    // ---- format --------------------------------------------------------

    public static String format(ContestPlugin plugin, List<QsoRecord> qsos, StationContext ctx) {
        if (qsos.isEmpty())
            return "Add sample QSOs below to preview scoring.\n\n" + sampleTemplate(plugin);
        ContestScore s = compute(plugin, qsos, ctx);
        String mtype = plugin.getScoringRules() != null && plugin.getScoringRules().getMultiplierType() != null
                ? plugin.getScoringRules().getMultiplierType()
                : (plugin.getMultiplierModel() != null && plugin.getMultiplierModel().isPerBand()
                    ? "per-band model" : "worked list");

        StringBuilder b = new StringBuilder();
        b.append("SCORE PREVIEW   (multiplier: ").append(mtype)
         .append(",  mult column: ").append(plugin.computeMultiplierDbColumn()).append(")\n\n");
        b.append(String.format("  %-3s %-11s %-5s %-5s %-16s %8s%n",
                "#", "CALL", "BAND", "MODE", "EXCHANGE", "POINTS"));
        int i = 1;
        for (QsoRecord q : qsos) {
            b.append(String.format("  %-3d %-11s %-5s %-5s %-16s %8s%n",
                    i++,
                    nz(q.getCallsign()),
                    nz(q.getBand()),
                    nz(q.getMode()),
                    exchange(q),
                    q.isDupe() ? "dupe" : String.valueOf(q.getPoints())));
        }
        b.append('\n');
        b.append(String.format("  QSOs (counted): %d    QSO points: %d    Multipliers: %d    SCORE: %d%n",
                s.qsoCount(), s.points(), s.mults(), s.score()));

        b.append('\n');
        b.append("— Claimed/running total; sponsors re-adjudicate from the submitted log.\n");
        if ("wae".equals(plugin.getScoringRules() != null ? plugin.getScoringRules().getMultiplierType() : null))
            b.append("  WAE QTC points are not modeled here (preview QSO points only).\n");
        b.append("  Own-station facts (call/grid/QTH) affect asymmetric and QSO-party scoring.\n");
        return b.toString();
    }

    // ---- sample-log parsing -------------------------------------------

    /**
     * Parse the sample log. One QSO per line:
     * {@code call, band, mode, ex1, ex2, …} (comma- or whitespace-separated).
     * {@code ex1…} fill field1..field5 in slot order. Blank lines and {@code #}
     * comments are ignored. Values are upper-cased to match logged records.
     */
    public static List<QsoRecord> parse(String text) {
        List<QsoRecord> out = new ArrayList<>();
        if (text == null) return out;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("[,\\s]+");
            QsoRecord q = new QsoRecord();
            if (t.length > 0) q.setCallsign(t[0]);
            if (t.length > 1) q.setBand(t[1].toLowerCase());
            if (t.length > 2) q.setMode(up(t[2]));
            for (int k = 3; k < t.length && k <= 7; k++) {
                String v = up(t[k]);
                switch (k - 3) {
                    case 0 -> q.setContestField1(v);
                    case 1 -> q.setContestField2(v);
                    case 2 -> q.setContestField3(v);
                    case 3 -> q.setContestField4(v);
                    case 4 -> q.setContestField5(v);
                }
            }
            out.add(q);
        }
        return out;
    }

    /** Seed text: a comment header naming the exchange slots, plus two stub rows. */
    public static String sampleTemplate(ContestPlugin plugin) {
        List<String> slots = slotFieldIds(plugin);
        String cols = slots.isEmpty() ? "(no exchange fields)" : String.join(", ", slots);
        return "# one QSO per line:  call, band, mode" + (slots.isEmpty() ? "" : ", " + cols) + "\n"
             + "W1AW, 20m, CW" + stub(slots.size()) + "\n"
             + "DL1ABC, 20m, CW" + stub(slots.size()) + "\n";
    }

    // ---- helpers -------------------------------------------------------

    /** Plugin band list: the band field's options if declared, else the full band plan. */
    private static List<String> bands(ContestPlugin p) {
        if (p.getEntryFields() != null)
            for (ContestPlugin.FieldDef fd : p.getEntryFields())
                if ("band".equals(fd.getId()) && fd.getOptions() != null && !fd.getOptions().isEmpty())
                    return fd.getOptions();
        return BandPlan.allBands();
    }

    /** Entry-field ids that take a field1..field5 slot, in declaration order. */
    private static List<String> slotFieldIds(ContestPlugin p) {
        List<String> ids = new ArrayList<>();
        if (p.getEntryFields() != null)
            for (ContestPlugin.FieldDef fd : p.getEntryFields()) {
                if (fd.isConstant()) continue;
                String id = fd.getId() == null ? "" : fd.getId();
                if (NON_SLOT.contains(id)) continue;
                if (ids.size() < 5) ids.add(id);
            }
        return ids;
    }

    private static String stub(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(", ?");
        return b.toString();
    }

    private static boolean sameCall(QsoRecord a, QsoRecord b) {
        return nz(a.getCallsign()).equalsIgnoreCase(nz(b.getCallsign()));
    }

    private static String exchange(QsoRecord q) {
        StringBuilder b = new StringBuilder();
        for (String v : new String[]{q.getContestField1(), q.getContestField2(),
                q.getContestField3(), q.getContestField4(), q.getContestField5()})
            if (v != null && !v.isBlank()) { if (b.length() > 0) b.append(' '); b.append(v); }
        return b.length() == 0 ? "-" : b.toString();
    }

    private static String up(String s) { return s == null ? null : s.trim().toUpperCase(); }
    private static String nz(String s) { return s == null ? "" : s; }
}
