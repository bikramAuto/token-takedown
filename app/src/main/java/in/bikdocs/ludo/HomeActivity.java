package in.bikdocs.ludo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_COUNT = "player_count";
    public static final String EXTRA_VS_AI = "vs_ai";
    public static final String EXTRA_PLAYER_NAMES = "player_names";

    private int selectedPlayerCount = 2; // default (1v1)
    private LinearLayout card2, card3, card4;
    private MaterialButton startButton;
    private boolean isTeamModeSelected = false;
    private LinearLayout cardModeClassic, cardModeTeam;
    private TextView tvModeClassicTitle, tvModeTeamTitle;
    private LinearLayout sectionGameMode;
    private EditText inputName1, inputName2, inputName3, inputName4;
    private ImageView avatar1, avatar2, avatar3, avatar4;
    private LinearLayout rowPlayer2, rowPlayer3, rowPlayer4;
    
    // Per-player AI toggle controls
    private ImageButton btnToggleAi2, btnToggleAi3, btnToggleAi4;
    private boolean[] playerIsAI = {false, true, true, true}; // Player 1 (index 0) is Human, others default to AI
    private View dimOverlay;

    // Settings radial menu fields
    private boolean isMenuExpanded = false;
    private View btnMenuRule;
    private View btnMenuThumbsUp;
    private View btnMenuEnvelope;
    private View btnMenuGamepad;
    private View btnMenuMusic;
    private View btnMenuSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        card2 = findViewById(R.id.card_2p);
        card3 = findViewById(R.id.card_3p);
        card4 = findViewById(R.id.card_4p);
        startButton = findViewById(R.id.btn_start);
        dimOverlay = findViewById(R.id.dim_overlay);
        cardModeClassic = findViewById(R.id.card_mode_classic);
        cardModeTeam = findViewById(R.id.card_mode_team);
        tvModeClassicTitle = findViewById(R.id.tv_mode_classic_title);
        tvModeTeamTitle = findViewById(R.id.tv_mode_team_title);
        sectionGameMode = findViewById(R.id.section_game_mode);
        ImageView logoDice = findViewById(R.id.logo_dice);
        View logoGlow = findViewById(R.id.logo_glow);

        // Name inputs
        inputName1 = findViewById(R.id.input_name_1);
        inputName2 = findViewById(R.id.input_name_2);
        inputName3 = findViewById(R.id.input_name_3);
        inputName4 = findViewById(R.id.input_name_4);

        rowPlayer2 = findViewById(R.id.row_player_2);
        rowPlayer3 = findViewById(R.id.row_player_3);
        rowPlayer4 = findViewById(R.id.row_player_4);

        // AI toggles
        btnToggleAi2 = findViewById(R.id.btn_toggle_ai_2);
        btnToggleAi3 = findViewById(R.id.btn_toggle_ai_3);
        btnToggleAi4 = findViewById(R.id.btn_toggle_ai_4);

        // Avatars
        avatar1 = findViewById(R.id.avatar_1);
        avatar2 = findViewById(R.id.avatar_2);
        avatar3 = findViewById(R.id.avatar_3);
        avatar4 = findViewById(R.id.avatar_4);

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

        // Initialize Player 1 Avatar
        if (avatar1 != null) {
            avatar1.setImageResource(R.drawable.ic_person);
            avatar1.setImageTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
            int pad = dpToPx(5);
            avatar1.setPadding(pad, pad, pad, pad);
        }

        // Setup AI toggle UI states
        updateAllPlayerAiUI();

        // Bind AI toggle clicks
        if (btnToggleAi2 != null) {
            btnToggleAi2.setOnClickListener(v -> {
                playerIsAI[1] = !playerIsAI[1];
                updatePlayerAiUI(2);
            });
        }
        if (btnToggleAi3 != null) {
            btnToggleAi3.setOnClickListener(v -> {
                playerIsAI[2] = !playerIsAI[2];
                updatePlayerAiUI(3);
            });
        }
        if (btnToggleAi4 != null) {
            btnToggleAi4.setOnClickListener(v -> {
                playerIsAI[3] = !playerIsAI[3];
                updatePlayerAiUI(4);
            });
        }

        // Default selection highlight
        updateModeSelectionUI();

        // Card click listeners
        if (card2 != null) {
            card2.setOnClickListener(v -> {
                selectedPlayerCount = 2;
                isTeamModeSelected = false;
                updateModeSelectionUI();
            });
        }
        if (card3 != null) {
            card3.setOnClickListener(v -> {
                selectedPlayerCount = 3;
                isTeamModeSelected = false;
                updateModeSelectionUI();
            });
        }
        if (card4 != null) {
            card4.setOnClickListener(v -> {
                selectedPlayerCount = 4;
                updateModeSelectionUI();
            });
        }

        // Game Mode click listeners
        if (cardModeClassic != null) {
            cardModeClassic.setOnClickListener(v -> {
                if (!isTeamModeSelected) return;
                isTeamModeSelected = false;
                updateModeSelectionUI();
            });
        }
        if (cardModeTeam != null) {
            cardModeTeam.setOnClickListener(v -> {
                if (isTeamModeSelected) return;
                isTeamModeSelected = true;
                selectedPlayerCount = 4;
                updateModeSelectionUI();
            });
        }

        // Animated entrances
        animateEntrance(card2, 100);
        animateEntrance(card3, 200);
        animateEntrance(card4, 300);
        animateEntrance(startButton, 500);

        // Start button listener
        startButton.setOnClickListener(v -> {
            @SuppressWarnings("deprecation")
            Runnable launchMainActivity = () -> {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra(EXTRA_PLAYER_COUNT, isTeamModeSelected ? 4 : selectedPlayerCount);
                intent.putExtra("is_team_mode", isTeamModeSelected);
                
                // Set vsAI flag if at least one active player is AI
                boolean vsAI = false;
                if (isTeamModeSelected) {
                    vsAI = playerIsAI[1] || playerIsAI[2] || playerIsAI[3];
                } else if (selectedPlayerCount == 2) {
                    vsAI = playerIsAI[2]; // Player 3 (index 2) is active in 2P mode
                } else if (selectedPlayerCount == 3) {
                    vsAI = playerIsAI[1] || playerIsAI[2];
                } else if (selectedPlayerCount == 4) {
                    vsAI = playerIsAI[1] || playerIsAI[2] || playerIsAI[3];
                }
                intent.putExtra(EXTRA_VS_AI, vsAI);
                intent.putExtra("player_ai_types", playerIsAI);

                // Collect player names
                String[] names = new String[4];
                names[0] = getPlayerName(inputName1, "Player 1");
                names[1] = getPlayerName(inputName2, "Player 2");
                names[2] = getPlayerName(inputName3, "Player 3");
                names[3] = getPlayerName(inputName4, "Player 4");
                intent.putExtra(EXTRA_PLAYER_NAMES, names);

                startActivity(intent);
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            };

            String saveKey = isTeamModeSelected ? "game_state_team" : "game_state_" + selectedPlayerCount;
            android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
            if (prefs.contains(saveKey)) {
                new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Game in Progress")
                        .setMessage(isTeamModeSelected ? "You have a saved Team Mode game. Do you want to resume it or start a new game?" : "You have a saved game for " + selectedPlayerCount + " players. Do you want to resume it or start a new game?")
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

        // Initialize Settings Menu and circular buttons
        btnMenuRule = findViewById(R.id.btn_menu_rule);
        btnMenuThumbsUp = findViewById(R.id.btn_menu_thumbs_up);
        btnMenuEnvelope = findViewById(R.id.btn_menu_envelope);
        btnMenuGamepad = findViewById(R.id.btn_menu_gamepad);
        btnMenuMusic = findViewById(R.id.btn_menu_music);
        btnMenuSpeaker = findViewById(R.id.btn_menu_speaker);

        android.content.SharedPreferences prefs = getSharedPreferences("LudoSave", android.content.Context.MODE_PRIVATE);
        
        // Setup initial sound states
        boolean isMuted = prefs.getBoolean("is_muted", false);
        ((android.widget.ImageButton) btnMenuSpeaker).setImageResource(isMuted ? R.drawable.ic_sound_off : R.drawable.ic_sound);
        
        boolean isMusicMuted = prefs.getBoolean("is_music_muted", false);
        btnMenuMusic.setAlpha(isMusicMuted ? 0.4f : 1.0f);

        // Click on Settings Gear
        findViewById(R.id.btn_settings_circle).setOnClickListener(v -> {
            toggleSettingsMenu();
        });

        // Dim overlay click collapses the menu
        if (dimOverlay != null) {
            dimOverlay.setOnClickListener(v -> {
                if (isMenuExpanded) {
                    toggleSettingsMenu();
                }
            });
        }

        // Click on Rule Button
        btnMenuRule.setOnClickListener(v -> {
            toggleSettingsMenu();
            startActivity(new Intent(HomeActivity.this, RuleBookActivity.class));
        });

        // Click on Gamepad Button (AI Analyser)
        btnMenuGamepad.setOnClickListener(v -> {
            toggleSettingsMenu();
            startActivity(new Intent(HomeActivity.this, AILearningActivity.class));
        });

        // Click on Speaker Button (Mute Sound)
        btnMenuSpeaker.setOnClickListener(v -> {
            boolean currentMuted = prefs.getBoolean("is_muted", false);
            boolean newMuted = !currentMuted;
            prefs.edit().putBoolean("is_muted", newMuted).apply();
            ((android.widget.ImageButton) btnMenuSpeaker).setImageResource(newMuted ? R.drawable.ic_sound_off : R.drawable.ic_sound);
            android.widget.Toast.makeText(this, newMuted ? "Sound Effects Muted" : "Sound Effects Unmuted", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Click on Music Button (Mute Music)
        btnMenuMusic.setOnClickListener(v -> {
            boolean currentMusicMuted = prefs.getBoolean("is_music_muted", false);
            boolean newMusicMuted = !currentMusicMuted;
            prefs.edit().putBoolean("is_music_muted", newMusicMuted).apply();
            btnMenuMusic.setAlpha(newMusicMuted ? 0.4f : 1.0f);
            android.widget.Toast.makeText(this, newMusicMuted ? "Music Muted" : "Music Unmuted", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Click on Thumbs Up Button (Rate game)
        btnMenuThumbsUp.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Thank you for rating! 👍", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Click on Envelope Button (Contact/Feedback)
        btnMenuEnvelope.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Feedback: ludo@bikdocs.in", android.widget.Toast.LENGTH_LONG).show();
        });
    }

    private String getPlayerName(EditText input, String fallback) {
        if (input == null) return fallback;
        String name = input.getText().toString().trim();
        return name.isEmpty() ? fallback : name;
    }

    private void updateAllPlayerAiUI() {
        updatePlayerAiUI(2);
        updatePlayerAiUI(3);
        updatePlayerAiUI(4);
    }

    private void updatePlayerAiUI(int playerNum) {
        int idx = playerNum - 1;
        boolean isAI = playerIsAI[idx];
        
        EditText input = null;
        ImageView avatar = null;
        ImageButton toggleBtn = null;
        int defaultPad = dpToPx(5);
        
        switch (playerNum) {
            case 2:
                input = inputName2;
                avatar = avatar2;
                toggleBtn = btnToggleAi2;
                break;
            case 3:
                input = inputName3;
                avatar = avatar3;
                toggleBtn = btnToggleAi3;
                break;
            case 4:
                input = inputName4;
                avatar = avatar4;
                toggleBtn = btnToggleAi4;
                break;
        }
        
        if (input == null || avatar == null || toggleBtn == null) return;
        
        if (isAI) {
            input.setText("Lori");
            input.setEnabled(false);
            input.setAlpha(0.6f);
            avatar.setImageResource(R.drawable.lori_avatar);
            avatar.setImageTintList(null);
            avatar.setPadding(0, 0, 0, 0);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            toggleBtn.setImageResource(R.drawable.lori_avatar);
            toggleBtn.setImageTintList(null);
            toggleBtn.setPadding(0, 0, 0, 0);
            toggleBtn.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            String currentText = input.getText().toString();
            if (currentText.equals("Lori") || currentText.trim().isEmpty()) {
                input.setText("Player " + playerNum);
            }
            input.setEnabled(true);
            input.setAlpha(1.0f);
            avatar.setImageResource(R.drawable.ic_person);
            avatar.setImageTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
            avatar.setPadding(defaultPad, defaultPad, defaultPad, defaultPad);
            avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            
            toggleBtn.setImageResource(R.drawable.ic_person);
            toggleBtn.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            int togglePad = dpToPx(7);
            toggleBtn.setPadding(togglePad, togglePad, togglePad, togglePad);
            toggleBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    private void updatePlayerNameVisibility() {
        if (isTeamModeSelected) {
            if (rowPlayer2 != null) rowPlayer2.setVisibility(View.VISIBLE);
            if (rowPlayer3 != null) rowPlayer3.setVisibility(View.VISIBLE);
            if (rowPlayer4 != null) rowPlayer4.setVisibility(View.VISIBLE);
            return;
        }
        switch (selectedPlayerCount) {
            case 2:
                if (rowPlayer2 != null) rowPlayer2.setVisibility(View.GONE);
                if (rowPlayer3 != null) rowPlayer3.setVisibility(View.VISIBLE); // Yellow (index 2) is active in 2P mode
                if (rowPlayer4 != null) rowPlayer4.setVisibility(View.GONE);
                break;
            case 3:
                if (rowPlayer2 != null) rowPlayer2.setVisibility(View.VISIBLE);
                if (rowPlayer3 != null) rowPlayer3.setVisibility(View.VISIBLE);
                if (rowPlayer4 != null) rowPlayer4.setVisibility(View.GONE);
                break;
            case 4:
                if (rowPlayer2 != null) rowPlayer2.setVisibility(View.VISIBLE);
                if (rowPlayer3 != null) rowPlayer3.setVisibility(View.VISIBLE);
                if (rowPlayer4 != null) rowPlayer4.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateChipSelection() {
        styleCard(card2, selectedPlayerCount == 2);
        styleCard(card3, selectedPlayerCount == 3);
        styleCard(card4, selectedPlayerCount == 4);
        updateStartButtonText();
    }

    private void styleCard(LinearLayout card, boolean selected) {
        if (card == null) return;
        if (selected) {
            card.setBackgroundResource(R.drawable.bg_player_chip_selected);
            card.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
        } else {
            card.setBackgroundResource(R.drawable.bg_player_chip);
            card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    private void animateEntrance(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(40f);
        view.animate()
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
                startButton.setText("Start Game");
            } else {
                startButton.setText("Start Game");
            }
        }
    }

    private void toggleSettingsMenu() {
        isMenuExpanded = !isMenuExpanded;
        
        float endAlpha = isMenuExpanded ? 1.0f : 0.0f;
        float endScale = isMenuExpanded ? 1.0f : 0.5f;
        int visibility = isMenuExpanded ? View.VISIBLE : View.GONE;

        View[] menuItems = {btnMenuRule, btnMenuThumbsUp, btnMenuEnvelope, btnMenuGamepad, btnMenuMusic, btnMenuSpeaker};
        
        if (isMenuExpanded) {
            if (dimOverlay != null) {
                dimOverlay.setVisibility(View.VISIBLE);
                dimOverlay.setAlpha(0f);
                dimOverlay.animate().alpha(1f).setDuration(300).start();
            }
        } else {
            if (dimOverlay != null) {
                dimOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> dimOverlay.setVisibility(View.GONE)).start();
            }
        }

        for (int i = 0; i < menuItems.length; i++) {
            final View item = menuItems[i];
            if (item == null) continue;
            
            if (isMenuExpanded) {
                item.setAlpha(0f);
                item.setScaleX(0.5f);
                item.setScaleY(0.5f);
                item.setVisibility(View.VISIBLE);
                item.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setStartDelay(i * 30) // nice cascade effect!
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
            } else {
                item.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(200)
                    .setStartDelay(0)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> item.setVisibility(View.GONE))
                    .start();
            }
        }

        // Animate settings gear rotation
        View settingsGear = findViewById(R.id.btn_settings_circle);
        if (settingsGear != null) {
            settingsGear.animate()
                .rotation(isMenuExpanded ? 180f : 0f)
                .setDuration(350)
                .start();
        }
    }

    private void updateModeSelectionUI() {
        if (sectionGameMode != null) {
            if (selectedPlayerCount == 4) {
                sectionGameMode.setVisibility(View.VISIBLE);
            } else {
                sectionGameMode.setVisibility(View.GONE);
            }
        }

        if (cardModeClassic == null || cardModeTeam == null || tvModeClassicTitle == null || tvModeTeamTitle == null) return;
        
        if (isTeamModeSelected) {
            cardModeTeam.setBackgroundResource(R.drawable.bg_player_chip_selected);
            tvModeTeamTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_gold));
            cardModeTeam.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();

            cardModeClassic.setBackgroundResource(R.drawable.bg_player_chip);
            tvModeClassicTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            cardModeClassic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();

            if (card2 != null) {
                card2.setEnabled(false);
                card2.setAlpha(0.3f);
            }
            if (card3 != null) {
                card3.setEnabled(false);
                card3.setAlpha(0.3f);
            }
        } else {
            cardModeClassic.setBackgroundResource(R.drawable.bg_player_chip_selected);
            tvModeClassicTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_gold));
            cardModeClassic.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();

            cardModeTeam.setBackgroundResource(R.drawable.bg_player_chip);
            tvModeTeamTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            cardModeTeam.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();

            if (card2 != null) {
                card2.setEnabled(true);
                card2.setAlpha(1.0f);
            }
            if (card3 != null) {
                card3.setEnabled(true);
                card3.setAlpha(1.0f);
            }
        }

        updateChipSelection();
        updatePlayerNameVisibility();
        updatePlayerNameHints();
    }

    private void updatePlayerNameHints() {
        if (inputName1 == null || inputName2 == null || inputName3 == null || inputName4 == null) return;
        if (isTeamModeSelected) {
            inputName1.setHint("Player 1 (Red & Yel)");
            inputName2.setHint("Player 2 (Grn & Blu)");
            inputName3.setHint("Player 3 (Red & Yel)");
            inputName4.setHint("Player 4 (Grn & Blu)");
        } else {
            inputName1.setHint("Player 1");
            inputName2.setHint("Player 2");
            inputName3.setHint("Player 3");
            inputName4.setHint("Player 4");
        }
    }
}
