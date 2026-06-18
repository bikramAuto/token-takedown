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
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_COUNT = "player_count";
    public static final String EXTRA_VS_AI = "vs_ai";
    public static final String EXTRA_PLAYER_NAMES = "player_names";

    private int selectedPlayerCount = 2; // default (1v1)
    private ViewPager2 viewPager;
    private View sectionPlayerNames;
    private View containerStartButton;
    private View btnSettingsCircle;
    private boolean isCardSelectionExpanded = false;
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

        viewPager = findViewById(R.id.view_pager_cards);
        sectionPlayerNames = findViewById(R.id.section_player_names);
        containerStartButton = findViewById(R.id.container_start_button);
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

        btnSettingsCircle = findViewById(R.id.btn_settings_circle);

        // Configure ViewPager2
        java.util.List<PlayerCard> cardList = new java.util.ArrayList<>();
        cardList.add(new PlayerCard(2, R.drawable.ic_card_2p, "2 PLAYERS"));
        cardList.add(new PlayerCard(3, R.drawable.ic_card_3p, "3 PLAYERS"));
        cardList.add(new PlayerCard(4, R.drawable.ic_card_4p, "4 PLAYERS"));
        cardList.add(new PlayerCard(4, R.drawable.ic_card_team, "TEAM MODE"));

        CardAdapter adapter = new CardAdapter(cardList);
        if (viewPager != null) {
            viewPager.setAdapter(adapter);
            viewPager.setOffscreenPageLimit(4);
            
            // Page transformer for card styling, scale and transparency
            viewPager.setPageTransformer((page, position) -> {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.85f + r * 0.15f);
                page.setScaleX(0.85f + r * 0.15f);
                page.setAlpha(0.5f + r * 0.5f);
                
                com.google.android.material.card.MaterialCardView cardRoot = page.findViewById(R.id.card_root);
                if (cardRoot != null) {
                    if (Math.abs(position) < 0.15f) {
                        cardRoot.setStrokeWidth(0);
                        cardRoot.setCardElevation(dpToPx(8));
                    } else {
                        cardRoot.setStrokeWidth(0);
                        cardRoot.setCardElevation(dpToPx(2));
                    }
                }
            });

            // Page change callback
            viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (isCardSelectionExpanded) {
                        expandPlayerNameCard(position);
                    } else {
                        if (position == 3) {
                            selectedPlayerCount = 4;
                            isTeamModeSelected = true;
                        } else {
                            selectedPlayerCount = position + 2;
                            isTeamModeSelected = false;
                        }
                        updateModeSelectionUI();
                    }
                }
            });
        }

        // Default selection highlight
        updateModeSelectionUI();

        // Game Mode click listeners (now disabled since Game Mode section is hidden)
        if (cardModeClassic != null) {
            cardModeClassic.setOnClickListener(v -> {});
        }
        if (cardModeTeam != null) {
            cardModeTeam.setOnClickListener(v -> {});
        }

        // Animated entrances
        animateEntrance(viewPager, 100);

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
            sectionGameMode.setVisibility(View.GONE);
        }

        updateStartButtonText();
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

    // ViewPager2 Card model & adapter implementation
    private static class PlayerCard {
        final int playerCount;
        final int imageRes;
        final String title;

        PlayerCard(int playerCount, int imageRes, String title) {
            this.playerCount = playerCount;
            this.imageRes = imageRes;
            this.title = title;
        }
    }

    private class CardAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CardAdapter.CardViewHolder> {
        private final java.util.List<PlayerCard> items;

        CardAdapter(java.util.List<PlayerCard> items) {
            this.items = items;
        }

        @androidx.annotation.NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_card, parent, false);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull CardViewHolder holder, int position) {
            PlayerCard item = items.get(position);
            holder.imageView.setImageResource(item.imageRes);

            holder.cardRoot.setOnClickListener(v -> onCardClicked(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class CardViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final com.google.android.material.card.MaterialCardView cardRoot;
            final ImageView imageView;

            CardViewHolder(android.view.View itemView) {
                super(itemView);
                cardRoot = itemView.findViewById(R.id.card_root);
                imageView = itemView.findViewById(R.id.card_image);
            }
        }
    }

    private void onCardClicked(int position) {
        if (viewPager.getCurrentItem() != position) {
            viewPager.setCurrentItem(position, true);
            return;
        }

        if (isCardSelectionExpanded) {
            collapsePlayerNameCard();
        } else {
            expandPlayerNameCard(position);
        }
    }

    private void expandPlayerNameCard(int position) {
        if (position == 3) {
            selectedPlayerCount = 4;
            isTeamModeSelected = true;
        } else {
            selectedPlayerCount = position + 2;
            isTeamModeSelected = false;
        }
        isCardSelectionExpanded = true;

        // Hide the settings gear
        if (btnSettingsCircle != null) {
            btnSettingsCircle.setVisibility(View.GONE);
            if (isMenuExpanded) {
                toggleSettingsMenu();
            }
        }
        
        // Hide player cards
        if (viewPager != null) {
            viewPager.animate().alpha(0f).setDuration(250).withEndAction(() -> viewPager.setVisibility(View.GONE)).start();
        }

        // Show names card and start button
        if (sectionPlayerNames != null) {
            sectionPlayerNames.setVisibility(View.VISIBLE);
            sectionPlayerNames.setAlpha(0f);
            sectionPlayerNames.animate().alpha(1f).setDuration(250).start();
        }
        if (containerStartButton != null) {
            containerStartButton.setVisibility(View.VISIBLE);
            containerStartButton.setAlpha(0f);
            containerStartButton.animate().alpha(1f).setDuration(250).start();
        }

        // Hide game mode selector completely since Team Mode is now a primary card
        if (sectionGameMode != null) {
            sectionGameMode.setVisibility(View.GONE);
        }

        updatePlayerNameVisibility();
        updatePlayerNameHints();
    }

    private void collapsePlayerNameCard() {
        isCardSelectionExpanded = false;

        // Show settings gear
        if (btnSettingsCircle != null) {
            btnSettingsCircle.setVisibility(View.VISIBLE);
            btnSettingsCircle.setAlpha(0f);
            btnSettingsCircle.animate().alpha(1f).setDuration(200).start();
        }
        
        // Show player cards
        if (viewPager != null) {
            viewPager.setVisibility(View.VISIBLE);
            viewPager.animate().alpha(1f).setDuration(250).start();
        }

        // Hide names card, start button, and game mode card
        if (sectionPlayerNames != null) {
            sectionPlayerNames.setVisibility(View.GONE);
        }
        if (containerStartButton != null) {
            containerStartButton.setVisibility(View.GONE);
        }
        if (sectionGameMode != null) {
            sectionGameMode.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isCardSelectionExpanded) {
            collapsePlayerNameCard();
        } else if (isMenuExpanded) {
            toggleSettingsMenu();
        } else {
            super.onBackPressed();
        }
    }
}
