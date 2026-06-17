package in.bikdocs.ludo;

import java.util.List;

public class LudoBot {

    // Analyzes valid moves and returns the best token index to move.
    public static int selectBestMove(LudoGameEngine engine, int playerIdx, List<Integer> validTokens) {
        if (validTokens == null || validTokens.isEmpty()) {
            return -1;
        }
        if (validTokens.size() == 1) {
            return validTokens.get(0);
        }

        int diceValue = engine.getDiceValue();
        int bestTokenIdx = validTokens.get(0);
        int highestScore = -1000;

        // Check if Survival+Kill mode should be active
        // (If any opponent has 2 or more tokens in the goal, they have few tokens left to reach home)
        boolean survivalKillModeActive = false;
        for (int p = 0; p < 4; p++) {
            if (p == playerIdx || !engine.isPlayerActive(p)) continue;
            int tokensInGoal = 0;
            for (int t = 0; t < 4; t++) {
                if (engine.getTokenPositions()[p][t] == LudoGameEngine.POSITION_GOAL) {
                    tokensInGoal++;
                }
            }
            if (tokensInGoal >= 2) {
                survivalKillModeActive = true;
                break;
            }
        }

        for (int t : validTokens) {
            int score = 0;
            int currentPos = engine.getTokenPositions()[playerIdx][t];
            int nextPos = (currentPos == LudoGameEngine.POSITION_YARD) ? 1 : currentPos + diceValue;

            // Base weights
            int safeZoneBonus = 20;
            int dangerPenalty = 40;
            int captureBonus = 100;
            
            // Adjust weights based on human behavior (Phase 2 Learning) or Survival mode
            if (survivalKillModeActive) {
                captureBonus = 500;  // MUST kill if possible to lower their win percentage
                dangerPenalty = 200; // MUST survive to keep pieces on board for killing
                safeZoneBonus = 100; // Hide on stars to survive
            } else {
                if (PlayerStatsLogger.cachedAggression > 0.5f) {
                    safeZoneBonus = 60; // Human is highly aggressive, prioritize defense!
                }
                if (PlayerStatsLogger.cachedRiskTolerance > 0.5f) {
                    dangerPenalty = 20; // Human is risky, bot can afford to be slightly riskier too to set traps
                }
            }

            // Score exactly as requested in Phase 1 (with dynamic Phase 2 weights)
            if (nextPos >= 1 && nextPos <= 51) {
                LudoGameEngine.Point targetCoord = engine.getCoordinateForPosition(playerIdx, t, nextPos);
                
                // Priority 1: Capture opponent
                if (checkCaptureAtPosition(engine, playerIdx, targetCoord)) {
                    score += captureBonus;
                }
                
                // Priority 4: Move into safe zone
                if (engine.isSafeAtPosition(playerIdx, nextPos)) {
                    score += safeZoneBonus;
                }
                
                // Penalty: Move into danger
                if (isDangerZone(engine, playerIdx, targetCoord, nextPos)) {
                    score -= dangerPenalty;
                }

                // Custom Team Mode: Add bonus for landing on a partner to form a block
                if (engine.isTeamMode()) {
                    int partner = engine.getPartnerIndex(playerIdx);
                    if (partner != -1 && engine.isPlayerActive(partner)) {
                        for (int pt = 0; pt < 4; pt++) {
                            int partnerPos = engine.getTokenPositions()[partner][pt];
                            if (partnerPos >= 1 && partnerPos <= 51) {
                                LudoGameEngine.Point partnerCoord = engine.getCoordinateForPosition(partner, pt, partnerPos);
                                if (partnerCoord != null && partnerCoord.x == targetCoord.x && partnerCoord.y == targetCoord.y) {
                                    score += 35; // block creation bonus!
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Priority 2 & 3: Reach home or enter home lane
            if (nextPos >= 52) { // Home lane (52-56) or Goal (57)
                score += 50;
            }
            if (nextPos == LudoGameEngine.POSITION_GOAL) {
                score += 30; // Move toward home / reach home
            }

            // Always add a base score for moving forward so it prefers advancing when no rules match
            if (currentPos > 0) {
                score += currentPos / 5; // small tie-breaker
            } else if (currentPos == LudoGameEngine.POSITION_YARD && diceValue == 6) {
                score += 30; // getting out is always good
            }

            if (score > highestScore) {
                highestScore = score;
                bestTokenIdx = t;
            }
        }

        return bestTokenIdx;
    }

    public static boolean checkCaptureAtPosition(LudoGameEngine engine, int botPlayerIdx, LudoGameEngine.Point targetPoint) {
        int[][] positions = engine.getTokenPositions();
        for (int p = 0; p < 4; p++) {
            if (p == botPlayerIdx || !engine.isPlayerActive(p)) continue;
            if (engine.isTeamMode() && (p % 2 == botPlayerIdx % 2)) continue; // Teammate/partner exclusion
            for (int t = 0; t < 4; t++) {
                if (positions[p][t] >= 1 && positions[p][t] <= 51) {
                    LudoGameEngine.Point oppPoint = engine.getTokenCoordinate(p, t);
                    if (oppPoint.x == targetPoint.x && oppPoint.y == targetPoint.y) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isDangerZone(LudoGameEngine engine, int botPlayerIdx, LudoGameEngine.Point targetPoint, int targetRelativePos) {
        if (engine.isSafeAtPosition(botPlayerIdx, targetRelativePos)) {
            return false;
        }
        int[][] positions = engine.getTokenPositions();
        for (int p = 0; p < 4; p++) {
            if (p == botPlayerIdx || !engine.isPlayerActive(p)) continue;
            if (engine.isTeamMode() && (p % 2 == botPlayerIdx % 2)) continue; // Teammate/partner exclusion
            for (int t = 0; t < 4; t++) {
                int oppPos = positions[p][t];
                if (oppPos >= 1 && oppPos <= 51) {
                    for (int roll = 1; roll <= 6; roll++) {
                        int oppNextPos = oppPos + roll;
                        if (oppNextPos <= 51) {
                            LudoGameEngine.Point oppDangerPoint = engine.getCoordinateForPosition(p, t, oppNextPos);
                            if (oppDangerPoint.x == targetPoint.x && oppDangerPoint.y == targetPoint.y) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
