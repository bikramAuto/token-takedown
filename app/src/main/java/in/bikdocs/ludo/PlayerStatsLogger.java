package in.bikdocs.ludo;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerStatsLogger {
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public static float cachedAggression = 0f;
    public static float cachedRiskTolerance = 0f;
    public static float cachedDefensive = 0f;
    public static float cachedHomeFocus = 0f;
    public static int cachedTotalMoves = 0;

    public static void refreshStats(Context context) {
        dbExecutor.execute(() -> {
            PlayerTurnDao dao = AppDatabase.getInstance(context).playerTurnDao();
            cachedAggression = dao.getAverageAggression();
            cachedRiskTolerance = dao.getAverageRiskTolerance();
            cachedDefensive = dao.getAverageDefensive();
            cachedHomeFocus = dao.getAverageHomeFocus();
            cachedTotalMoves = dao.getTotalTurnsAnalyzed();
        });
    }

    public static void logHumanTurn(Context context, LudoGameEngine engine, int playerIdx, int chosenTokenIdx, List<Integer> validTokens) {
        if (validTokens == null || validTokens.isEmpty()) return;

        int diceValue = engine.getDiceValue();
        int chosenStartPos = engine.getTokenPositions()[playerIdx][chosenTokenIdx];
        int chosenNextPos = (chosenStartPos == LudoGameEngine.POSITION_YARD) ? 1 : chosenStartPos + diceValue;
        
        boolean choseCapture = false;
        boolean choseSafeZone = false;
        boolean choseDanger = false;
        boolean wasInDanger = false;
        boolean choseHomeFocus = false;
        
        if (chosenStartPos >= 1 && chosenStartPos <= 51) {
            LudoGameEngine.Point startCoord = engine.getCoordinateForPosition(playerIdx, chosenTokenIdx, chosenStartPos);
            wasInDanger = LudoBot.isDangerZone(engine, playerIdx, startCoord, chosenStartPos);
        }

        if (chosenNextPos >= 1 && chosenNextPos <= 51) {
            LudoGameEngine.Point targetCoord = engine.getCoordinateForPosition(playerIdx, chosenTokenIdx, chosenNextPos);
            choseCapture = LudoBot.checkCaptureAtPosition(engine, playerIdx, targetCoord);
            choseSafeZone = engine.isSafeAtPosition(playerIdx, chosenNextPos);
            choseDanger = LudoBot.isDangerZone(engine, playerIdx, targetCoord, chosenNextPos);
        }
        
        if (chosenNextPos >= 52 && chosenNextPos <= 57) {
            choseHomeFocus = true;
        }

        boolean choseDefensive = choseSafeZone || (wasInDanger && !choseDanger && !choseCapture);

        PlayerTurn turn = new PlayerTurn();
        turn.timestamp = System.currentTimeMillis();
        turn.playerId = playerIdx;
        turn.availableMovesCount = validTokens.size();
        turn.wasAggressive = 0;
        turn.wasRisky = 0;
        turn.wasDefensive = choseDefensive ? 1 : 0;
        turn.wasHomeFocus = choseHomeFocus ? 1 : 0;
        
        if (choseCapture) {
            turn.chosenMoveType = "AGGRESSIVE";
            turn.wasAggressive = 1;
        } else if (choseSafeZone) {
            turn.chosenMoveType = "SAFE";
        } else if (choseDanger) {
            turn.chosenMoveType = "RISKY";
            turn.wasRisky = 1;
        } else {
            turn.chosenMoveType = "NEUTRAL";
        }
        
        dbExecutor.execute(() -> {
            PlayerTurnDao dao = AppDatabase.getInstance(context).playerTurnDao();
            dao.insertTurn(turn);
            cachedAggression = dao.getAverageAggression();
            cachedRiskTolerance = dao.getAverageRiskTolerance();
            cachedDefensive = dao.getAverageDefensive();
            cachedHomeFocus = dao.getAverageHomeFocus();
            cachedTotalMoves = dao.getTotalTurnsAnalyzed();
        });
    }

    private static void initDefaults(android.content.SharedPreferences prefs) {
        // Reset the mistakenly seeded 57/32 stats if we haven't done the reset yet
        if (!prefs.contains("fixed_defaults")) {
            prefs.edit()
                .putInt("games_played", 0)
                .putInt("wins", 0)
                .putBoolean("fixed_defaults", true)
                .apply();
        }
    }

    public static void incrementGamesPlayed(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("LudoStats", Context.MODE_PRIVATE);
        initDefaults(prefs);
        int played = prefs.getInt("games_played", 0);
        prefs.edit().putInt("games_played", played + 1).apply();
    }

    public static void incrementWins(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("LudoStats", Context.MODE_PRIVATE);
        initDefaults(prefs);
        int wins = prefs.getInt("wins", 0);
        prefs.edit().putInt("wins", wins + 1).apply();
    }

    public static int getGamesPlayed(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("LudoStats", Context.MODE_PRIVATE);
        initDefaults(prefs);
        return prefs.getInt("games_played", 0);
    }

    public static int getWins(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("LudoStats", Context.MODE_PRIVATE);
        initDefaults(prefs);
        return prefs.getInt("wins", 0);
    }
}
