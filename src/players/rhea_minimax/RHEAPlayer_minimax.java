package players.rhea_minimax;

import core.GameState;
import players.Player;
import players.optimisers.ParameterizedPlayer;
import players.rhea_minimax.utils.RHEAParams_minimax;
import utils.ElapsedCpuTimer;
import utils.Types;

import java.util.Random;

import static players.rhea_minimax.utils.Constants.TIME_BUDGET;

public class RHEAPlayer_minimax extends ParameterizedPlayer {
    private RollingHorizonPlayer player;
    private GameInterface gInterface;
    private RHEAParams_minimax params;

    public RHEAPlayer_minimax(long seed, int playerID) {
        this(seed, playerID, new RHEAParams_minimax());
    }

    public RHEAPlayer_minimax(long seed, int playerID, RHEAParams_minimax params) {
        super(seed, playerID, params);
        reset(seed, playerID);
    }

    @Override
    public int[] get_predictions(int tick) {
        //MM get predictions of opponent moves for accuracy estimation (for itself it would produce zeros)
        int[] predicted_actions = new int[Types.NUM_PLAYERS];
        for (int p=0; p<Types.NUM_PLAYERS; p++) {
            predicted_actions[p] = player.ea.predicted_actions[p][tick];
        }
        return predicted_actions;
    }

    @Override
    public void print_accuracy(){
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);

        // Make sure we have parameters
        this.params = (RHEAParams_minimax) getParameters();
        if (this.params == null) {
            this.params = new RHEAParams_minimax();
            super.setParameters(this.params);
        }

        // Set up random generator
        Random randomGenerator = new Random(seed);

        // Create interface with game
        gInterface = new GameInterface(this.params, randomGenerator, playerID - Types.TILETYPE.AGENT0.getKey());

        // Set up player
        player = new RollingHorizonPlayer(randomGenerator, this.params, gInterface, playerID);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        ElapsedCpuTimer elapsedTimer = null;
        if (params.budget_type == TIME_BUDGET) {
            elapsedTimer = new ElapsedCpuTimer();
            elapsedTimer.setMaxTimeMillis(params.time_budget);
        }
        setup(gs, elapsedTimer);
        Types.ACTIONS action = gInterface.translate(player.getAction(elapsedTimer, gs.nActions()));

        //MM populate action_buffer with selected action
        int tick = gs.getTick();
        gs.actions_buffer[playerID-10][tick] = this.player.getBestAction(0);

        return action;
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
        return new RHEAPlayer_minimax(seed, playerID, params);
    }
}
