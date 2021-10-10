package players;

import core.GameState;
import utils.Types;

public class DoNothingPlayer extends Player {
    public DoNothingPlayer(int pId) {
        super(0, pId);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
        return Types.ACTIONS.ACTION_STOP;
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }
    @Override
    public int[] get_predictions(int x) {
        int [] res = new int[4];
        return res;
    }

    @Override
    public void print_accuracy(){
    }

    @Override
    public Player copy() {
        return new DoNothingPlayer(playerID);
    }
}
