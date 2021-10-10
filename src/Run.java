import core.Game;
import players.*;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rhea.RHEAPlayer;
import players.rhea.utils.Constants;
import players.rhea.utils.RHEAParams;
import players.rhea_minimax.RHEAPlayer_minimax;
import players.rhea_minimax.utils.RHEAParams_minimax;
import utils.*;

import java.util.*;

import static utils.Types.VISUALS;

public class Run {

    private static void printHelp()
    {
        System.out.println("Usage: java Run [args]");
        System.out.println("\t [arg index = 0] Game Mode. 0: FFA; 1: TEAM");
        System.out.println("\t [arg index = 1] Number of level generation seeds. \"-1\" to execute with the ones from paper (20).");
        System.out.println("\t [arg index = 2] Repetitions per seed [N]. \"1\" for one game only with visuals.");
        System.out.println("\t [arg index = 3] Vision Range [VR]. (0, 1, 2 for PO; -1 for Full Observability)");
        System.out.println("\t [arg index = 4-7] Agents. When in TEAM, agents are mates as indices 4-6, 5-7:");
        System.out.println("\t\t 0 DoNothing");
        System.out.println("\t\t 1 Random");
        System.out.println("\t\t 2 OSLA");
        System.out.println("\t\t 3 SimplePlayer");
        System.out.println("\t\t 4 RHEA 200 itereations, shift buffer, pop size 1, random init, length: 12");
        System.out.println("\t\t 5 MCTS 200 iterations, length: 12");
    }

