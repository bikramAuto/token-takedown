package in.bikdocs.ludo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_COUNT = "player_count";
    public static final String EXTRA_VS_AI = "vs_ai";

    private int selectedPlayerCount = 4; // default
    private MaterialCardView card2, card3, card4;
    private MaterialButton startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        card2 = findViewById(R.id.card_2_players);
        card3 = findViewById(R.id.card_3_players);
        card4 = findViewById(R.id.card_4_players);
        MaterialSwitch aiSwitch = findViewById(R.id.switch_ai);
        startButton = findViewById(R.id.btn_start);
        ImageView logoDice = findViewById(R.id.logo_dice);
        View logoGlow = findViewById(R.id.logo_glow);

        // Set the logo dice drawable
        DiceDrawable diceDrawable = new DiceDrawable(6);
        logoDice.setImageDrawable(diceDrawable);

        // Animate logo glow pulsing
        ObjectAnimator glowAnim = ObjectAnimator.ofFloat(logoGlow, "alpha", 0.4f, 1f);
        glowAnim.setDuration(2000);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.start();

        // Slow rotation on dice logo
        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(logoDice, "rotation", 0f, 360f);
        rotateAnim.setDuration(8000);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.start();

        // Dynamic Version Tag
        android.widget.TextView tvVersion = findViewById(R.id.tv_version);
        if (tvVersion != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                tvVersion.setText("v" + versionName + " • Made with ♥");
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Animated entrances
        animateCardEntrance(card2, 200);
        animateCardEntrance(card3, 350);
        animateCardEntrance(card4, 500);

        // Default selection highlight
        updateCardSelection();

        // Card click listeners
        card2.setOnClickListener(v -> {
            selectedPlayerCount = 2;
            updateCardSelection();
        });
        card3.setOnClickListener(v -> {
            selectedPlayerCount = 3;
            updateCardSelection();
        });
        card4.setOnClickListener(v -> {
            selectedPlayerCount = 4;
            updateCardSelection();
        });

        // Start button
        startButton.setOnClickListener(v -> {
            @SuppressWarnings("deprecation")
            Runnable launchMainActivity = () -> {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra(EXTRA_PLAYER_COUNT, selectedPlayerCount);
                intent.putExtra(EXTRA_VS_AI, aiSwitch.isChecked());
                startActivity(intent);
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            };

            String saveKey = "game_state_" + selectedPlayerCount;
            android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
            if (prefs.contains(saveKey)) {
                new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Game in Progress")
                        .setMessage("You have a saved game for " + selectedPlayerCount + " players. Do you want to resume it or start a new game?")
                        .setPositiveButton("Resume", (dialog, which) -> {
                            launchMainActivity.run();
                        })
                        .setNegativeButton("New Game", (dialog, which) -> {
                            prefs.edit().remove(saveKey).apply();
                            launchMainActivity.run();
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            } else {
                launchMainActivity.run();
            }
        });
        
        // Top-left overflow menu
        android.widget.ImageButton menuButton = findViewById(R.id.btn_menu);
        menuButton.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(HomeActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_rules) {
                    startActivity(new Intent(HomeActivity.this, RuleBookActivity.class));
                    return true;
                } else if (itemId == R.id.action_stats) {
                    startActivity(new Intent(HomeActivity.this, AILearningActivity.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void updateCardSelection() {
        // Reset all cards to unselected state
        styleCard(card2, false);
        styleCard(card3, false);
        styleCard(card4, false);

        // Highlight selected
        switch (selectedPlayerCount) {
            case 2: styleCard(card2, true); break;
            case 3: styleCard(card3, true); break;
            case 4: styleCard(card4, true); break;
        }

        updateStartButtonText();
    }

    private void styleCard(MaterialCardView card, boolean selected) {
        if (selected) {
            card.setStrokeColor(Color.parseColor("#FFD700")); // gold
            card.setStrokeWidth(dpToPx(2.5f));
            card.setCardElevation(dpToPx(8));
            card.setCardBackgroundColor(Color.parseColor("#2A2A2A"));
            // Subtle scale-up
            card.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
        } else {
            card.setStrokeColor(Color.parseColor("#444444")); // grid
            card.setStrokeWidth(dpToPx(1.5f));
            card.setCardElevation(dpToPx(4));
            card.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    private void animateCardEntrance(View card, long delay) {
        card.setAlpha(0f);
        card.setTranslationY(60f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStartButtonText();
    }

    private void updateStartButtonText() {
        if (startButton != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
            String saveKey = "game_state_" + selectedPlayerCount;
            if (prefs.contains(saveKey)) {
                startButton.setText("Resume Game");
            } else {
                startButton.setText("Start Game");
            }
        }
    }
}
