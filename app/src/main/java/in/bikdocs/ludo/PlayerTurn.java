package in.bikdocs.ludo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "turn_history")
public class PlayerTurn {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public long timestamp;
    public int playerId;
    
    public int availableMovesCount;
    public String chosenMoveType; // "AGGRESSIVE", "SAFE", "RISKY", "NEUTRAL"
    
    public int wasAggressive; // 1 or 0
    public int wasRisky; // 1 or 0
    public int wasDefensive; // 1 or 0
    public int wasHomeFocus; // 1 or 0
}
