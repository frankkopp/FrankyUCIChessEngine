# Franky UCI Chess Engine
Java based chess engine using the UCI protocol to communicate with chess UIs like Arena, xBoard, WinBoard, Fritz, etc.

Franky 1.0
==============================================
(c) Frank Kopp 2019

Released as JAR or EXE (http://www.jar2exe.com/)

Credits:
- https://www.chessprogramming.org
- TalkChess.org
- Flux, Pulse / Phokham Nonava
- Stockfish / Tord Romstad, Marco Costalba, Joona Kiiski and Gary Linscott
- Crafty / Robert Hyatt
- Beowulf / Colin Frayn
- Mediocre / Jonatan Pettersson
- Bruce Moreland & GERBIL (http://www.brucemo.com/compchess/programming/index.htm)
- CPW-Engine / Pawel Koziol, Edmund Moshammer
- DarkThought / Ernst A. Heinz
- Arena Chess GUI / Martin Blume (Germany) et al
- and many more

Features:
- UCI
- 0x88 Board
    - Zobrist hashing
    - Some bitboards e.g. diagonals
- Hashtable
- Pondering
- Opening Book
- Quiescence
- AlphaBeta
- Killer Moves
- Principal Variation Search
- Aspiration Window Search (in Root)
- Mate Distance Pruning
- Minor Promotion Pruning
- Reverse Futility Pruning
- Null Move Pruning with Verification
- Razor
- Internal Iterative Deepening
- Search Extensions
- Futility Prunings
    - Limited Razoring
    - Extended Futility Pruning
    - Futility Pruning (also in Quiescence Search)
- Late Move Reduction
- Evaluation
    - Static
    - Material
    - Mobility
    - Piece Position
    - King Safety (Simple)
    - Game Phase based evaluation of mid- and end game value (Tapered)

ToDo:
- SEE
- Attacks
- SMP (multi threading)
- More and better evaluations (e.g. development, pawn structure, control, end game)
- More bitboards operations - where it is an improvement to 0x88

Strength:
  - WAC Suite 5sec
  - Successful: 276 (92 Prozent)
  - Failed:      24 (8 Prozent)

EOL:
   - est. 1880

Test results:
- 01: 00 Franky-0.12        491.0/880 ·······
- 02: Flux-1.0              37.0/40    36-2-2
- 03: Mediocre_v0.5         34.5/40    32-3-5
- 04: Beowulf               33.0/40    29-3-8
- 05: Abbess2018            28.5/40   28-11-1
- 06: Bismark_1.4           28.0/40   27-11-2
- 07: Adam3.3               27.0/40   25-11-4
- 08: Clarabit_100_x64_win  26.5/40   23-10-7
- 09: La Dame Blanche 2.0c  26.0/40   24-12-4
- 10: GERBIL                24.5/40   23-14-3
- 10: RamJet_014            24.5/40   22-13-5
- 12: Roce39                19.5/40   17-18-5
- 13: Predateur2.2.1_x64    17.0/40   15-21-4
- 14: Clueless              16.5/40   16-23-1
- 15: Ranita_24             14.5/40   14-25-1
- 16: Belzebub_067          14.0/40   12-24-4
- 17: Fmax                  11.5/40    8-25-7
- 17: SEE                   8.0/40     7-31-2
- 18: Piranha               5.5/40     5-34-1
- 19: Nanook v0.17          3.5/40     1-34-5
- 20: UsurpatorIIemu        1.0/40     1-39-0
- 21: VirutorChessUci_1.1.2 0.0/40     0-40-0
- 21: Ram                   0.0/40     0-40-0-
