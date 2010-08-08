/*** * Mages: Multiplayer Game Engine for mobile devices * Copyright (C) 2008 aksonov *  * This library is free software; you can redistribute it and/or * modify it under the terms of the GNU Lesser General Public * License as published by the Free Software Foundation; either * version 2 of the License, or (at your option) any later version. *  * This library is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU * Lesser General Public License for more details. *  * You should have received a copy of the GNU Lesser General Public * License along with this library; if not, write to the Free Software * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA *  * Contact: aksonov dot gmail dot com * * Author: Pavlo Aksonov */package org.aksonov.mages.server;import java.util.ArrayList;import java.util.Hashtable;import java.util.List;import java.util.Vector;import org.aksonov.mages.BaseGameTimerCallback;import org.aksonov.mages.Board;import org.aksonov.mages.GameTimer;import org.aksonov.mages.entities.GameSettings;import org.aksonov.mages.entities.Move;import org.aksonov.mages.entities.Note;import org.aksonov.mages.entities.PlayerInfo;import org.aksonov.tools.Log;import org.aksonov.tools.StdoutLogger;import org.apache.log4j.Category;import org.mega.gasp.event.DataEvent;import org.mega.gasp.event.EndEvent;import org.mega.gasp.event.JoinEvent;import org.mega.gasp.event.QuitEvent;import org.mega.gasp.event.StartEvent;import org.mega.gasp.event.impl.DataEventImpl;import org.mega.gasp.event.impl.EndEventImpl;import org.mega.gasp.platform.Actor;import org.mega.gasp.platform.ActorSession;import org.mega.gasp.platform.impl.PlatformImpl;import org.mega.gasp.server.GASPServer;// TODO: Auto-generated Javadoc/** * Base class for any game server in Mages engine, implements all base * functionality except one abstract method - game board creation (which is * specific for concrete game)  *  * @author Pavel */public abstract class MagesServer extends GASPServer {	// GASP logger	/** The cat. */	protected Category cat;		/** The is started. */	private boolean isStarted = false;		/** The is ended. */	private boolean isEnded = false;	// hashtable to store player informations, key aSID, value an object	// representing player	/** The players. */	protected List players = new ArrayList();		/** The player infos. */	private Hashtable<Integer, PlayerInfo> playerInfos = new Hashtable<Integer, PlayerInfo>();	/** The player ids. */	protected int[] playerIds = new int[10];	/** The settings. */	private GameSettings settings = null;		/** The board. */	private Board board;		/** The game timer. */	private GameTimer gameTimer;		/** The winner. */	private int winner = -1;	static {		Log.setLogger(StdoutLogger.instance);	}		/**	 * Creates the board.	 * 	 * @return the board	 */	public abstract Board createBoard();				// constructor	/**	 * Instantiates a new mages server.	 */	protected MagesServer() {		super();		cat = Category.getInstance("GASPLogging");		cat.debug("Log4J Server Category instanciated!");	}	/**	 * On move.	 * 	 * @param move	 *            the move	 * @param aSID	 *            the a sid	 */	public void onMove(Move move, int aSID) {		if (!isStarted) {			cat.debug("Game is NOT STARTED yet");		} else {			int currentPlayer = board.getCurrentPlayer();			// making move			if (board.makeMove(move)) {				// switch timer to other player				gameTimer.start(board.getCurrentPlayer());				// get total time				int time = (int) gameTimer.getTime(currentPlayer);				// setting time to move				cat.debug("Setting time for move to " + time);				move.setTime(time);				settings.moves.add(move);				// notify to all players				notifyToAllPlayers(move, aSID);								if (board.isGameOver()){					int arg1 = board.getScore((byte)0);					int arg2 = board.getScore((byte)1);					Note notification = Note.createEndOfGame(							Note.GAME_OVER_REASON, playerIds[currentPlayer],							arg1, arg2);					// limit					notifyToAllPlayers(notification, aSID);										// end game					EndEvent e = new EndEventImpl(0);					appIns.onEndEvent(e);				}			} else {				Log.d("Server", "Move: " + move + " is not valid!");			}		}	}	/**	 * Start.	 */	public void start() {		cat.debug("Server START running!");	}		/**	 * Calculate rating.	 * 	 * @param info	 *            the info	 * @param settings	 *            the settings	 * @param board	 *            the board	 * 	 * @return the int	 */	protected int calculateRating(PlayerInfo info, GameSettings settings, Board board){		return info.rating + board.getScore(info.player) - 50 ;	}	/**	 * End.	 */	public void end() {		cat.debug("End of game");		if (settings != null && settings.players.size() > 0) {			EndEvent end = new EndEventImpl(settings.players.get(0).arg1);			for (int i = 0; i < settings.players.size(); i++) {				ActorSession actor = appIns.getActorSession(settings.players						.get(i).arg1);				if (actor != null) {				if (masterApp.getActor(actor.getActorID()) != null && playerInfos.get(actor.getActorSessionID())!=null && settings.rated){					PlayerInfo info = playerInfos.get(actor.getActorSessionID());					Log.d("MagesServer", "Changing rating of player " + info +" Game result: " + board.getScore((byte)0) + " " + board.getScore((byte)1));					Actor a = masterApp.getActor(actor.getActorID());					info.rating = calculateRating(info, settings, board);					a.setRating(info.rating);					Log.d("MagesServer", "end() - setting new rating=" + info.rating + " for actor " + a.getUsername());					PlatformImpl.getPlatform().getDBManager().saveRating(a.getActorID(), a.getRating());									}				actor.raiseEvent(end);				}			}			settings = null;		}		cat.debug("Server STOP running!");	}	// GASP event listeners	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#onStartEvent(org.mega.gasp.event.StartEvent)	 */	public void onStartEvent(StartEvent e) {		cat.debug(("STARTEVENT received by server: aSID>" + e				.getActorSessionID()));		start();		if (settings == null) {			cat.debug("onStartEvent: Game settings is null, so just return");			return;		}		if (playerInfos.get(e.getActorSessionID()) == null) {			cat					.debug("onStartEvent: Player info doesn't exist for actor session: "							+ e.getActorSessionID());			return;		}		playerInfos.get(e.getActorSessionID()).ready = true;	}	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#onEndEvent(org.mega.gasp.event.EndEvent)	 */	public void onEndEvent(EndEvent e) {		cat				.debug(("ENDEVENT received by server: aSID>" + e						.getActorSessionID()));		end();	}	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#onJoinEvent(org.mega.gasp.event.JoinEvent)	 */	public void onJoinEvent(JoinEvent e) {		cat.debug("JOINEVENT received by server: board is " + board + ", aSID>"				+ e.getActorSessionID() + " username>" + e.getUsername());		if (!players.contains(e.getActorSessionID())) {			players.add(e.getActorSessionID());		}	}	/**	 * Start game.	 */	private void startGame() {		isStarted = true;		gameTimer.start(board.getCurrentPlayer());	}	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#onQuitEvent(org.mega.gasp.event.QuitEvent)	 */	public void onQuitEvent(QuitEvent e) {		cat.debug(("QUITEVENT received by server: aSID>" + e				.getActorSessionID()));		players.remove(("" + e.getActorSessionID()));		playerInfos.remove(e.getActorSessionID());		int found = -1;		for (int i = 0; i < settings.players.size(); i++) {			if (settings.players.get(i).arg1 == e.getActorSessionID()) {				found = i;			}		}		if (found >= 0) {			settings.players.remove(found);		}		if (checkActivePlayers() < settings.minActors) {			stopGame();		}	}	/**	 * Stop game.	 */	public void stopGame() {		isStarted = false;		gameTimer.pause();	}	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#onDataEvent(org.mega.gasp.event.DataEvent)	 */	public void onDataEvent(DataEvent e) {		cat.debug(("DATAEVENT received by server: aSID>"				+ e.getActorSessionID() + " Hashtable size>" + e.getData()				.size()));		Hashtable h1 = e.getData();		int nbObj = h1.size();		for (int i = 0; i < nbObj; i++) {			Object o = h1.get((i + ""));			treatMessage(o, e.getActorSessionID());		}	}	/**	 * On game settings.	 * 	 * @param s	 *            the s	 * @param senderASID	 *            the sender asid	 */	protected void onGameSettings(GameSettings s, final int senderASID) {		cat.debug("Receiving game settings from ASID: " + senderASID + ", "				+ settings);		if (appIns.getOwnerAID() == senderASID && settings == null) {			cat.debug("Sender is owner, so we are setting settings");			this.settings = s;			if (settings.players.size() > 0) {				throw new IllegalArgumentException(						"Player list should be empty for initial game");			}			this.board = createBoard();			for (int i = 0; i < settings.moves.size(); i++) {				board.makeMove(settings.moves.get(i));			}			gameTimer = new GameTimer(new BaseGameTimerCallback() {				@Override				public void onTimeChanged(int player, long totalTime,						long moveTime) {					if (!isStarted || settings == null) {						return;					}					if (settings != null && totalTime > settings.timePerGame) {						cat.debug("!!!!!!Player " + playerIds[player]								+ " exceeded total time: " + totalTime);						isStarted = false;						gameTimer.reset();					}					if (settings != null && moveTime > settings.timePerMove) {						cat.debug("!!!!!!Player " + playerIds[player]								+ " exceeded move time: " + totalTime);						isStarted = false;						gameTimer.reset();					}					if (settings != null && !isStarted) {						cat.debug("Creation END_GAME notification");						int arg1 = player == 0 ? 0 : 100;						int arg2 = player == 0 ? 100 : 0;						board.setScore((byte)0, arg1);						board.setScore((byte)1, arg2);						Note notification = Note.createEndOfGame(								Note.TIME_LIMIT_REASON, playerIds[player],								arg1, arg2);						// limit						notifyToAllPlayers(notification, senderASID);												// end game						EndEvent e = new EndEventImpl(0);						appIns.onEndEvent(e);					}				}			}, settings.moveIncr);			gameTimer.start();			appIns.setCustomData(settings);			masterApp.updateLastAIListChangedTime(); // update flag for			// lobby list update		}	}	/**	 * On note.	 * 	 * @param note	 *            the note	 * @param sender	 *            the sender	 */	protected void onNote(Note note, int sender) {		cat.debug("onNote " + note);		if (playerInfos.get(sender) == null) {			cat.debug("Unknown sender! " + sender);			return;		}		if (note.type == Note.PROPOSE_TYPE && note.reason == Note.RESIGN) {			cat.debug("onNote got resign, generate end of game event");			note.dispose();			PlayerInfo info = playerInfos.get(sender);			byte player = info.player;			int arg1 = player == 0 ? 0 : 100;			int arg2 = player == 0 ? 100 : 0;						board.setScore((byte)0, arg1);			board.setScore((byte)1, arg2);			note = Note.createEndOfGame(Note.RESIGN, info.id, arg1, arg2);			notifyToAllPlayers(note, sender);			// end game			EndEvent e = new EndEventImpl(0);			appIns.onEndEvent(e);		} else {			notifyToAllPlayers(note, sender);		}	}	/* (non-Javadoc)	 * @see org.mega.gasp.server.GASPServer#couldJoin(org.aksonov.mages.entities.PlayerInfo)	 */	@Override	public boolean couldJoin(PlayerInfo info) {		if (settings == null) {			return false;		}		for (int i = 0; i < settings.players.size(); i++) {			PlayerInfo p = settings.players.get(i);			if (info.player != -1 && p.player == info.player && p.id != info.id) {				return false;			}		}		return true;	}	/**	 * On player info.	 * 	 * @param info	 *            the info	 * @param sender	 *            the sender	 */	protected void onPlayerInfo(PlayerInfo info, int sender) {		cat.debug("!!!Setting custom data for actor session: " + sender				+ ", data: " + info);		if (sender == 0) {			throw new IllegalArgumentException("Sender (ASID) is null!");		}		info.arg1 = sender;		ActorSession actorSession = appIns.getActorSession(sender);		info.id = actorSession.getActorID();		// send existing players to this player;		sendMessagesToPlayer(new Vector(settings.players), sender);		if (!settings.players.contains(info)) {			settings.players.add(info);		}		if (info.player >= 0)			playerIds[info.player] = info.id;		playerInfos.put(sender, info);		info.setId(actorSession.getActorID());		info.setUsername(actorSession.getPseudoName());		// send info to all actors		notifyToAllPlayers(info, sender);		masterApp.updateLastAIListChangedTime(); // update flag for		// lobby list		int number = checkActivePlayers();		if (number >= settings.minActors) {			Log.d("Server", "Start game, we have " + number + " players");			// send settings to all actors			notifyToAllPlayers(settings, sender);			startGame();		} else {			Log.d("Server", "Cannot start game, only " + number + " players");		}	}	// treat specifically all custom types objects	/**	 * Treat message.	 * 	 * @param o	 *            the o	 * @param senderASID	 *            the sender asid	 */	public synchronized void treatMessage(Object o, int senderASID) {		if (isEnded) {			cat.debug("Game is ended, we don't accept anything");		}		if (o instanceof GameSettings) {			onGameSettings((GameSettings) o, senderASID);		} else if (o instanceof Move) {			onMove((Move) o, senderASID);		} else if (o instanceof Note) {			onNote((Note) o, senderASID);		} else if (o instanceof PlayerInfo) {			onPlayerInfo((PlayerInfo) o, senderASID);		}	}	// traditional methods used	/**	 * Notify to all players.	 * 	 * @param o	 *            the o	 * @param sender	 *            the sender	 */	private void notifyToAllPlayers(Object o, int sender) {		cat.debug("notifyAllPlayers called");		Hashtable h = new Hashtable();		h.put("0", o);		DataEvent de = new DataEventImpl(sender, h);		for (int i = 0; i < players.size(); i++) {			int pid = (Integer) players.get(i);			sendDataTo(pid, de);		}	}	/**	 * Check active players.	 * 	 * @return the int	 */	private int checkActivePlayers() {		int number = 0;		Log				.d("Server", "Current number of players: "						+ settings.players.size());		for (int j = 0; j < settings.players.size(); j++) {			PlayerInfo p = settings.players.get(j);			Log.d("Server", "Player: " + p.id +", postion:" + p.player);			if (p.player >= 0) {				number++;			}		}		return number;	}	/**	 * Send messages to player.	 * 	 * @param v	 *            the v	 * @param playerASID	 *            the player asid	 */	private void sendMessagesToPlayer(Vector v, int playerASID) {		cat.debug("sendMessagesToPlayer called");		if (v.size() == 0)			return;		Hashtable h = new Hashtable();		for (int i = 0; i < v.size(); i++) {			h.put(("" + i), v.get(i));		}		DataEvent e = new DataEventImpl(playerASID, h);		sendDataTo(playerASID, e);	}}