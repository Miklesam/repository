# 2048 Game (Module 2)

A classic 2048 puzzle game built with Kotlin for Android Studio.

## Features

- **Classic 2048 Gameplay**: Slide numbered tiles to combine them and reach 2048
- **Swipe Controls**: Intuitive swipe gestures to move tiles in all directions
- **Score Tracking**: Real-time score updates with best score persistence
- **Beautiful UI**: Authentic 2048 styling with proper tile colors and animations
- **Game States**: Win condition detection and game over handling
- **Responsive Design**: Adapts to different screen sizes

## How to Play

1. **Objective**: Combine tiles with the same number to create tiles with higher values
2. **Goal**: Reach the 2048 tile to win the game
3. **Controls**: Swipe up, down, left, or right to move all tiles in that direction
4. **Scoring**: Each tile merge adds points to your score
5. **Game Over**: The game ends when the board is full and no moves are possible

## Game Mechanics

- **Tile Movement**: All tiles move in the direction of your swipe
- **Tile Merging**: Two tiles with the same number combine into one tile with double the value
- **New Tiles**: After each move, a new tile (2 or 4) appears in a random empty space
- **Scoring**: Points are awarded based on the value of merged tiles
- **Win Condition**: Reach the 2048 tile to win
- **Game Over**: No more moves available when the board is full

## Technical Details

- Built with Kotlin
- Custom View for game rendering
- Swipe gesture detection for controls
- SharedPreferences for best score persistence
- Material Design 3 theming
- Responsive layout with proper scaling

## Tile Values and Colors

- **2**: Light beige (#EEE4DA)
- **4**: Light brown (#EDE0C8)
- **8**: Orange (#F2B179)
- **16**: Dark orange (#F59563)
- **32**: Red-orange (#F67C5F)
- **64**: Red (#F65E3B)
- **128**: Gold (#EDCF72)
- **256**: Gold (#EDCC61)
- **512**: Gold (#EDC850)
- **1024**: Gold (#EDC53F)
- **2048**: Gold (#EDC22E)

## Installation

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the `games:2` module on an Android device or emulator

## Game Features

- **New Game**: Start a fresh game anytime
- **Score Tracking**: Current score and best score display
- **Win Detection**: Automatic win detection when reaching 2048
- **Game Over Detection**: Automatic game over when no moves are available
- **Reset Functionality**: Easy game reset with confirmation dialog

Enjoy playing 2048! 🎯