    public static void main(String[] args) {

        //default
        if(args.length == 0)
            args = new String[]{"0", "1", "1", "-1", "2", "3", "4", "5"};

        if(args.length != 8) {
            printHelp();
            return;
        }

        try {

            Random rnd = new Random();

            // Create players
            ArrayList<Player> players = new ArrayList<>();
            int playerID = Types.TILETYPE.AGENT0.getKey();
            int boardSize = Types.BOARD_SIZE;

            Types.GAME_MODE gMode = Types.GAME_MODE.FFA;
            if(Integer.parseInt(args[0]) == 1)
                gMode = Types.GAME_MODE.TEAM;

            int S = Integer.parseInt(args[1]);
            int N = Integer.parseInt(args[2]);
            Types.DEFAULT_VISION_RANGE = Integer.parseInt(args[3]);

            long seeds[];

            if (S == -1)
            {
                //Special case, these seeds are fixed for the experiments in the paper:
                seeds = new long[] {93988, 19067, 64416, 83884, 55636, 27599, 44350, 87872, 40815,
                        11772, 58367, 17546, 75375, 75772, 58237, 30464, 27180, 23643, 67054, 19508};
            }else
            {
                if(S <= 0)
                    S = 1;

                //Otherwise, all seeds are random
                seeds = new long[S];
                for(int i = 0; i < S; i++)
                    seeds[i] = rnd.nextInt(100000);
            }

            long seed = 0;

            String[] playerStr = new String[4];

            for(int i = 4; i <= 7; ++i) {
                int agentType = Integer.parseInt(args[i]);
                Player p = null;


                switch(agentType) {
                    case -1:
                        continue;
                    case 0:
                        p = new DoNothingPlayer(playerID++);
                        playerStr[i-4] = "DoNothing";
                        break;
                    case 1:
                        p = new RandomPlayer(seed, playerID++);
                        playerStr[i-4] = "Random";
                        break;
                    case 2:
                        p = new OSLAPlayer(seed, playerID++);
                        playerStr[i-4] = "OSLA";
                        break;
                    case 3:
                        p = new SimplePlayer(seed, playerID++);
                        playerStr[i-4] = "RuleBased";
                        break;
                    case 4:
                        RHEAParams rheaParams = new RHEAParams();
                        rheaParams.op_model = Constants.OP_RANDOM;
                        rheaParams.budget_type = Constants.FM_BUDGET;
                        rheaParams.fm_budget = 2000;
                        rheaParams.population_size = 1;
                        rheaParams.individual_length = 12;
                        rheaParams.offspring_count = 1;
                        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        rheaParams.elitism = true;
                        p = new RHEAPlayer(seed, playerID++, rheaParams);
                        playerStr[i-4] = "RHEA RANDOM";
                        break;
                    case 5:
                        MCTSParams mctsParams = new MCTSParams();
                        mctsParams.op_model = Constants.OP_RANDOM;
                        mctsParams.stop_type = mctsParams.STOP_FMCALLS;
                        mctsParams.num_fmcalls = 2000;
                        mctsParams.rollout_depth = 12;
                        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;
                        p = new MCTSPlayer(seed, playerID++, mctsParams);
                        playerStr[i-4] = "MCTS RANDOM";
                        break;
                    case 6:
                        RHEAParams rheaParams_lb = new RHEAParams();
                        rheaParams_lb.op_model = Constants.OP_LIMITED_BUFFER;
                        rheaParams_lb.budget_type = Constants.FM_BUDGET;
                        rheaParams_lb.fm_budget = 2000;
                        rheaParams_lb.population_size = 1;
                        rheaParams_lb.individual_length = 12;
                        rheaParams_lb.offspring_count = 1;
                        rheaParams_lb.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        rheaParams_lb.elitism = true;
                        p = new RHEAPlayer(seed, playerID++, rheaParams_lb);
                        playerStr[i-4] = "RHEA LB";
                        break;
                    case 7:
                        RHEAParams rheaParams_ub = new RHEAParams();
                        rheaParams_ub.op_model = Constants.OP_UNLIMITED_BUFFER;
                        rheaParams_ub.budget_type = Constants.FM_BUDGET;
                        rheaParams_ub.fm_budget = 2000;
                        rheaParams_ub.population_size = 1;
                        rheaParams_ub.individual_length = 12;
                        rheaParams_ub.offspring_count = 1;
                        rheaParams_ub.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        rheaParams_ub.elitism = true;
                        p = new RHEAPlayer(seed, playerID++, rheaParams_ub);
                        playerStr[i-4] = "RHEA UB";
                        break;
                    case 8:
                        RHEAParams_minimax rheaParams_mm = new RHEAParams_minimax();
                        rheaParams_mm.elitism = true;
                        rheaParams_mm.budget_type = Constants.FM_BUDGET;
                        rheaParams_mm.fm_budget = 2000;
                        rheaParams_mm.population_size = 1;
                        rheaParams_mm.individual_length = 12;
                        rheaParams_mm.offspring_count = 1;
                        rheaParams_mm.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        p = new RHEAPlayer_minimax(seed, playerID++, rheaParams_mm);
                        playerStr[i-4] = "RHEA MINIMAX";
                        break;
                    case 9:
                        MCTSParams mctsParams_lb = new MCTSParams();
                        mctsParams_lb.op_model = mctsParams_lb.OP_LIMITED_BUFFER;
                        mctsParams_lb.stop_type = mctsParams_lb.STOP_FMCALLS;
                        mctsParams_lb.num_fmcalls = 2000;
                        mctsParams_lb.rollout_depth = 12;
                        mctsParams_lb.heuristic_method = mctsParams_lb.CUSTOM_HEURISTIC;
                        p = new MCTSPlayer(seed, playerID++, mctsParams_lb);
                        playerStr[i-4] = "MCTS LB";
                        break;
                    case 10:
                        MCTSParams mctsParams_ub = new MCTSParams();
                        mctsParams_ub.op_model = mctsParams_ub.OP_UNLIMITED_BUFFER;
                        mctsParams_ub.stop_type = mctsParams_ub.STOP_FMCALLS;
                        mctsParams_ub.num_fmcalls = 2000;
                        mctsParams_ub.rollout_depth = 12;
                        mctsParams_ub.heuristic_method = mctsParams_ub.CUSTOM_HEURISTIC;
                        p = new MCTSPlayer(seed, playerID++, mctsParams_ub);
                        playerStr[i-4] = "MCTS UB";
                        break;
                    case 11:
                        RHEAParams_minimax rheaParams_mm5 = new RHEAParams_minimax();
                        rheaParams_mm5.elitism = true;
                        rheaParams_mm5.budget_type = Constants.FM_BUDGET;
                        rheaParams_mm5.fm_budget = 2000;
                        rheaParams_mm5.population_size = 5;
                        rheaParams_mm5.individual_length = 12;
                        rheaParams_mm5.offspring_count = 1;
                        rheaParams_mm5.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        p = new RHEAPlayer_minimax(seed, playerID++, rheaParams_mm5);
                        playerStr[i-4] = "RHEA MINIMAX";
                        break;
                    case 12:
                        RHEAParams rheaParams_know = new RHEAParams();
                        rheaParams_know.op_model = Constants.OP_KNOW;
                        rheaParams_know.budget_type = Constants.FM_BUDGET;
                        rheaParams_know.fm_budget = 2000;
                        rheaParams_know.population_size = 1;
                        rheaParams_know.individual_length = 12;
                        rheaParams_know.offspring_count = 1;
                        rheaParams_know.heurisic_type = Constants.CUSTOM_HEURISTIC;
                        rheaParams_know.elitism = true;
                        p = new RHEAPlayer(seed, playerID++, rheaParams_know);
                        playerStr[i-4] = "RHEA KNOW";
                        break;
                    default:
                        System.out.println("WARNING: Invalid agent ID: " + agentType );
                }

                players.add(p);
            }

            String gameIdStr = "";
            for(int i = 0; i <= 7; ++i) {
                gameIdStr += args[i];
                if(i != 7)
                    gameIdStr+="-";
            }

            Game game = new Game(seeds[0], boardSize, gMode, gameIdStr);

            // Make sure we have exactly NUM_PLAYERS players
            assert players.size() == Types.NUM_PLAYERS;
            game.players_names = playerStr;
            game.setPlayers(players);

            System.out.print(gameIdStr + " [");
            for(int i = 0; i < playerStr.length; ++i) {
                System.out.print(playerStr[i]);
                if(i != playerStr.length-1)
                    System.out.print(',');

            }
            System.out.println("]");

            runGames(game, seeds, N, false);
        } catch(Exception e) {
            e.printStackTrace();
            printHelp();
        }
    }

