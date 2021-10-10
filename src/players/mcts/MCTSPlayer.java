package players.mcts;

import core.GameState;
import players.mcts.op_m.limited_buffer;
import players.mcts.op_m.unlimited_buffer;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import utils.ElapsedCpuTimer;
import utils.Types;

import java.util.ArrayList;
import java.util.Random;

public class MCTSPlayer extends ParameterizedPlayer {

    /**
     * Random generator.
     */
    private Random m_rnd;
    public players.mcts.op_m.limited_buffer limited_buffer;
    public players.mcts.op_m.unlimited_buffer unlimited_buffer;

    /**
     * All actions available.
     */
    public Types.ACTIONS[] actions;
    public int[][] predictions;

    /**
     * Params for this MCTS
     */
    public MCTSParams params;

    public MCTSPlayer(long seed, int id) {
        this(seed, id, new MCTSParams());
    }

    public MCTSPlayer(long seed, int id, MCTSParams params) {
        super(seed, id, params);
        reset(seed, id);

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        actions = new Types.ACTIONS[actionsList.size()];
        int i = 0;
        for (Types.ACTIONS act : actionsList) {
            actions[i++] = act;
        }
        this.limited_buffer = new limited_buffer(20, m_rnd);
        this.unlimited_buffer = new unlimited_buffer(m_rnd);
        predictions = new int[Types.NUM_PLAYERS][Types.MAX_GAME_TICKS];
    }

    @Override
    public void print_accuracy(){
    }
    @Override
    public int[] get_predictions(int tick) {
        int[] res = new int[Types.NUM_PLAYERS];
        for (int p=0; p<Types.NUM_PLAYERS; p++) {
            res[p] = predictions[p][tick];
        }
        return res;
    }
    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        m_rnd = new Random(seed);
        this.limited_buffer = new limited_buffer(20, m_rnd);
        this.unlimited_buffer = new unlimited_buffer(m_rnd);

        this.params = (MCTSParams) getParameters();
        if (this.params == null) {
            this.params = new MCTSParams();
            super.setParameters(this.params);
        }
    }

    public void update_counts(GameState gs)
    {
        int[][] actions = gs.actions_buffer;
        if (params.op_model == params.OP_UNLIMITED_BUFFER) {
            this.unlimited_buffer.update_counts(actions);
        } else if (params.op_model == params.OP_LIMITED_BUFFER) {
            this.limited_buffer.update_counts(actions);
        }
    }


    @Override
    public Types.ACTIONS act(GameState gs) {

        // TODO update gs
        if (gs.getGameMode().equals(Types.GAME_MODE.TEAM_RADIO)){
            int[] msg = gs.getMessage();
        }

        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(params.num_time);

        // Number of actions available
        int num_actions = actions.length;

        // Root of the tree
        SingleTreeNode m_root = new SingleTreeNode(params, m_rnd, num_actions, actions, this);
        m_root.setRootGameState(gs);
        //MM update counts for buffer opponent models
        this.update_counts(gs);

        //Determine the action using MCTS...
        m_root.mctsSearch(ect);

        //MM fill predictions of opponent actions array
        int tick = gs.getTick();;
        for (int p=0; p<Types.NUM_PLAYERS; p++) {
            predictions[p][tick] = m_root.predictions[p];
        }

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();

        // TODO update message memory

        //... and return it.
        return actions[action];
    }

    @Override
    public int[] getMessage() {
        // default message
        int[] message = new int[Types.MESSAGE_LENGTH];
        message[0] = 1;
        return message;
    }

    @Override
    public Player copy() {
        return new MCTSPlayer(seed, playerID, params);
    }
}