/*
 * MIT License
 *
 * Copyright (c) 2018 Frank Kopp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package fko.javaUCIEngineFramework.Omega;

import fko.chessly.Chessly;
import fko.chessly.Playroom;
import fko.chessly.game.*;
import fko.chessly.mvc.ModelEvents.PlayerDependendModelEvent;
import fko.chessly.mvc.ModelObservable;
import fko.chessly.openingbook.OpeningBookImpl;
import fko.chessly.player.ComputerPlayer;
import fko.chessly.player.Player;
import fko.chessly.player.computer.ObservableEngine;
import fko.chessly.player.computer.Omega.OmegaSearch.SearchResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Chessly Engine implementation using enum and ints instead of objects.<br>
 * Object creation and gc is too expensive for minimax searches.
 *
 */
public class OmegaEngine extends ModelObservable implements ObservableEngine {

    /** read in the default configuration - change the public fields if necessary */
    public final OmegaConfiguration _CONFIGURATION = new OmegaConfiguration();

    // The current game this engine is used in
    private Optional<Game> _game = Optional.empty() ;

    // used to wait for move from search
    private CountDownLatch _waitForMoveLatch = new CountDownLatch(0);

    // the search engine itself
    private OmegaSearch _omegaSearch = null;

    // the search result of the search - null when no result yet
    private SearchResult _searchResult = null;

    // the player owning this engine
    private ComputerPlayer _player = null;
    private GameColor _activeColor = GameColor.NONE;

    // the opening book
    private OpeningBookImpl _openingBook = null;

    // to have a value when the engine is not thinking
    private long _lastUsedTime = 0;
    private long _lastNodesPerSecond = 0;

    // fields used for pondering
    private GameMove _ponderMove;

    /**
     * Constructor - used by factory
     */
    public OmegaEngine() {
        // empty
    }

    /**
     * Initializes the engine
     */
    @Override
    public void init(Player player) {
        // setup our player and color
        if (!(player instanceof ComputerPlayer)) {
            Chessly.fatalError("Engine object can only be used with an instance of ComputerPlayer!");
        }
        this._player = (ComputerPlayer) player;
        this._activeColor = player.getColor();

        // DEBUG: color based configuration
        if (_activeColor.isWhite()) {
            //            _CONFIGURATION._USE_NMP = true;
            //            _CONFIGURATION._USE_VERIFY_NMP = true;
            //            _CONFIGURATION._USE_QUIESCENCE = false;
        } else {
            //            _CONFIGURATION._USE_NMP = false;
            //            _CONFIGURATION._USE_VERIFY_NMP = false;
            //            _CONFIGURATION._USE_QUIESCENCE = false;
        }

        // initialize opening book
        if (_CONFIGURATION._USE_BOOK) {
            String path = _CONFIGURATION._OB_FolderPath + _CONFIGURATION._OB_fileNamePlain;
            _openingBook =   new OpeningBookImpl(this, path, _CONFIGURATION._OB_Mode);
        }

        _omegaSearch = new OmegaSearch(this);

    }

    /**********************************************************************
     * Engine Interface
     **********************************************************************/

    /**
     * Sets the current game.
     * @param game
     */
    @Override
    public void setGame(Game game) {
        this._game = Optional.of(game);
    }

    /**
     * @return the game
     */
    public Optional<Game> getGame() {
        return this._game;
    }

    // not used
    @Override public void setNumberOfThreads(int n) { /*empty*/ }

