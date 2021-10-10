package players.rhea_mm;

import players.rhea_mm.evo.Evolution_opponent;
import players.rhea_mm.utils.RHEAParams_mm;
import utils.ElapsedCpuTimer;

import java.util.Arrays;
import java.util.Random;

class RollingHorizonPlayer {
    private Random randomGenerator;
    private RHEAParams_mm params;
    private GameInterface gameInterface;

    private int[] actionBuffer;
    private boolean newBuffer;

    private Evolution_opponent ea;


    RollingHorizonPlayer(Random randomGenerator, RHEAParams_mm params, GameInterface gInterface) {
        this.gameInterface = gInterface;
        this.randomGenerator = randomGenerator;
        this.params = params;
        this.actionBuffer = new int[params.frame_skip];  // This buffer will hold list of actions to be executed
        Arrays.fill(actionBuffer, -1);
        newBuffer = true;
        this.ea = new Evolution_opponent(params, randomGenerator, gameInterface);
    }

    int getAction(ElapsedCpuTimer elapsedTimer, int max_actions) {
        // Init if we ran out of actions in the buffer
        if (newBuffer) {
            ea.init(max_actions);
        }

//        System.out.println();
        // Find best next action within the allowed budget
        int action = max_actions;
        int iter = 0;
        while (gameInterface.budget(elapsedTimer, params.iteration_budget - ea.getNIterations(),
                null)) {
            System.out.println(ea.getNIterations());
            action = ea.iteration(max_actions);
            gameInterface.endIteration(elapsedTimer, null);
            iter++;
        }
        System.out.println("#Iterations "+iter);
        System.out.println(action);
        System.out.println(gameInterface.translate(action));

        return action;
    }

    private int getBestAction(int idx) {
        return ea.getBestAction(idx);
    }
}
