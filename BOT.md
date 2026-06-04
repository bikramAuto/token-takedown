# Lori (The Ludo AI) Architecture & Mechanics

This document explains how Lori, the AI bot in this Ludo game, thinks, learns, and executes moves under the hood.

## 1. How Lori Thinks & Chooses a Move (The "Brain")
The brain of Lori lives in `LudoBot.java`. When it's her turn, she looks at the dice roll and simulates where every valid token would land if it moved. She then assigns a **Score** to every possible move based on a priority system:

* **Capture (Kill):** +100 points (Highest priority). If a move lands exactly on an opponent's token, Lori will almost always take it.
* **Reach Home Lane:** +50 points. Getting tokens safely into the final stretch is highly prioritized.
* **Safe Zones (Stars):** +20 points. Landing on a star to protect a token from being killed.
* **Break Out of Yard:** +30 points. If she rolls a 6 and has a token stuck in base, she prefers getting it out.
* **Danger Zones (Penalty):** -40 points. Lori simulates the next turn for all opponents. If moving a token puts it in a spot where an opponent could reach it with a roll of 1-6 on their next turn, she subtracts 40 points from that move.
* **Tie Breaker:** If no special rules apply, she adds a tiny score based on how far along the board a token already is, meaning she slightly prefers pushing her leading tokens forward.

Lori simply calculates these scores for all 1 to 4 possible moves, and picks the token with the highest score.

## 2. How Lori "Learns" from You
This happens in `PlayerStatsLogger.java` using a local SQLite Database. Every single time **you (the human)** make a move, the app silently analyzes what you did:

* **Did you choose to kill one of Lori's tokens when you had the chance?** It marks your turn as `AGGRESSIVE`.
* **Did you move a token into the firing line of an opponent?** It marks your turn as `RISKY`.
* **Did you choose to hide on a Star instead of taking a risk?** It marks your turn as `DEFENSIVE`.

The database keeps a running average of your playstyle across all your past games (e.g., "The human is aggressive 65% of the time").

**The Adaptation (Phase 2 Learning):**
Before Lori scores her moves, she checks your stats:
* If your **Aggression is > 50%**, Lori realizes you are bloodthirsty. She dynamically increases her own "Safe Zone Bonus" from +20 to +60. Lori will play extremely defensively, prioritizing hiding on stars to avoid you.
* If your **Risk Tolerance is > 50%**, Lori realizes you make reckless moves. She lowers her own "Danger Penalty" from -40 to -20, meaning she is more willing to leave her tokens exposed as "bait," knowing you are likely to risk your own tokens to come after them.

**Survival + Kill Mode (Endgame AI):**
Before picking a move, Lori checks the entire board. If she detects that *any* opponent has **2 or more tokens** safely in the goal, she flips into a violent survival state:
* **Capture Bonus** spikes to `+500`. She will ruthlessly target opponent tokens to lower their win probability.
* **Danger Penalty** spikes to `-200`. She becomes hyper-cautious, refusing to leave her own tokens exposed.
* **Safe Zone Bonus** spikes to `+100`. She prioritizes staying on gold stars while she hunts.

## 3. Lori's "Hand" (Executing Actions)
If we think of Lori as a physical person sitting across from you at a table, her "body parts" map perfectly to the code.

**Moving the Piece:**
Lori's hand is the `moveToken(int playerIdx, int tokenIdx)` method inside `MainActivity.java`. 
Once Lori's brain (`LudoBot.selectBestMove`) decides *which* token she wants to play, she passes that decision to `moveToken()`. This method acts exactly like a hand picking up the piece:
* It calls `boardView.animateTokenMovement()`, which physically drags the piece across the screen.
* It grabs the results of where the piece landed from the engine (e.g. "Did I capture anyone?").
* It triggers the "thwack" sound effect as the piece hits the board.

**Throwing the Dice:**
When it's time to roll the dice, Lori's hand is the `performBotRoll()` method in `MainActivity.java`. 
This method triggers the sound of the dice rattling, launches the tumbling dice animation onto the screen, and grabs the random number that was rolled.

## 4. Lori's "Eyes"
Lori's eyes are the `engine.getValidMoves()` and `engine.getTokenPositions()` functions. She doesn't "see" the graphics on the screen like you do. Instead, she queries these methods to get a mathematical list of exactly where every piece is currently sitting on the board.

## 5. Timing and Synchronization
* **Pacing:** `MainActivity` uses artificial delays before the bot rolls the dice, and before the bot moves the token. This makes the bot feel like a human player pausing to think.
* **Animation:** The board uses a rendering loop running at 60fps. It moves the token forward one square every 250 milliseconds using a parabolic math function (a `sin` wave) to make the token jump up and down in a 3D-like arch.
* **Audio Sync:** At the exact millisecond each hop begins, it fires the `audioEngine.playStepSound()`, creating a perfectly synced audio/visual impact.