    /**
     * Starts calculation and returns next move
     * @param gameBoard
     * @return computed move
     *
     * TODO: pondering
     */
    @Override
    public GameMove getNextMove(GameBoard gameBoard) {
        assert gameBoard !=null : "gameBoard must not be null";

        boolean ponderHit = ponderHit(gameBoard);

        // have we been pondering
        if (!_CONFIGURATION._USE_PONDERER || OmegaConfiguration.PERFT || !ponderHit) {
            // or ponder miss
            _omegaSearch.stop();
            _ponderMove = null;
        }

        // convert GameBoard to OmegaBoard
        OmegaBoardPosition omegaBoard = new OmegaBoardPosition(gameBoard);
        assert(gameBoard.toFENString().equals(omegaBoard.toFENString()));

        // tell the ui and the observers out state
        _engineState  = ObservableEngine.THINKING;
        _statusInfo = "Engine calculating.";
        setChanged();
        notifyObservers(new PlayerDependendModelEvent(
                "ENGINE "+_activeColor+" start calculating",
                _player, SIG_ENGINE_START_CALCULATING));

        // Reset all the counters used for the TreeSearchEngineWatcher
        resetCounters();

        // check for move from opening book
        GameMove bookMove = null;
        if (_CONFIGURATION._USE_BOOK && !ponderHit && _openingBook != null) {
            _openingBook.initialize();
            bookMove = _openingBook.getBookMove(gameBoard.toFENString());
            if (bookMove != null) {
                // tell the ui and the observers out state
                _statusInfo = "Book move. Engine waiting.";
                _engineState  = ObservableEngine.IDLE;
                setChanged();
                notifyObservers(new PlayerDependendModelEvent(
                        "ENGINE "+_activeColor+" finished calculating",
                        _player, SIG_ENGINE_FINISHED_CALCULATING));

                return bookMove;
            }
            // unload opening book if not used any more
            _openingBook = null;
            System.gc();
        }

        // configure the search
        int maxDepth; long remainingTime;
        if (_player.getColor().isWhite()) {
            maxDepth = Playroom.getInstance().getCurrentEngineLevelWhite();
            remainingTime = (_game.get().getWhiteTime()-_game.get().getWhiteClock().getTime())/1000;
        } else if (_player.getColor().isBlack()) {
            maxDepth = Playroom.getInstance().getCurrentEngineLevelBlack();
            remainingTime = (_game.get().getBlackTime()-_game.get().getBlackClock().getTime())/1000;
        } else
            throw new RuntimeException("Invalid next player color. Was " + _player.getColor());

        // if not configured will use default mode
        if (_game.get().isTimedGame()) {
            _omegaSearch.configureRemainingTime(
                    remainingTime,
                    maxDepth);
        } else {
            _omegaSearch.configureMaxDepth(maxDepth);
        }

        /*
         * If we have a ponderhit we call _omegaSearch.ponderHit() to reset the
         * search parameters while the search is running.
         * If we do not have a ponderhit we start a regular search
         */
        if (ponderHit) {
            printVerboseInfo(String.format("PONDER HIT%n"));
            _omegaSearch.ponderHit();
        } else {
            printVerboseInfo(String.format("PONDER MISS%n"));
            startSearch(omegaBoard);
        }

        // wait for the result to come in from the search
        try {
            _waitForMoveLatch.await();
        } catch (InterruptedException e) { /*empty*/ }

        // stop the search thread and wait until finished
        _omegaSearch.stop();

        // convert result OmegaMove to GameMove
        GameMove bestMove = OmegaMove.convertToGameMove(_searchResult.bestMove);
        if (bestMove!=null) bestMove.setValue(_searchResult.resultValue);

        // tell the ui and the observers out state
        _statusInfo = "Engine waiting.";
        _engineState  = ObservableEngine.IDLE;
        setChanged();
        notifyObservers(new PlayerDependendModelEvent("ENGINE "+_activeColor+" finished calculating", _player, SIG_ENGINE_FINISHED_CALCULATING));

        // pondering
        if (_CONFIGURATION._USE_PONDERER && !OmegaConfiguration.PERFT) {

            if (_searchResult != null && OmegaMove.isValid(_searchResult.ponderMove)) {

                _ponderMove = OmegaMove.convertToGameMove(_searchResult.ponderMove);

                _engineState  = ObservableEngine.PONDERING;
                _statusInfo = "Engine pondering.";

                // ponder search
                OmegaBoardPosition ponderBoard = new OmegaBoardPosition(omegaBoard);
                // make best move
                ponderBoard.makeMove(_searchResult.bestMove);
                // make ponder move
                ponderBoard.makeMove(_searchResult.ponderMove);

                // no time control - just depth
                _omegaSearch.configurePondering();

                // now ponder...
                startSearch(ponderBoard);

            } else {
                _ponderMove = null;
            }
        }

        return bestMove;
    }

    /**
     * @param omegaBoard
     */
    private void startSearch(OmegaBoardPosition omegaBoard) {
        clearResult();
        // set latch to wait until the OmegaSearch stored a move through
        // the callback to storeResult().
        _waitForMoveLatch = new CountDownLatch(1);
        _omegaSearch.startSearch(omegaBoard);
    }

    /**
     * @see fko.chessly.player.computer.Engine#stopEngine()
     */
    @Override
    public void stopEngine() {
        _omegaSearch.stop();
    }

