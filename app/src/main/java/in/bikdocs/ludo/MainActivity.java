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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        engine = new LudoGameEngine();
        audioEngine = new AudioEngine(this);

        android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
        
        activePlayerCount = getIntent().getIntExtra(HomeActivity.EXTRA_PLAYER_COUNT, 4);
        String saveKey = "game_state_" + activePlayerCount;
        String savedState = prefs.getString(saveKey, null);

        if (savedState != null && engine.importStateFromJson(savedState)) {
            // Successfully resumed saved state, skip Intent configs
        } else {
            // No saved state or invalid, read configuration from HomeActivity for new game
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

            engine.setPlayerAI(0, false);
            for (int i = 1; i < 4; i++) {
                engine.setPlayerAI(i, vsAI && engine.isPlayerActive(i));
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

        // Show Lori's DP for active AI bots
        if (engine.isPlayerAI(1)) findViewById(R.id.lori_dp_1).setVisibility(View.VISIBLE);
        if (engine.isPlayerAI(2)) findViewById(R.id.lori_dp_2).setVisibility(View.VISIBLE);
        if (engine.isPlayerAI(3)) findViewById(R.id.lori_dp_3).setVisibility(View.VISIBLE);

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
            int currentIdx = engine.getCurrentPlayerIndex();
            if (!engine.isPlayerAI(currentIdx)) {
                performDiceRollFlow();
            }
        });

        boardView.setOnTokenClickListener((playerIdx, tokenIdx) -> {
            // Player clicks a token to move
            if (isRolling || !engine.isDiceRolled())
                return;
            if (playerIdx != engine.getCurrentPlayerIndex())
                return;
            if (engine.isPlayerAI(playerIdx))
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
            int currentIdx = engine.getCurrentPlayerIndex();
            List<Integer> validMoves = engine.getValidMoves(currentIdx);
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
                uiHandler.postDelayed(() -> {
                    moveToken(currentIdx, validMoves.get(0));
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
                    String finalSaveKey = "game_state_" + activePlayerCount;
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
        int currentIdx = engine.getCurrentPlayerIndex();
        if (engine.isPlayerAI(currentIdx)) {
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
                int currentIdx = engine.getCurrentPlayerIndex();
                List<Integer> validMoves = engine.getValidMoves(currentIdx);

                if (validMoves.isEmpty()) {
                    engine.nextTurn();
                    updateUI();
                    checkBotTurn();
                } else {
                    // Let Bot select best move
                    int selectedToken = LudoBot.selectBestMove(engine, currentIdx, validMoves);
                    uiHandler.postDelayed(() -> moveToken(currentIdx, selectedToken), 1000);
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
        int color = getPlayerColorValue(engine.getCurrentPlayerIndex());

        // Update dice button background to current player color
        diceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        // Refresh dice icon
        int diceVal = engine.isDiceRolled() ? engine.getDiceValue() : 1;
        setDiceIcon(diceVal);

        // Ensure correct visibility state if updateUI is called during abnormal states
        if (!isRolling) {
            if (lottieDice != null)
                lottieDice.setVisibility(View.GONE);
            if (diceImage != null)
                diceImage.setVisibility(View.VISIBLE);
        }

        // Enable/Disable roll button
        if (engine.isDiceRolled() || engine.isPlayerAI(engine.getCurrentPlayerIndex())) {
            diceButton.setAlpha(0.5f);
            diceButton.setClickable(false);
        } else {
            diceButton.setAlpha(1.0f);
            diceButton.setClickable(true);
        }

        // Move dice physically to the active player's placeholder
        FrameLayout targetPlaceholder = null;
        switch (engine.getCurrentPlayerIndex()) {
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
                    String resetSaveKey = "game_state_" + activePlayerCount;
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
        Toast.makeText(this, LudoGameEngine.PlayerColor.values()[playerIdx].name + " Finished!", Toast.LENGTH_SHORT)
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
            LudoGameEngine.PlayerColor pc = LudoGameEngine.PlayerColor.values()[rankings.get(i)];
            sb.append(places[i]).append(" Place: ").append(pc.name).append("\n");
        }

        if (lastPlayer != -1) {
            LudoGameEngine.PlayerColor pc = LudoGameEngine.PlayerColor.values()[lastPlayer];
            sb.append(places[rankings.size()]).append(" Place: ").append(pc.name).append("\n");
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
            String saveKey = "game_state_" + activePlayerCount;
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
