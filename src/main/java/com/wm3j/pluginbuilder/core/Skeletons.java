package com.wm3j.pluginbuilder.core;

/** Minimal valid starting points for a new plugin. */
public final class Skeletons {
    private Skeletons() {}

    public static String contest() {
        return """
            {
              "contestId": "MY_CONTEST",
              "contestName": "My Contest",
              "cabrilloContestName": "",
              "version": "1.0.0",
              "exchangeFormat": "RST + Serial",
              "entryFields": [
                { "id": "callsign",   "label": "Call",   "entryRow": 0 },
                { "id": "rst_rcvd",   "label": "RST",    "entryRow": 0, "validator": "numeric" },
                { "id": "serial_rcvd","label": "Nr",     "entryRow": 0, "validator": "numeric" },
                { "id": "rst_sent",   "label": "RST",    "entryRow": 1, "validator": "numeric" },
                { "id": "serial_sent","label": "Nr",     "entryRow": 1, "autoIncrement": true }
              ],
              "cabrilloSent": ["rst_sent", "serial_sent"],
              "cabrilloRcvd": ["rst_rcvd", "serial_rcvd"],
              "scoringRules": { "pointsPerQso": 1, "multiplierType": "dxcc" },
              "multiplierModel": { "field": "callsign", "perBand": false }
            }
            """;
    }

    public static String award() {
        return """
            {
              "awardId": "MY_AWARD",
              "awardName": "My Award",
              "description": "",
              "matchOn": "state",
              "targetLabel": "States",
              "targets": [],
              "tiers": [ { "threshold": 50, "name": "Basic" } ]
            }
            """;
    }
}
