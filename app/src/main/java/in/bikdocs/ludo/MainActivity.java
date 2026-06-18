package in.bikdocs.ludo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import android.transition.TransitionManager;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.xml.KonfettiView;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.core.Position;

public class MainActivity extends AppCompatActivity {

    private LudoGameEngine engine;
    private AudioEngine audioEngine;

    private LudoBoardView boardView;
    private ImageView diceImage;
    private com.airbnb.lottie.LottieAnimationView lottieDice;
    private FrameLayout diceButton;
    private KonfettiView konfettiView;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isRolling = false;
    private boolean vsAI = true; // Red vs Green, Yellow, Blue AI by default
    private int activePlayerCount = 4;
    private String[] playerNames = {"Player 1", "Player 2", "Player 3", "Player 4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        engine = new LudoGameEngine();
        audioEngine = new AudioEngine(this);

        android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
        audioEngine.setMuted(prefs.getBoolean("is_muted", false));
        
        activePlayerCount = getIntent().getIntExtra(HomeActivity.EXTRA_PLAYER_COUNT, 4);
        boolean isTeamMode = getIntent().getBooleanExtra("is_team_mode", false);
        String[] intentNames = getIntent().getStringArrayExtra(HomeActivity.EXTRA_PLAYER_NAMES);
        if (intentNames != null && intentNames.length == 4) {
            playerNames = intentNames;
        }
        String saveKey = isTeamMode ? "game_state_team" : "game_state_" + activePlayerCount;
        String savedState = prefs.getString(saveKey, null);

        if (savedState != null && engine.importStateFromJson(savedState)) {
            // Successfully resumed saved state, skip Intent configs
            isTeamMode = engine.isTeamMode();
        } else {
            // No saved state or invalid, read configuration from HomeActivity for new game
            engine.setTeamMode(isTeamMode);
            vsAI = getIntent().getBooleanExtra(HomeActivity.EXTRA_VS_AI, true);

            switch (activePlayerCount) {
                case 2:
                    engine.setPlayerActive(0, true);
                    engine.setPlayerActive(1, false);
                    engine.setPlayerActive(2, true);
                    engine.setPlayerActive(3, false);
                    break;
                case 3:
                    engine.setPlayerActive(0, true);
                    engine.setPlayerActive(1, true);
                    engine.setPlayerActive(2, true);
                    engine.setPlayerActive(3, false);
                    break;
                default:
                    engine.setPlayerActive(0, true);
                    engine.setPlayerActive(1, true);
                    engine.setPlayerActive(2, true);
                    engine.setPlayerActive(3, true);
                    break;
            }

            boolean[] playerIsAI = getIntent().getBooleanArrayExtra("player_ai_types");
            if (playerIsAI == null) {
                // Fallback: Red is human, others default to AI
                playerIsAI = new boolean[]{false, true, true, true};
            }
            engine.setPlayerAI(0, false);
            for (int i = 1; i < 4; i++) {
                engine.setPlayerAI(i, playerIsAI[i] && engine.isPlayerActive(i));
            }
        }

        // Bind layouts
        boardView = findViewById(R.id.ludo_board_view);
        diceImage = findViewById(R.id.dice_image);
        lottieDice = findViewById(R.id.lottie_dice);
        diceButton = findViewById(R.id.dice_button);
        konfettiView = findViewById(R.id.konfettiView);

        // Bind engine to board view and pass audio reference
        boardView.setEngine(engine);
        boardView.setAudioEngine(audioEngine);

        // Configure game controls
        setupListeners();

        // Initialize indicator visibility and player avatars
        for (int i = 0; i < 4; i++) {
            int indicatorId = getResources().getIdentifier("indicator_" + i, "id", getPackageName());
            View indicatorView = findViewById(indicatorId);
            if (indicatorView != null) {
                if (engine.isPlayerActive(i)) {
                    indicatorView.setVisibility(View.VISIBLE);
                } else {
                    indicatorView.setVisibility(View.GONE);
                }
            }

            int avatarId = getResources().getIdentifier("avatar_" + i, "id", getPackageName());
            com.google.android.material.imageview.ShapeableImageView avatarView = findViewById(avatarId);
            if (avatarView != null) {
                if (engine.isPlayerAI(i)) {
                    avatarView.setImageResource(R.drawable.lori_avatar);
                    avatarView.setImageTintList(null);
                    avatarView.setPadding(0, 0, 0, 0);
                    avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    avatarView.setImageResource(R.drawable.ic_person);
                    avatarView.setImageTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
                    int padding = (int) (8 * getResources().getDisplayMetrics().density);
                    avatarView.setPadding(padding, padding, padding, padding);
                    avatarView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            }
        }

        // Set player names
        TextView tv0 = findViewById(R.id.tv_name_0);
        TextView tv1 = findViewById(R.id.tv_name_1);
        TextView tv2 = findViewById(R.id.tv_name_2);
        TextView tv3 = findViewById(R.id.tv_name_3);

        if (engine.isTeamMode()) {
            tv0.setFilters(new android.text.InputFilter[0]);
            tv1.setFilters(new android.text.InputFilter[0]);
            tv2.setFilters(new android.text.InputFilter[0]);
            tv3.setFilters(new android.text.InputFilter[0]);
            
            tv0.setText(playerNames[0] + " (T-RY)");
            tv1.setText(playerNames[1] + " (T-GB)");
            tv2.setText(playerNames[2] + " (T-RY)");
            tv3.setText(playerNames[3] + " (T-GB)");
        } else {
            tv0.setText(playerNames[0]);
            tv1.setText(playerNames[1]);
            tv2.setText(playerNames[2]);
            tv3.setText(playerNames[3]);
        }

        updateUI();

        // Check if first player is bot (should not be, but safe check)
        checkBotTurn();
    }

    private void setupListeners() {
        android.view.View btnHome = findViewById(R.id.btn_home);
        android.view.View btnReset = findViewById(R.id.btn_reset);

        btnHome.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> showRestartConfirmation());

        diceButton.setOnClickListener(v -> {
            if (isRolling || engine.isDiceRolled())
                return;
            // Only roll if current player is human
            int movingIdx = engine.getMovingPlayerIndex();
            if (!engine.isPlayerAI(movingIdx)) {
                performDiceRollFlow();
            }
        });

        boardView.setOnTokenClickListener((playerIdx, tokenIdx) -> {
            // Player clicks a token to move
            if (isRolling || !engine.isDiceRolled())
                return;
            int movingPlayerIdx = engine.getMovingPlayerIndex();
            if (playerIdx != movingPlayerIdx)
                return;
            if (engine.isPlayerAI(movingPlayerIdx))
                return; // bot should choose

            moveToken(playerIdx, tokenIdx);
        });
    }

