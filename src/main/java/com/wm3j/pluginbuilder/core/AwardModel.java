package com.wm3j.pluginbuilder.core;

/** The scalar award fields the form owns (targets/bonus/tiers handled separately as lists). */
public final class AwardModel {
    public String awardId = "";
    public String awardName = "";
    public String description = "";
    public String matchOn = "";        // state|country|callsign|prefix|dxccPrefix|continent|grid
    public String targetLabel = "";
    public boolean matchBaseCallsign = false;   // options.matchBaseCallsign
    public boolean confirmedOnly = false;       // options.confirmedOnly
    public String windowStart = "";    // window.startUtc
    public String windowEnd = "";      // window.endUtc
}
