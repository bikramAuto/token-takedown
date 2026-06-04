package in.bikdocs.ludo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface PlayerTurnDao {
    @Insert
    void insertTurn(PlayerTurn turn);
    
    @Query("SELECT COALESCE(AVG(wasAggressive), 0.0) FROM turn_history")
    float getAverageAggression();
    
    @Query("SELECT COALESCE(AVG(wasRisky), 0.0) FROM turn_history")
    float getAverageRiskTolerance();

    @Query("SELECT COALESCE(AVG(wasDefensive), 0.0) FROM turn_history")
    float getAverageDefensive();
    
    @Query("SELECT COALESCE(AVG(wasHomeFocus), 0.0) FROM turn_history")
    float getAverageHomeFocus();
    
    @Query("SELECT COUNT(*) FROM turn_history")
    int getTotalTurnsAnalyzed();
}
