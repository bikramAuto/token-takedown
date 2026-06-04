package in.bikdocs.ludo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AILearningActivity extends AppCompatActivity {

    private TextView tvSummary;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_learning);

        tvSummary = findViewById(R.id.tv_analysis_summary);
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        loadAndRenderStats();
    }

    private void loadAndRenderStats() {
        tvSummary.setText("Loading AI metrics...\n");
        
        dbExecutor.execute(() -> {
            PlayerTurnDao dao = AppDatabase.getInstance(this).playerTurnDao();
            
            float aggro = dao.getAverageAggression();
            float risk = dao.getAverageRiskTolerance();
            float defensive = dao.getAverageDefensive();
            float home = dao.getAverageHomeFocus();
            int totalMoves = dao.getTotalTurnsAnalyzed();
            int gamesPlayed = PlayerStatsLogger.getGamesPlayed(this);
            
            new Handler(Looper.getMainLooper()).post(() -> {
                renderProfile(aggro, risk, defensive, home, totalMoves, gamesPlayed);
            });
        });
    }

    private void renderProfile(float aggro, float risk, float def, float home, int moves, int games) {
        android.widget.LinearLayout container = findViewById(R.id.stats_container);
        container.removeAllViews();

        int aggroPct = (int) (aggro * 100);
        int riskPct = (int) (risk * 100);
        int defPct = (int) (def * 100);
        int homePct = (int) (home * 100);

        addStatRow(container, "Aggression", aggroPct);
        addStatRow(container, "Risk Taking", riskPct);
        addStatRow(container, "Defensive Play", defPct);
        addStatRow(container, "Home Focus", homePct);

        TextView summary = findViewById(R.id.tv_analysis_summary);
        summary.setText("Games Analyzed: " + games + "\n" +
                        "Moves Analyzed: " + moves);
    }

    private void addStatRow(android.widget.LinearLayout container, String label, int percentage) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 0, 0, 16);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(android.graphics.Color.parseColor("#2ECC71"));
        tvLabel.setTextSize(15f);
        tvLabel.setTypeface(android.graphics.Typeface.MONOSPACE);
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(labelParams);

        TextView tvBar = new TextView(this);
        tvBar.setText(getProgressBar(percentage) + " " + percentage + "%");
        tvBar.setTextColor(android.graphics.Color.parseColor("#2ECC71"));
        tvBar.setTextSize(15f);
        tvBar.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvBar.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(tvLabel);
        row.addView(tvBar);
        container.addView(row);
    }

    private String getProgressBar(int percentage) {
        int filled = percentage / 10;
        int empty = 10 - filled;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append("█");
        for (int i = 0; i < empty; i++) sb.append("░");
        return sb.toString();
    }
}