    private void performDiceRollFlow() {
        isRolling = true;
        audioEngine.playRollSound();

        int rolled = engine.rollDice();

        diceImage.setVisibility(View.GONE);
        lottieDice.setVisibility(View.VISIBLE);
        lottieDice.playAnimation();
        
        animateDiceJump(() -> {
            lottieDice.cancelAnimation();
            lottieDice.setVisibility(View.GONE);
            diceImage.setVisibility(View.VISIBLE);
            setDiceIcon(rolled);
            isRolling = false;

            // Handle triple-six cancellation rules
            boolean turnCancelled = engine.handleSixRollRules();
            if (turnCancelled) {
                updateUI();
                checkBotTurn();
                return;
            }

            // Identify valid moves
            int movingIdx = engine.getMovingPlayerIndex();
            List<Integer> validMoves = engine.getValidMoves(movingIdx);
            boardView.setValidMoves(validMoves);

            if (validMoves.isEmpty()) {
                // Auto pass after brief delay
                uiHandler.postDelayed(() -> {
                    engine.nextTurn();
                    updateUI();
                    checkBotTurn();
                }, 1200);
            } else if (validMoves.size() == 1) {
                // Auto move if there is only 1 option
                int movingPlayerIdx = engine.getMovingPlayerIndex();
                uiHandler.postDelayed(() -> {
                    moveToken(movingPlayerIdx, validMoves.get(0));
                }, 600); // Slight delay so the user sees the dice result before token moves
            }
        });
    }

