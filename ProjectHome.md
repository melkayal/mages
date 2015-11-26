# News #
08/08/2010: Fixed network API issues, project is in alpha testing now with latest Android 2.2 api

06/30/2010: Project is still alive! However API 0.9 used by initial version of Mages is very incompatible with latest Google Android API. This requires some efforts to make Mages back to work. Latest client version have been committed to trunk, but there are still issues connected with network API.

Anyone who are willing to help please send email to owner(s) to became commiter for this project. Thanks!

# Overview #

Mages is multiplayer client/server game engine for Android and other mobile devices (currently Android and J2ME devices are supported, development for Windows Mobile in the progress). It allows developers to create internet multiplayer games by implementing only core game logic and GUI by using powerful engine API.

Developers can reuse effective Comet-based engine networking protocol, common game tasks like login to game server, retrieving of active player list, list of available game sessions, create new game session, join existing game, invite other player, just chat with opponents and many other features.

If a developer is implementing board game like Monopoly, Poker, Bridge http://en.wikipedia.org/wiki/Board_game or turn-based strategy http://en.wikipedia.org/wiki/Turn-based_strategy the engine provides advanced timer control (time limit per move, per game and optional incremental time after each made move), game history (user could view game history back/forward in read-only mode), rating support, moves confirmation mechanism (server will send game moves until client confirms reception) for bad quality GPRS connection and restoring made moves after occasional quit from the game (because of connection drop or other issues).

Transport layer (i.e. how game players will communicate each other) is separated into independent services to allow pluggable exchange. Server side is based on GASP game platform http://gasp.objectweb.org  with a lot of additional features (following major features were added to GASP: Comet support, base game entities management, timer control, rating support, active player list load, game invitation, move confirmation and automatic reload of player list and game list based on change timestamps - no extra traffic)

# Features #
  * Support for pluggable games
  * Support for pluggable AI programs
  * Minimal latency with Comet-based protocol (single connection is used during game play    - Tomcat NIO functionality is utilized)
  * Minimal traffic (valuable for GPRS networks and roaming)
  * Pluggable and configurable transport layer (the engine provides two services - local server for play with programs and GASP server for networking multiplayer game)
  * Localization support
  * Player ratings store/load/calculate support (developers may also override rating calculations for their games)
  * Advanced timer control (max time per move, max time per game, move increment time)
  * Create/join game session, connect/disconnect to the game server
  * Invitation to game session support and other common game operations
  * Chat messaging to communicate during play
  * Possibility to resign during the game
  * Ready Chess GUI with both keyboard, touch input and resizable board - ready to be integrated with world online chess servers
  * Asynchronous  programming is used almost anywhere where it is possible to avoid delays
  * Android users could play with J2ME users since GASP contains ready J2ME client which works with the same protocol. Sure, it doesn’t have so many features and possibilities but will bring numerous users for the game servers.
  * Open source license will allow to improve the engine and add more and more features.
