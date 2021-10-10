package players.mcts.op_m;

import players.mcts.SimpleMultinomialSampler;
import utils.Types;

import java.util.Random;

public class limited_buffer {
    private int player_idx;
    private double[][] probability_distribution;
    private int count;
    private double[][] action_counts;
    private double tick_count;
    private Random random;
    private SimpleMultinomialSampler multinomial_dist;
    private double[][] prob;
    private double buffer_size;

    public limited_buffer(double buffer_size, Random random)
    {
        this.player_idx = player_idx;
        this.action_counts = new double[Types.NUM_PLAYERS][Types.NUM_ACTIONS];
        this.tick_count = 0;
        this.multinomial_dist = new SimpleMultinomialSampler(random);
        this.random = random;
        this.prob = new double[Types.NUM_PLAYERS][Types.NUM_ACTIONS];
        this.buffer_size = buffer_size;
    }

    public void update_counts(int[][] actions)
    {
        int action;
        for (int p=0; p < Types.NUM_PLAYERS; p++) {
            for (int a=0; a < Types.NUM_ACTIONS; a++) {
                this.action_counts[p][a] = 0.0;
            }

            for (int i = this.count; i >= this.count - this.buffer_size & i >= 0; i--) {
                action = actions[p][i];
                if (action == -1) throw new Error("action -1");
                this.action_counts[p][action] += 1.;
            }
        }
        this.tick_count += 1.0;
        this.count += 1;

        double v;
        for (int op_idx=0; op_idx < Types.NUM_PLAYERS; op_idx++) {
            for (int i=0; i<Types.NUM_ACTIONS; i++) {
                v = this.action_counts[op_idx][i];
                this.prob[op_idx][i] = v / this.buffer_size;
            }
        }

    }

    public int get_op_moves(int op_idx)
    {
        int action;
        if (this.count > this.buffer_size) {
            action = get_action_idx(this.multinomial_dist.multinomialSample(1, prob[op_idx]));
            if (action == -1) {
                throw new Error("action -1");
            }
        } else {
            action = random.nextInt(Types.NUM_ACTIONS);
        }
        return action;
    }

    public int get_action_idx(int[] actions)
    {
        int idx = 0;
        for (int action : actions) {
            if (action == 1) {
                return idx;
            } else {
                idx++;
            }
        }
        return -1;
    }
}