    /**
     * Runs 1 game.
     * @param g - game to run
     * @param ki1 - primary key controller
     * @param ki2 - secondary key controller
     * @param separateThreads - if separate threads should be used for the agents or not.
     */
    public static void runGame(Game g, KeyController ki1, KeyController ki2, boolean separateThreads) {
        WindowInput wi = null;
        GUI frame = null;
        if (VISUALS) {
            frame = new GUI(g, "Java-Pommerman", ki1, false, true);
            wi = new WindowInput();
            wi.windowClosed = false;
            frame.addWindowListener(wi);
            frame.addKeyListener(ki1);
            frame.addKeyListener(ki2);
        }

        g.run(frame, wi, separateThreads);
    }

    public static void runGames(Game g, long seeds[], int repetitions, boolean useSeparateThreads){
        int numPlayers = g.getPlayers().size();
        int[] winCount = new int[numPlayers];
        int[] tieCount = new int[numPlayers];
        int[] lossCount = new int[numPlayers];

        int[] overtimeCount = new int[numPlayers];

        int numSeeds = seeds.length;
        int totalNgames = numSeeds * repetitions;

        for(int s = 0; s<numSeeds; s++) {
            long seed = seeds[s];

            for (int i = 0; i < repetitions; i++) {
                long playerSeed = System.currentTimeMillis();

                System.out.print( playerSeed + ", " + seed + ", " + (s*repetitions + i) + "/" + totalNgames + ", ");

                g.reset(seed);
                EventsStatistics.REP = i;
                GameLog.REP = i;

                // Set random seed for players and reset them
                ArrayList<Player> players = g.getPlayers();
                for (int p = 0; p < g.nPlayers(); p++) {
                    players.get(p).reset(playerSeed, p + Types.TILETYPE.AGENT0.getKey());
                }
                Types.RESULT[] results = g.run(useSeparateThreads);

                for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
                    switch (results[pIdx]) {
                        case WIN:
                            winCount[pIdx]++;
                            break;
                        case TIE:
                            tieCount[pIdx]++;
                            break;
                        case LOSS:
                            lossCount[pIdx]++;
                            break;
                    }
                }

                int[] overtimes = g.getPlayerOvertimes();
                for(int j = 0; j < overtimes.length; ++j)
                    overtimeCount[j] += overtimes[j];

                for (int p=0; p < Types.NUM_PLAYERS; p++)
                    players.get(p).print_accuracy();

            }
        }

        //Done, show stats
        System.out.println("N \tWin \tTie \tLoss \tPlayer (overtime average)");
        for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
            String player = g.getPlayers().get(pIdx).getClass().toString().replaceFirst("class ", "");

            double winPerc = winCount[pIdx] * 100.0 / (double)totalNgames;
            double tiePerc = tieCount[pIdx] * 100.0 / (double)totalNgames;
            double lossPerc = lossCount[pIdx] * 100.0 / (double)totalNgames;
            double overtimesAvg = overtimeCount[pIdx] / (double)totalNgames;

            System.out.println(totalNgames + "\t" + winPerc + "%\t" + tiePerc + "%\t" + lossPerc + "%\t" + player + " (" + overtimesAvg + ")" );
        }
    }
}
