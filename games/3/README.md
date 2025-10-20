# Match 3 Game

A classic Match 3 puzzle game built for Android using Kotlin.

## Features

- **8x8 Game Board**: Play on an 8x8 grid with colorful gems
- **6 Different Gem Types**: Red, Blue, Green, Yellow, Purple, and Orange gems
- **Match 3+ Mechanics**: Match 3 or more gems of the same color to score points
- **Chain Reactions**: Create cascading matches for higher scores
- **Limited Moves**: Complete the game within 30 moves
- **Smooth Animations**: Enjoy fluid gem swapping and matching animations
- **Score System**: Earn 10 points for each matched gem

## How to Play

1. **Tap a Gem**: Select a gem by tapping on it
2. **Swap Adjacent Gems**: Tap an adjacent gem to swap positions
3. **Create Matches**: Form lines of 3 or more gems of the same color
4. **Watch the Magic**: Matched gems disappear and new ones fall from the top
5. **Chain Reactions**: Create multiple matches in sequence for bonus points
6. **Manage Moves**: You have 30 moves to score as many points as possible

## Game Mechanics

- Only adjacent gems (horizontally or vertically) can be swapped
- Matches must be at least 3 gems in a line (horizontal or vertical)
- If a swap doesn't create a match, the gems automatically swap back
- Gems fall down to fill empty spaces after matches
- New gems are randomly generated to fill the top of the board
- The game ends when you run out of moves

## Technical Details

- Built with Kotlin for Android
- Uses GridLayout for the game board
- Implements custom animations for gem swapping
- Responsive UI with Material Design principles
- Optimized for performance with efficient board state management

## Controls

- **Tap**: Select and swap gems
- **New Game Button**: Start a fresh game

Enjoy the addictive Match 3 gameplay!
