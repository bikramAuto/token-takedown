# Ludo Board Play 🎲

A modern, highly polished, offline Ludo board game for Android. Play with your friends locally or challenge the built-in AI! 

## Features

* **Classic Ludo Rules:** Full support for the standard Ludo ruleset including safe zones, capturing, and consecutive 6-roll bonuses.
* **Flexible Player Configurations:** Choose to play a 2-player, 3-player, or 4-player match.
* **Play vs AI:** Don't have friends around? Toggle the "Play vs AI" switch on the home screen to have the `LudoBot` take over the remaining spots.
* **Seamless Auto-Save:** Never lose your game progress. If you accidentally close the app or hit the back button, your game state is instantly saved. The home screen will intelligently display "Resume Game" to let you pick up exactly where you left off.
* **Low-Latency Audio:** Built on Android's `SoundPool` for instant, lag-free audio feedback. Features custom RPG-style dice clattering and snappy procedural synth tones.
* **Premium UI/UX:** 
  * Sleek dark mode aesthetics with elegant gold accents.
  * Satisfying confetti animations upon finishing a game.
  * Modern top-app bar header controls for resetting or returning home.
* **Player Statistics Tracking:** Automatically tracks your wins and games played.
* **AI Learning Interface:** A fun, retro-terminal style screen simulating AI progress and statistics.

## Screenshots & UI Highlights
* **Home Screen:** Dynamic floating cards for selecting player counts and AI toggling.
* **Rule Book:** A neatly organized, easy-to-read reference for game rules.
* **Game Board:** 100% custom-drawn canvas-based Ludo board with dynamic token rendering.

## Tech Stack

* **Language:** Java
* **Minimum SDK:** 24 (Android 7.0)
* **Target SDK:** 34 (Android 14)
* **UI/Design:** Material Components, Vector Drawables, Custom `View` rendering (Canvas API)
* **Audio:** `SoundPool` for instant SFX
* **Storage:** `SharedPreferences` for auto-save state, `Room` for persistence

## How to Play
1. **Start:** Select the number of players and toggle AI opponents from the Home Screen.
2. **Roll:** Tap the dice to roll. You must roll a **6** to release a token from your yard.
3. **Move:** Tap one of your glowing tokens to move it forward by the rolled amount.
4. **Capture:** Land on an opponent's token (outside a safe star) to send them back to their yard and earn a bonus roll.
5. **Win:** Navigate all 4 of your tokens safely around the board and into the central Home triangle. First player to do so wins!

---
*Made with ♥*
