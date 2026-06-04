package in.bikdocs.ludo;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LudoGameEngine {
    public enum PlayerColor {
        RED(0, "Red"),
        GREEN(1, "Green"),
        YELLOW(2, "Yellow"),
        BLUE(3, "Blue");

        public final int id;
        public final String name;
        PlayerColor(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class Point {
        public final int x, y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Board Constants
    public static final int POSITION_YARD = 0;
    public static final int POSITION_GOAL = 57;

    // Static Track definitions
    private static final Point[] MAIN_TRACK = new Point[52];
    static {
        // Left arm bottom row
        MAIN_TRACK[0] = new Point(0, 6);
        MAIN_TRACK[1] = new Point(1, 6);
        MAIN_TRACK[2] = new Point(2, 6);
        MAIN_TRACK[3] = new Point(3, 6);
        MAIN_TRACK[4] = new Point(4, 6);
        MAIN_TRACK[5] = new Point(5, 6);
        // Top arm left column
        MAIN_TRACK[6] = new Point(6, 5);
        MAIN_TRACK[7] = new Point(6, 4);
        MAIN_TRACK[8] = new Point(6, 3);
        MAIN_TRACK[9] = new Point(6, 2);
        MAIN_TRACK[10] = new Point(6, 1);
        MAIN_TRACK[11] = new Point(6, 0);
        // Top arm tip
        MAIN_TRACK[12] = new Point(7, 0);
        // Top arm right column
        MAIN_TRACK[13] = new Point(8, 0);
        MAIN_TRACK[14] = new Point(8, 1);
        MAIN_TRACK[15] = new Point(8, 2);
        MAIN_TRACK[16] = new Point(8, 3);
        MAIN_TRACK[17] = new Point(8, 4);
        MAIN_TRACK[18] = new Point(8, 5);
        // Right arm top row
        MAIN_TRACK[19] = new Point(9, 6);
        MAIN_TRACK[20] = new Point(10, 6);
        MAIN_TRACK[21] = new Point(11, 6);
        MAIN_TRACK[22] = new Point(12, 6);
        MAIN_TRACK[23] = new Point(13, 6);
        MAIN_TRACK[24] = new Point(14, 6);
        // Right arm tip
        MAIN_TRACK[25] = new Point(14, 7);
        // Right arm bottom row
        MAIN_TRACK[26] = new Point(14, 8);
        MAIN_TRACK[27] = new Point(13, 8);
        MAIN_TRACK[28] = new Point(12, 8);
        MAIN_TRACK[29] = new Point(11, 8);
        MAIN_TRACK[30] = new Point(10, 8);
        MAIN_TRACK[31] = new Point(9, 8);
        // Bottom arm right column
        MAIN_TRACK[32] = new Point(8, 9);
        MAIN_TRACK[33] = new Point(8, 10);
        MAIN_TRACK[34] = new Point(8, 11);
        MAIN_TRACK[35] = new Point(8, 12);
        MAIN_TRACK[36] = new Point(8, 13);
        MAIN_TRACK[37] = new Point(8, 14);
        // Bottom arm tip
        MAIN_TRACK[38] = new Point(7, 14);
        // Bottom arm left column
        MAIN_TRACK[39] = new Point(6, 14);
        MAIN_TRACK[40] = new Point(6, 13);
        MAIN_TRACK[41] = new Point(6, 12);
        MAIN_TRACK[42] = new Point(6, 11);
        MAIN_TRACK[43] = new Point(6, 10);
        MAIN_TRACK[44] = new Point(6, 9);
        // Left arm top row
        MAIN_TRACK[45] = new Point(5, 8);
        MAIN_TRACK[46] = new Point(4, 8);
        MAIN_TRACK[47] = new Point(3, 8);
        MAIN_TRACK[48] = new Point(2, 8);
        MAIN_TRACK[49] = new Point(1, 8);
        MAIN_TRACK[50] = new Point(0, 8);
        // Left arm tip
        MAIN_TRACK[51] = new Point(0, 7);
    }

    // Safe zone indices on the MAIN_TRACK
    private static final int[] SAFE_INDICES = {1, 9, 14, 22, 27, 35, 40, 48};

    // Paths dictionary: index is PlayerColor.id, contents are the 58 point positions (0 is yard, 1-51 main track, 52-56 home run, 57 goal)
    private final Point[][] paths = new Point[4][58];
    // Yard locations for the tokens
    private final Point[][] yardPositions = new Point[4][4];

    // Game states
    private final PlayerColor[] players = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE};
    private final boolean[] isPlayerActive = {true, true, true, true}; // supports custom configuration
    private final boolean[] isPlayerAI = {false, true, true, true};     // Red is Human, others default to AI
    
    private int currentPlayerIndex = 0;
    private int diceValue = 1;
    private boolean isDiceRolled = false;
    private int consecutiveSixes = 0;
    
    // tokenPositions[player_id][token_id] = 0..57
    private final int[][] tokenPositions = new int[4][4];
    private final List<Integer> finishedRankings = new ArrayList<>();

    public LudoGameEngine() {
        initPaths();
        resetGame();
    }

    private void initPaths() {
        // Red(0)=BL(40), Green(1)=TL(1), Yellow(2)=TR(14), Blue(3)=BR(27)
        int[] startIndices = {40, 1, 14, 27};

        // Initialize Yard locations for rendering
        // P0=BL
        yardPositions[0][0] = new Point(2, 11); yardPositions[0][1] = new Point(3, 11);
        yardPositions[0][2] = new Point(2, 12); yardPositions[0][3] = new Point(3, 12);

        // P1=TL
        yardPositions[1][0] = new Point(2, 2); yardPositions[1][1] = new Point(3, 2);
        yardPositions[1][2] = new Point(2, 3); yardPositions[1][3] = new Point(3, 3);

        // P2=TR
        yardPositions[2][0] = new Point(11, 2); yardPositions[2][1] = new Point(12, 2);
        yardPositions[2][2] = new Point(11, 3); yardPositions[2][3] = new Point(12, 3);

        // P3=BR
        yardPositions[3][0] = new Point(11, 11); yardPositions[3][1] = new Point(12, 11);
        yardPositions[3][2] = new Point(11, 12); yardPositions[3][3] = new Point(12, 12);

        for (int p = 0; p < 4; p++) {
            // position 0: yard default
            paths[p][0] = new Point(0, 0); // dynamically fetched from yardPositions

            int startIndex = startIndices[p];
            // positions 1 to 51: main track clockwise
            for (int i = 0; i < 51; i++) {
                int trackIdx = (startIndex + i) % 52;
                paths[p][i + 1] = MAIN_TRACK[trackIdx];
            }

            // positions 52 to 56: Home Run Path
            for (int i = 0; i < 5; i++) {
                if (p == 0) paths[p][52 + i] = new Point(7, 13 - i); // Red (BL): (7,13) -> (7,9)
                else if (p == 1) paths[p][52 + i] = new Point(1 + i, 7); // Green (TL): (1,7) -> (5,7)
                else if (p == 2) paths[p][52 + i] = new Point(7, 1 + i); // Yellow (TR): (7,1) -> (7,5)
                else paths[p][52 + i] = new Point(13 - i, 7); // Blue (BR): (13,7) -> (9,7)
            }

            // position 57: Goal triangle center
            if (p == 0) paths[p][57] = new Point(7, 8); // BL Goal
            else if (p == 1) paths[p][57] = new Point(6, 7); // TL Goal
            else if (p == 2) paths[p][57] = new Point(7, 6); // TR Goal
            else paths[p][57] = new Point(8, 7); // BR Goal
        }
    }

    public void resetGame() {
        currentPlayerIndex = 0;
        diceValue = 1;
        isDiceRolled = false;
        consecutiveSixes = 0;
        finishedRankings.clear();
        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                tokenPositions[p][t] = POSITION_YARD;
            }
        }
    }

    public PlayerColor getCurrentPlayer() {
        return players[currentPlayerIndex];
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public boolean isDiceRolled() {
        return isDiceRolled;
    }

    public int getDiceValue() {
        return diceValue;
    }

    public int[][] getTokenPositions() {
        return tokenPositions;
    }

    public Point getTokenCoordinate(int playerIdx, int tokenIdx) {
        int pos = tokenPositions[playerIdx][tokenIdx];
        if (pos == POSITION_YARD) {
            return yardPositions[playerIdx][tokenIdx];
        }
        return paths[playerIdx][pos];
    }

    /**
     * Returns the board coordinate for a given player at an arbitrary position.
     * Used by the board view for animation and by the bot for look-ahead.
     */
    public Point getCoordinateForPosition(int playerIdx, int tokenIdx, int position) {
        if (position == POSITION_YARD) {
            return yardPositions[playerIdx][tokenIdx];
        }
        if (position >= 0 && position < paths[playerIdx].length) {
            return paths[playerIdx][position];
        }
        return paths[playerIdx][POSITION_GOAL]; // clamp to goal
    }

    /**
     * Returns true if the given player-relative position is a safe zone on the main track.
     */
    public boolean isSafeAtPosition(int playerIdx, int position) {
        if (position == POSITION_YARD || position >= 52) {
            return true; // yard, home run, and goal are all safe
        }
        if (position >= 1 && position <= 51) {
            Point coord = paths[playerIdx][position];
            int mainIdx = getMainTrackIndex(coord);
            return isSafeIndex(mainIdx);
        }
        return false;
    }

    public Point[] getMainTrack() {
        return MAIN_TRACK;
    }

    public Point[][] getYardPositions() {
        return yardPositions;
    }

    public boolean isPlayerAI(int playerIdx) {
        return isPlayerAI[playerIdx];
    }

    public void setPlayerAI(int playerIdx, boolean isAI) {
        isPlayerAI[playerIdx] = isAI;
    }

    public boolean isPlayerActive(int playerIdx) {
        return isPlayerActive[playerIdx];
    }

    public void setPlayerActive(int playerIdx, boolean active) {
        isPlayerActive[playerIdx] = active;
    }

    public int getActivePlayerCount() {
        int count = 0;
        for (boolean active : isPlayerActive) {
            if (active) count++;
        }
        return count;
    }

    public List<Integer> getFinishedRankings() {
        return finishedRankings;
    }

    public void markPlayerFinished(int playerIdx) {
        if (!finishedRankings.contains(playerIdx)) {
            finishedRankings.add(playerIdx);
        }
    }

    public boolean isGameOver() {
        int activeCount = getActivePlayerCount();
        if (activeCount <= 1) return true;
        return finishedRankings.size() >= activeCount - 1;
    }

    // Rolls the dice and updates states
    public int rollDice() {
        boolean hasTokenInYard = false;
        for (int t = 0; t < 4; t++) {
            if (tokenPositions[currentPlayerIndex][t] == POSITION_YARD) {
                hasTokenInYard = true;
                break;
            }
        }

        if (hasTokenInYard) {
            double chanceOfSix = (1.0 / 6.0) + 0.10;
            double rand = Math.random();
            if (rand < chanceOfSix) {
                diceValue = 6;
            } else {
                double remainingRand = (rand - chanceOfSix) / (1.0 - chanceOfSix);
                diceValue = (int) (remainingRand * 5) + 1;
                if (diceValue > 5) diceValue = 5;
            }
        } else {
            diceValue = (int) (Math.random() * 6) + 1;
        }

        isDiceRolled = true;
        return diceValue;
    }

    // Checks if the roll is 6 and increments consecutive count
    // Returns true if turn is cancelled (three 6s in a row)
    public boolean handleSixRollRules() {
        if (diceValue == 6) {
            consecutiveSixes++;
            if (consecutiveSixes == 3) {
                consecutiveSixes = 0;
                isDiceRolled = false;
                nextTurn();
                return true;
            }
        } else {
            consecutiveSixes = 0;
        }
        return false;
    }

    // Get list of token indices that are valid to move
    public List<Integer> getValidMoves(int playerIdx) {
        List<Integer> validTokens = new ArrayList<>();
        if (!isDiceRolled) return validTokens;

        for (int t = 0; t < 4; t++) {
            int pos = tokenPositions[playerIdx][t];
            if (pos == POSITION_YARD) {
                // To get out of yard, you must roll a 6
                if (diceValue == 6) {
                    validTokens.add(t);
                }
            } else if (pos < POSITION_GOAL) {
                // Cannot exceed final goal position
                if (pos + diceValue <= POSITION_GOAL) {
                    validTokens.add(t);
                }
            }
        }
        return validTokens;
    }

    public static class MoveResult {
        public final boolean captureOccurred;
        public final boolean landedSafeZone;
        public final boolean reachedGoal;
        public final boolean bonusTurn;

        public MoveResult(boolean captureOccurred, boolean landedSafeZone, boolean reachedGoal, boolean bonusTurn) {
            this.captureOccurred = captureOccurred;
            this.landedSafeZone = landedSafeZone;
            this.reachedGoal = reachedGoal;
            this.bonusTurn = bonusTurn;
        }
    }

    // Executes the move for the selected token
    public MoveResult executeMove(int playerIdx, int tokenIdx) {
        int currentPos = tokenPositions[playerIdx][tokenIdx];
        int nextPos;

        if (currentPos == POSITION_YARD && diceValue == 6) {
            nextPos = 1; // Release onto track
        } else {
            nextPos = currentPos + diceValue;
        }

        tokenPositions[playerIdx][tokenIdx] = nextPos;
        isDiceRolled = false;

        boolean capture = false;
        boolean safe = false;
        boolean reachedGoal = (nextPos == POSITION_GOAL);

        // Perform capture checks only if on the main track (positions 1 to 51)
        if (nextPos >= 1 && nextPos <= 51) {
            Point targetCoord = paths[playerIdx][nextPos];
            
            // Check if this coordinate matches a safe zone
            int mainTrackIndex = getMainTrackIndex(targetCoord);
            if (isSafeIndex(mainTrackIndex)) {
                safe = true;
            } else {
                // Check other players' tokens on the same coordinate
                for (int p = 0; p < 4; p++) {
                    if (p == playerIdx || !isPlayerActive[p]) continue;
                    for (int t = 0; t < 4; t++) {
                        if (tokenPositions[p][t] >= 1 && tokenPositions[p][t] <= 51) {
                            Point oppCoord = paths[p][tokenPositions[p][t]];
                            if (oppCoord.x == targetCoord.x && oppCoord.y == targetCoord.y) {
                                // Capture! Reset opponent token to yard
                                tokenPositions[p][t] = POSITION_YARD;
                                capture = true;
                            }
                        }
                    }
                }
            }
        }

        // Standard Ludo Rule: Rolling a 6 OR capturing a token OR reaching the goal grants a bonus turn.
        boolean bonus = (diceValue == 6) || capture || reachedGoal;

        if (!bonus) {
            consecutiveSixes = 0;
            nextTurn();
        }

        return new MoveResult(capture, safe, reachedGoal, bonus);
    }

    // Increment turn to next active player
    public void nextTurn() {
        isDiceRolled = false;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        } while (!isPlayerActive[currentPlayerIndex] || finishedRankings.contains(currentPlayerIndex));
    }

    // Checks if the target player has won (all 4 tokens in goal)
    public boolean checkWinner(int playerIdx) {
        for (int t = 0; t < 4; t++) {
            if (tokenPositions[playerIdx][t] != POSITION_GOAL) {
                return false;
            }
        }
        return true;
    }

    // Helper to find index of a Point on the main track loop
    private int getMainTrackIndex(Point p) {
        for (int i = 0; i < 52; i++) {
            if (MAIN_TRACK[i].x == p.x && MAIN_TRACK[i].y == p.y) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSafeIndex(int idx) {
        if (idx == -1) return false;
        for (int s : SAFE_INDICES) {
            if (s == idx) return true;
        }
        return false;
    }

    public String exportStateToJson() {
        try {
            JSONObject state = new JSONObject();
            
            JSONArray activeArray = new JSONArray();
            JSONArray aiArray = new JSONArray();
            for (int i = 0; i < 4; i++) {
                activeArray.put(isPlayerActive[i]);
                aiArray.put(isPlayerAI[i]);
            }
            state.put("isPlayerActive", activeArray);
            state.put("isPlayerAI", aiArray);
            
            state.put("currentPlayerIndex", currentPlayerIndex);
            state.put("diceValue", diceValue);
            state.put("isDiceRolled", isDiceRolled);
            state.put("consecutiveSixes", consecutiveSixes);
            
            JSONArray tokensArray = new JSONArray();
            for (int p = 0; p < 4; p++) {
                JSONArray pTokens = new JSONArray();
                for (int t = 0; t < 4; t++) {
                    pTokens.put(tokenPositions[p][t]);
                }
                tokensArray.put(pTokens);
            }
            state.put("tokenPositions", tokensArray);
            
            JSONArray rankingsArray = new JSONArray();
            for (Integer r : finishedRankings) {
                rankingsArray.put(r);
            }
            state.put("finishedRankings", rankingsArray);
            
            return state.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean importStateFromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return false;
        try {
            JSONObject state = new JSONObject(jsonStr);
            
            JSONArray activeArray = state.getJSONArray("isPlayerActive");
            JSONArray aiArray = state.getJSONArray("isPlayerAI");
            for (int i = 0; i < 4; i++) {
                isPlayerActive[i] = activeArray.getBoolean(i);
                isPlayerAI[i] = aiArray.getBoolean(i);
            }
            
            currentPlayerIndex = state.getInt("currentPlayerIndex");
            diceValue = state.getInt("diceValue");
            isDiceRolled = state.getBoolean("isDiceRolled");
            consecutiveSixes = state.getInt("consecutiveSixes");
            
            JSONArray tokensArray = state.getJSONArray("tokenPositions");
            for (int p = 0; p < 4; p++) {
                JSONArray pTokens = tokensArray.getJSONArray(p);
                for (int t = 0; t < 4; t++) {
                    tokenPositions[p][t] = pTokens.getInt(t);
                }
            }
            
            finishedRankings.clear();
            JSONArray rankingsArray = state.getJSONArray("finishedRankings");
            for (int i = 0; i < rankingsArray.length(); i++) {
                finishedRankings.add(rankingsArray.getInt(i));
            }
            
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
