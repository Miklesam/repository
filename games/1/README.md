# Flappy Bird Game (Module 1)

A simple Flappy Bird clone built with Kotlin for Android Studio.

## Features

- **Smooth Physics**: Realistic bird physics with gravity and jump mechanics
- **Collision Detection**: Precise collision detection between bird and pipes
- **Scoring System**: Score tracking with best score persistence
- **Touch Controls**: Simple tap-to-jump controls
- **Game States**: Start screen, gameplay, and game over states
- **Visual Elements**: Custom-drawn bird and pipe graphics

## How to Play

1. Tap the screen to start the game
2. Tap to make the bird jump and avoid the pipes
3. Try to get the highest score possible!
4. Tap to restart when you crash

## Game Mechanics

- **Bird Physics**: The bird falls due to gravity and jumps when you tap
- **Pipe Generation**: Pipes spawn randomly with gaps to fly through
- **Scoring**: Earn points by successfully passing through pipe gaps
- **Collision**: Game ends when the bird hits a pipe, ground, or ceiling

## Technical Details

- Built with Kotlin
- Uses custom View for game rendering
- 60 FPS game loop
- Custom drawing with Canvas and Paint
- ViewBinding for UI management

## Installation

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the `games:1` module on an Android device or emulator

Enjoy playing Flappy Bird! 🐦
