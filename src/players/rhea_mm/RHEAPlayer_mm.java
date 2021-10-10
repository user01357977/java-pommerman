package players.rhea_mm;

import core.GameState;
import players.Player;
import players.optimisers.ParameterizedPlayer;
import players.rhea_mm.utils.RHEAParams_mm;
import utils.ElapsedCpuTimer;
import utils.Types;

import java.util.Random;

import static players.rhea.utils.Constants.TIME_BUDGET;

public class RHEAPlayer_mm extends ParameterizedPlayer {
    private RollingHorizonPlayer player;
    private GameInterface gInterface;
    private RHEAParams_mm params = new RHEAParams_mm();

    private int[][][] action_space; // [population size][number of player][actions(t)]
    public RHEAPlayer_mm(long seed, int playerID) {
        this(seed, playerID, new RHEAParams_mm());
    }

    public RHEAPlayer_mm(long seed, int playerID, RHEAParams_mm params) {
        super(seed, playerID, params);
        reset(seed, playerID);
    }
    @Override
    public void print_accuracy(){
    }
    @Override
    public int[] get_predictions(int x) {
        int [] res = new int[4];
        return res;
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);

        // Make sure we have parameters
        this.params = (RHEAParams_mm) getParameters();
        if (this.params == null) {
            this.params = new RHEAParams_mm();
            super.setParameters(this.params);
        }

        // Set up random generator
        Random randomGenerator = new Random(seed);

        // Create interface with game
        gInterface = new GameInterface(this.params, randomGenerator, playerID - Types.TILETYPE.AGENT0.getKey());

        // Set up player
        player = new RollingHorizonPlayer(randomGenerator, this.params, gInterface);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        ElapsedCpuTimer elapsedTimer = null;
        if (params.budget_type == TIME_BUDGET) {
            elapsedTimer = new ElapsedCpuTimer();
            elapsedTimer.setMaxTimeMillis(params.time_budget);
        }
        setup(gs, elapsedTimer);
        return gInterface.translate(player.getAction(elapsedTimer, gs.nActions()));
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    private void setup(GameState rootState, ElapsedCpuTimer elapsedTimer) {
        gInterface.initTick(rootState, elapsedTimer);
    }

    @Override
    public Player copy() {
        return new RHEAPlayer_mm(seed, playerID, params);
    }
}