    private void moveToken(int playerIdx, int tokenIdx) {
        if (!engine.isPlayerAI(playerIdx)) {
            List<Integer> validMoves = engine.getValidMoves(playerIdx);
            PlayerStatsLogger.logHumanTurn(this, engine, playerIdx, tokenIdx, validMoves);
        }

        int startPos = engine.getTokenPositions()[playerIdx][tokenIdx];
        int dice = engine.getDiceValue();
        int targetPos = (startPos == LudoGameEngine.POSITION_YARD) ? 1 : startPos + dice;

        // Clear moves highlighting
        boardView.setValidMoves(new java.util.ArrayList<>());

        // Perform board step-by-step animation
        boardView.animateTokenMovement(playerIdx, tokenIdx, startPos, targetPos, () -> {
            // Apply gameplay changes in engine
            LudoGameEngine.MoveResult res = engine.executeMove(playerIdx, tokenIdx);

            if (res.captureOccurred) {
                audioEngine.playCaptureSound();
            } else if (res.landedSafeZone) {
                audioEngine.playSafeSound();
            } else if (res.reachedGoal) {
                audioEngine.playSafeSound();
            }

            // Check Winner
            if (engine.checkWinner(playerIdx) && !engine.getFinishedRankings().contains(playerIdx)) {
                engine.markPlayerFinished(playerIdx);
                audioEngine.playWinSound();

                // If human player finishes 1st, increment wins
                if (playerIdx == 0 && engine.getFinishedRankings().size() == 1) {
                    PlayerStatsLogger.incrementWins(this);
                }

                if (engine.isGameOver()) {
                    String finalSaveKey = engine.isTeamMode() ? "game_state_team" : "game_state_" + activePlayerCount;
                    getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE).edit().remove(finalSaveKey).apply();
                    PlayerStatsLogger.incrementGamesPlayed(this);
                    showMediumCelebration();
                    showLeaderboardDialog();
                } else {
                    showSmallCelebration(playerIdx);
                    updateUI();
                    checkBotTurn();
                }
                return;
            }

            updateUI();
            checkBotTurn();
        });
    }

    private void checkBotTurn() {
        int movingIdx = engine.getMovingPlayerIndex();
        if (engine.isPlayerAI(movingIdx)) {
            // Schedule bot actions with delays for nice UI pacing
            uiHandler.postDelayed(() -> {
                if (engine.isDiceRolled() || isFinishing() || isDestroyed())
                    return;
                performBotRoll();
            }, 1200);
        }
    }

    private void performBotRoll() {
        isRolling = true;
        audioEngine.playRollSound();

        int rolled = engine.rollDice();

        diceImage.setVisibility(View.GONE);
        lottieDice.setVisibility(View.VISIBLE);
        lottieDice.playAnimation();
        
        animateDiceJump(() -> {
            if (!isDestroyed() && !isFinishing()) {
                lottieDice.cancelAnimation();
                lottieDice.setVisibility(View.GONE);
                diceImage.setVisibility(View.VISIBLE);
                setDiceIcon(rolled);
                isRolling = false;

                boolean turnCancelled = engine.handleSixRollRules();
                if (turnCancelled) {
                    updateUI();
                    checkBotTurn();
                    return;
                }

                // AI logic here...
                int movingPlayerIdx = engine.getMovingPlayerIndex();
                List<Integer> validMoves = engine.getValidMoves(movingPlayerIdx);

                if (validMoves.isEmpty()) {
                    engine.nextTurn();
                    updateUI();
                    checkBotTurn();
                } else {
                    // Let Bot select best move
                    int selectedToken = LudoBot.selectBestMove(engine, movingPlayerIdx, validMoves);
                    uiHandler.postDelayed(() -> moveToken(movingPlayerIdx, selectedToken), 1000);
                }
            }
        });
    }

    private void setDiceIcon(int val) {
        DiceDrawable diceDrawable = new DiceDrawable(val);
        diceImage.setImageDrawable(diceDrawable);
    }

    private void animateDiceJump(Runnable onFinish) {
        lottieDice.setTranslationY(0f);
        lottieDice.animate()
                .translationY(-100f)
                .scaleX(2.5f)
                .scaleY(2.5f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    lottieDice.animate()
                            .translationY(0f)
                            .scaleX(1.8f)
                            .scaleY(1.8f)
                            .setDuration(400)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .withEndAction(onFinish)
                            .start();
                })
                .start();
    }

    private void updateUI() {
        LudoGameEngine.PlayerColor cur = engine.getCurrentPlayer();
        int color = getPlayerColorValue(engine.getMovingPlayerIndex());

        // Update dice button background to current player color
        diceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        // Refresh dice icon or show crown if waiting to roll
        if (engine.isDiceRolled()) {
            int diceVal = engine.getDiceValue();
            setDiceIcon(diceVal);
        } else {
            diceImage.setImageResource(R.drawable.ic_crown);
            diceImage.setImageTintList(null);
        }

        // Ensure correct visibility state if updateUI is called during abnormal states
        if (!isRolling) {
            if (lottieDice != null)
                lottieDice.setVisibility(View.GONE);
            if (diceImage != null)
                diceImage.setVisibility(View.VISIBLE);
        }

        // Enable/Disable roll button
        if (engine.isDiceRolled() || engine.isPlayerAI(engine.getMovingPlayerIndex())) {
            diceButton.setAlpha(0.5f);
            diceButton.setClickable(false);
        } else {
            diceButton.setAlpha(1.0f);
            diceButton.setClickable(true);
        }

        // Move dice physically to the active player's placeholder
        FrameLayout targetPlaceholder = null;
        switch (engine.getMovingPlayerIndex()) {
            case 0:
                targetPlaceholder = findViewById(R.id.placeholder_0);
                break;
            case 1:
                targetPlaceholder = findViewById(R.id.placeholder_1);
                break;
            case 2:
                targetPlaceholder = findViewById(R.id.placeholder_2);
                break;
            case 3:
                targetPlaceholder = findViewById(R.id.placeholder_3);
                break;
        }

        if (targetPlaceholder != null && diceButton.getParent() != targetPlaceholder) {
            ViewGroup parent = (ViewGroup) diceButton.getParent();
            if (parent != null) {
                parent.removeView(diceButton);
            }
            // Ensure layout params match the placeholder
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            targetPlaceholder.addView(diceButton, params);
        }

        // Update pointers visibility based on whose turn it is
        int currentTurn = engine.getMovingPlayerIndex();
        for (int i = 0; i < 4; i++) {
            int pointerId = getResources().getIdentifier("pointer_" + i, "id", getPackageName());
            View pointerView = findViewById(pointerId);
            if (pointerView != null) {
                if (i == currentTurn && engine.isPlayerActive(i)) {
                    pointerView.setVisibility(View.VISIBLE);
                } else {
                    pointerView.setVisibility(View.INVISIBLE);
                }
            }
        }

        boardView.invalidate();
    }

    private int getPlayerColorValue(int playerIdx) {
        switch (playerIdx) {
            case 0:
                return Color.parseColor("#FF4B4B");
            case 1:
                return Color.parseColor("#2ECC71");
            case 2:
                return Color.parseColor("#F1C40F");
            case 3:
                return Color.parseColor("#3498DB");
            default:
                return Color.GRAY;
        }
    }

    private void showRestartConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Game")
                .setMessage("Are you sure you want to restart Ludo Board Play?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    engine.resetGame();
                    String resetSaveKey = engine.isTeamMode() ? "game_state_team" : "game_state_" + activePlayerCount;
                    getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE).edit().remove(resetSaveKey).apply();
                    updateUI();
                    checkBotTurn();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showSmallCelebration(int playerIdx) {
        int color = getPlayerColorValue(playerIdx);
        EmitterConfig emitterConfig = new Emitter(100L, TimeUnit.MILLISECONDS).max(100);

        float x = 0f, y = 0f;
        if (playerIdx == 0) {
            x = 0f;
            y = 1f;
        } else if (playerIdx == 1) {
            x = 0f;
            y = 0f;
        } else if (playerIdx == 2) {
            x = 1f;
            y = 0f;
        } else if (playerIdx == 3) {
            x = 1f;
            y = 1f;
        }

        konfettiView.start(
                new PartyFactory(emitterConfig)
                        .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                        .colors(Arrays.asList(color, Color.WHITE))
                        .setSpeedBetween(0f, 30f)
                        .position(new Position.Relative(x, y))
                        .build());
        Toast.makeText(this, playerNames[playerIdx] + " Finished!", Toast.LENGTH_SHORT)
                .show();
    }

    private void showMediumCelebration() {
        EmitterConfig emitterConfig = new Emitter(300L, TimeUnit.MILLISECONDS).max(300);
        konfettiView.start(
                new PartyFactory(emitterConfig)
                        .spread(360)
                        .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                        .colors(Arrays.asList(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED, Color.BLUE))
                        .setSpeedBetween(0f, 30f)
                        .position(new Position.Relative(0.5, 0.3))
                        .build());
    }

    private void showLeaderboardDialog() {
        if (engine.isTeamMode()) {
            List<Integer> rankings = engine.getFinishedRankings();
            boolean team1Finished = rankings.contains(0) && rankings.contains(2);
            String winningTeam;
            String partner1;
            String partner2;
            if (team1Finished) {
                winningTeam = "Team Red-Yellow";
                partner1 = playerNames[0];
                partner2 = playerNames[2];
            } else {
                winningTeam = "Team Green-Blue";
                partner1 = playerNames[1];
                partner2 = playerNames[3];
            }

            new AlertDialog.Builder(this)
                    .setTitle("Victory! 🏆")
                    .setMessage(winningTeam + " has won Ludo Team Mode!\n\nCooperative Partners:\n• " + partner1 + "\n• " + partner2 + "\n\nAll 8 tokens have been secured home!")
                    .setCancelable(false)
                    .setPositiveButton("Play Again", (dialog, which) -> {
                        engine.resetGame();
                        updateUI();
                        checkBotTurn();
                    })
                    .show();
            return;
        }

        List<Integer> rankings = engine.getFinishedRankings();
        StringBuilder sb = new StringBuilder();

        int lastPlayer = -1;
        for (int i = 0; i < 4; i++) {
            if (engine.isPlayerActive(i) && !rankings.contains(i)) {
                lastPlayer = i;
                break;
            }
        }

        String[] places = { "1st", "2nd", "3rd", "4th" };
        for (int i = 0; i < rankings.size(); i++) {
            sb.append(places[i]).append(" Place: ").append(playerNames[rankings.get(i)]).append("\n");
        }

        if (lastPlayer != -1) {
            sb.append(places[rankings.size()]).append(" Place: ").append(playerNames[lastPlayer]).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Game Over! 🏆")
                .setMessage(sb.toString())
                .setCancelable(false)
                .setPositiveButton("Play Again", (dialog, which) -> {
                    engine.resetGame();
                    updateUI();
                    checkBotTurn();
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!engine.isGameOver()) {
            android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
            String saveKey = engine.isTeamMode() ? "game_state_team" : "game_state_" + activePlayerCount;
            prefs.edit().putString(saveKey, engine.exportStateToJson()).apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioEngine != null) {
            audioEngine.shutdown();
        }
    }
}