    /**********************************************************************
     * Omega Engine Methods
     **********************************************************************/


    /**
     * @param gameBoard
     * @return
     */
    private boolean ponderHit(GameBoard gameBoard) {
        GameMove lastMove = gameBoard.getLastMove();
        if (lastMove != null && lastMove.equals(_ponderMove)) {
            return true;
        }
        return false;
    }

    /**
     * Clears the stored result object
     */
    private void clearResult() {
        _searchResult = new SearchResult();
        _searchResult.bestMove = OmegaMove.NOMOVE;
    }

    /**
     * Call back from the OmegaSearch when a result is available.
     * Releases the latch to continue with the result.
     *
     * @param searchResult
     */
    public void storeResult(SearchResult searchResult) {
        _searchResult = searchResult;
        // result received - release the latch
        _waitForMoveLatch.countDown();
    }

    /**
     * @return GameColor color of player for this engine
     */
    public GameColor getActiveColor() {
        return _player.getColor();
    }

    /**
     * @return the player
     */
    public ComputerPlayer getPlayer() {
        return this._player;
    }

    /**
     *
     */
    private void resetCounters() {
        // TODO Auto-generated method stub
    }

    /**********************************************************************
     * ObservableEngine Interface
     **********************************************************************/

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getNumberOfMoves()
     */
    @Override
    public int getNumberOfMoves() {
        return _omegaSearch._rootMoves.size();
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentMoveNumber()
     */
    @Override
    public int getCurrentMoveNumber() {
        return _omegaSearch._currentRootMoveNumber;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentMove()
     */
    @Override
    public GameMove getCurrentMove() {
        return OmegaMove.convertToGameMove(_omegaSearch._currentRootMove);
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentMaxValueMove()
     */
    @Override
    public GameMove getCurrentMaxValueMove() {
        final GameMove bestMove = OmegaMove.convertToGameMove(_omegaSearch._currentBestRootMove);
        if (bestMove != null) {
            bestMove.setValue(_omegaSearch._currentBestRootValue);
        }
        return bestMove;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentSearchDepth()
     */
    @Override
    public int getCurrentSearchDepth() {
        return _omegaSearch._currentIterationDepth;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentMaxSearchDepth()
     */
    @Override
    public int getCurrentMaxSearchDepth() {
        return _omegaSearch._currentExtraSearchDepth;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getTotalNodes()
     */
    @Override
    public long getTotalNodes() {
        return _omegaSearch._nodesVisited;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentNodesPerSecond()
     */
    @Override
    public long getCurrentNodesPerSecond() {
        if (_omegaSearch.isSearching()) {
            _lastNodesPerSecond = (long) ((_omegaSearch._nodesVisited*1000.0F)
                    / Duration.between(_omegaSearch._ponderStartTime, Instant.now()).toMillis()+1);
            return _lastNodesPerSecond;
        }
        return _lastNodesPerSecond;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentUsedTime()
     */
    @Override
    public long getCurrentUsedTime() {
        if (_omegaSearch._timer != null) {
            _lastUsedTime = _omegaSearch._timer.getUsedTime();
        }
        return _lastUsedTime;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getTotalBoards()
     */
    @Override
    public long getTotalBoards() {
        return _omegaSearch._boardsEvaluated;
    }

    /* (non-Javadoc)
     * @see fko.chessly.player.computer.ObservableEngine#getTotalNonQuietBoards()
     */
    @Override
    public long getTotalNonQuietBoards() {
        return _omegaSearch._boardsNonQuiet;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getNodeCacheHits()
     */
    @Override
    public long getNodeCacheHits() {
        return _omegaSearch._nodeCache_Hits;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getNodeCacheMisses()
     */
    @Override
    public long getNodeCacheMisses() {
        return _omegaSearch._nodeCache_Misses;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentNodeCacheSize()
     */
    @Override
    public int getCurrentNodeCacheSize() {
        if (_omegaSearch._transpositionTable == null) return 0;
        return _omegaSearch._transpositionTable.getMaxEntries();
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentNodesInCache()
     */
    @Override
    public int getCurrentNodesInCache() {
        if (_omegaSearch._transpositionTable == null) return 0;
        return _omegaSearch._transpositionTable.getNumberOfEntries();
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentBoardCacheSize()
     */
    @Override
    public int getCurrentBoardCacheSize() {
        if (_omegaSearch._evalCache == null) return 0;
        return _omegaSearch._evalCache.getMaxEntries();
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentBoardsInCache()
     */
    @Override
    public int getCurrentBoardsInCache() {
        if (_omegaSearch._evalCache == null) return 0;
        return _omegaSearch._evalCache.getNumberOfEntries();
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getBoardCacheHits()
     */
    @Override
    public long getBoardCacheHits() {
        return _omegaSearch._evalCache_Hits;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getBoardCacheMisses()
     */
    @Override
    public long getBoardCacheMisses() {
        return _omegaSearch._evalCache_Misses;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentNumberOfThreads()
     */
    @Override
    public int getCurrentNumberOfThreads() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurConfig()
     */
    @Override
    public String getCurConfig() {
        String s = "";
        if (_CONFIGURATION._USE_PONDERER) {
            s += "P,";
        }
        if (_CONFIGURATION._USE_BOOK) {
            s += "OB,";
        }
        if (_CONFIGURATION._USE_NODE_CACHE) {
            s += "NC,";
        }
        if (_CONFIGURATION._USE_BOARD_CACHE) {
            s += "BC,";
        }
        if (_CONFIGURATION._USE_MOVE_CACHE) {
            s += "MC,";
        }
        if (_CONFIGURATION._USE_PRUNING) {
            s += "PRUN,";
        }
        if (_CONFIGURATION._USE_PVS) {
            s += "PVS,";
        }
        if (_CONFIGURATION._USE_MDP) {
            s += "MDP,";
        }
        if (_CONFIGURATION._USE_NMP) {
            s += "NMP,";
        }
        if (_CONFIGURATION._USE_QUIESCENCE) {
            s += "Q,";
        }

        if (OmegaConfiguration.PERFT) {
            s = "PERF TEST";
        }
        return s;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getCurrentPV()
     */
    @Override
    public GameMoveList getCurrentPV() {
        OmegaMoveList pv = _omegaSearch._principalVariation[0];
        int size = pv != null ? pv.size() : 0;
        GameMoveList l = new GameMoveList(size);
        if (size == 0) return l;
        for (int i = 0; i < size; i++) {
            if (pv != null && !pv.empty()) {
                l.add(OmegaMove.convertToGameMove(pv.get(i)));
            }
        }
        return l;
    }

    private String _statusInfo = "";
    @Override
    public String getStatusText() {
        return _statusInfo;
    }

    private int _engineState = ObservableEngine.IDLE;
    @Override
    public int getState() {
        return _engineState;
    }

    /**
     * @see fko.chessly.player.computer.ObservableEngine#getPonderMove()
     */
    @Override
    public GameMove getPonderMove() {
        return _ponderMove;
    }

    @Override
    public String getInfoText() {
        String s;
        // ui and engine write to this object in parallel
        synchronized (_engineInfoText) {
            s = _engineInfoText.toString();
            _engineInfoText.setLength(0);
        }
        return s;
    }

    /* Will store the VERBOSE info until the EngineWatcher collects it. */
    private static final int _engineInfoTextMaxSize = 10000;
    private final StringBuilder _engineInfoText = new StringBuilder(_engineInfoTextMaxSize);
    /**
     * Provide additional information for the UI to collect.
     * E.g. verbose information etc.
     * Size is limited to avoid out of memory.
     * @param info
     */
    @Override
    public void printVerboseInfo(String info) {
        synchronized (_engineInfoText) {
            _engineInfoText.append(info);
            // out of memory protection if the info is not retrieved
            int oversize = _engineInfoText.length() - _engineInfoTextMaxSize;
            if (oversize > 0) _engineInfoText.delete(0, oversize);
        }
        if (_CONFIGURATION.VERBOSE_TO_SYSOUT) System.out.print(info);
    }


    /**
     * This is mainly needed by UnitTest
     *
     * @return the searchResult
     */
    SearchResult getSearchResult() {
        return this._searchResult;
    }

    /** */
    public static final int SIG_ENGINE_START_CALCULATING = 6000;
    /** */
    public static final int SIG_ENGINE_FINISHED_CALCULATING = 6010;
    /** */
    public static final int SIG_ENGINE_START_PONDERING = 6020;
    /** */
    public static final int SIG_ENGINE_FINISHED_PONDERING = 6030;
    /** */
    public static final int SIG_ENGINE_NO_PONDERING = 6040;


}
