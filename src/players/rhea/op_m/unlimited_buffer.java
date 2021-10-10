package players.rhea.op_m;

import players.rhea.utils.SimpleMultinomialSampler;
import utils.Types;

import javax.management.RuntimeErrorException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class unlimited_buffer {
    private int player_idx;
    private double[][] probability_distribution;
    private int count;
    private double[][] action_counts;
    private double tick_count;
    private Random random;
    private SimpleMultinomialSampler multinomial_dist;
    private double[][] prob;

    public unlimited_buffer(int player_idx, int num_actions, java.util.Random random)
    {
        this.player_idx = player_idx;
        this.action_counts = new double[Types.NUM_PLAYERS][Types.NUM_ACTIONS];
        this.tick_count = 0;
        this.multinomial_dist = new SimpleMultinomialSampler(random);
        this.random = random;
        this.prob = new double[Types.NUM_PLAYERS][Types.NUM_ACTIONS];
    }

    public void update_counts(int[][] actions)
    {
        if (count != 0) { // ignore counting on first move
            for (int p=0; p< Types.NUM_PLAYERS; p++) {
                int action = actions[p][count - 1];
                if (action == -1) throw new Error("action -1");
                this.action_counts[p][action] += 1.;
            }
            this.tick_count += 1.0;
        }
        this.count += 1;

        double v;
        for (int op_idx=0; op_idx < Types.NUM_PLAYERS; op_idx++) {
            for (int i=0; i<Types.NUM_ACTIONS; i++) {
                v = this.action_counts[op_idx][i];
                if (this.tick_count != 0) { // to avoid null at first move
                    this.prob[op_idx][i] = v / this.tick_count;
                }
            }
        }

    }

    public int get_op_moves(int op_idx)
    {
        int action;
        if (this.count > 20) {
            action = get_action_idx(this.multinomial_dist.multinomialSample(1, prob[op_idx]));
            if (action == -1) {
                throw new java.lang.Error("action -1");
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
