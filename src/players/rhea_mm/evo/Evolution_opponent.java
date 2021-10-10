package players.rhea_mm.evo;

import players.rhea_mm.GameInterface;
import players.rhea_mm.utils.RHEAParams_mm;

import java.util.Random;
import java.util.*;

class ptr_minimax{
    public int idx;
    public double value;

    public ptr_minimax (int ix, double val){
        this.idx = ix;
        this.value = val;
    }
}

public class Evolution_opponent{

    public Individual[][] action_buffer; // [player][individual][action(t)]
    private double[][] state_values; // [player_1 individual][other_player individual]
    private int[] to_keep;
    private int[][] to_replace;

    private Random random;
    private RHEAParams_mm params;
    private GameInterface gInterface;

    private Mutation mutationClass;
    private Crossover crossoverClass;
    private Selection selectionClass;

    private int opp_pop_size = 2;
    private double[] minimax = {+100, -100};
    private int[] minimax_idx = {-1, -1};
    private int max_idx = -1;

    public ptr_minimax[] mini;
    //        ptr_minimax max = new ptr_minimax[params.population_size];
    public ptr_minimax max;
    private int nIterations;

    public Evolution_opponent(RHEAParams_mm params, Random random, GameInterface gInterface) {
        this.params = params;
        this.random = random;
        action_buffer = new Individual[4][params.population_size];
        this.state_values = new double[params.population_size][params.population_size];
        this.to_keep = new int[4];
        this.to_replace = new int[4][params.population_size-1];
        mini = new ptr_minimax[params.population_size];
        max = new ptr_minimax(-1, -10);
        for (int i = 0; i < params.population_size; i++){
            mini[i] = new ptr_minimax(-1, 10);
        }

        mutationClass = new Mutation(params, random);
        crossoverClass = new Crossover(params, random);
        selectionClass = new Selection(params, random);
        nIterations = 0;

        this.gInterface = gInterface;
    }

    public void get_keeps_n_replaces()
    {
        double max_value;
        double min_value;
        for (int m=0; m<4; m++) {
            max_value = -100;
            min_value = +100;
            int k = 0;
            for (int n=0; n< params.population_size; n++) {
                if (m==0) {
                    if (action_buffer[m][n].get_value() > max_value) {
                        max_value = action_buffer[m][n].get_value();
                        to_keep[m] = n;
                    } else{
                        to_replace[m][k] = n;
                        k++;
                    }
                }else{
                    if (action_buffer[m][n].get_value() < min_value) {
                        min_value = action_buffer[m][n].get_value();
                        to_keep[m] = n;
                    }else{
                        to_replace[m][k] = n;
                        k++;
                    }
                }
            }
        }
    }

    public void init(int max_actions) {
        nIterations = 0;
        if (action_buffer[0][0] != null) {
            shift_action_buffer(max_actions);
        } else {
            for (int m = 0; m < 4; m++) {
                for (int n = 0; n < params.population_size; n++) {
                    action_buffer[m][n] = new Individual(params.individual_length, random, max_actions);
                    action_buffer[m][n].randomize();
                }
            }
            for (int m = 0; m < 4; m++) {
                for (int n = 0; n < params.population_size; n++) {
                    action_buffer[m][n].set_value(gInterface.evaluate_a_state(action_buffer, null, n, -1));
                }
//            if (m == 0) Arrays.sort(action_buffer[m], Comparator.reverseOrder());
//            else Arrays.sort(action_buffer[m], Comparator.naturalOrder());
            }
        }
        get_keeps_n_replaces();
    }


    public void shift_action_buffer(int max_actions) {
        for (int m = 0; m < 4; m++) { // loop through players
            for (int n = 0; n < params.population_size; n++) { // loop thorugh individuals
                for (int i = 1; i < params.individual_length; i++) {
                    action_buffer[m][n].set_action(i - 1, action_buffer[m][n].get_action(i));
                }
                action_buffer[m][n].set_action(params.individual_length - 1, random.nextInt(max_actions));// = random.nextInt(max_actions);
            }
        }
    }

    public int iteration(int max_actions) {
        nIterations++;


        double[][] opponent_values = new double[params.population_size][params.population_size];
        double[] values = new double[params.population_size];

        for (int m = 0; m < params.population_size; m++) {
            for (int n = 0; n < params.population_size; n++) {
                opponent_values[m][n] = gInterface.evaluate_state_opp(action_buffer, null, m, n);
                if (mini[m].idx == -1) {
                    mini[m].idx = n;
                    mini[m].value = opponent_values[m][n];
                } else {
                    if (opponent_values[m][n] < mini[m].value) {
                        mini[m].idx = n;
                        mini[m].value = opponent_values[m][n];
                    }
                }
            }
            values[m] = gInterface.evaluate_state_opp(action_buffer, null, m, mini[m].idx);
            action_buffer[0][m].set_value(values[m]);
            if (max.idx == -1) {
                max.idx = m;
                max.value = values[m];
            }else {
                if (max.value < values[m]) {
                    max.idx = m;
                    max.value = values[m];
                }
            }
        }

        for (int k = 1; k < 4; k++) {
            for (int n = 0; n < params.population_size; n++) {
                action_buffer[k][n].set_value(opponent_values[max.idx][n]);
            }
        }
        get_keeps_n_replaces();

        Individual[][] offsprings = new Individual[4][params.offspring_count];

//        int startIdx = params.no_elites;

        for (int m = 0; m < 4; m++) {
            offsprings = generate_offsprings_opp(m);
            Individual[] combined = new Individual[params.population_size - params.no_elites + offsprings[m].length];
            int idx = 0;
            for (int n : to_replace[m]) {
                combined[idx] = action_buffer[m][n].copy();
                idx++;
            }
            for (int i=0; i<params.offspring_count; i++) {
                combined[idx] = offsprings[m][i].copy();
                idx++;
            }

            if (m == 0) Arrays.sort(combined, Comparator.reverseOrder());
            else Arrays.sort(combined, Comparator.naturalOrder());

            int k = 0;
            for (int n : to_replace[m]) {
                action_buffer[m][n] = combined[k].copy();
                k++;
            }
        }

        for (int m = 0; m < 4; m++) {
            for (int n = 0; n < params.population_size; n++) {
                opponent_values[m][n] = gInterface.evaluate_state_opp(action_buffer, null, m, n);
                if (mini[m].idx == -1) {
                    mini[m].idx = n;
                    mini[m].value = opponent_values[m][n];
                } else {
                    if (opponent_values[m][n] < mini[m].value) {
                        mini[m].idx = n;
                        mini[m].value = opponent_values[m][n];
                    }
                }
            }
            values[m] = gInterface.evaluate_state_opp(action_buffer, null, m, mini[m].idx);
            action_buffer[0][m].set_value(values[m]);
            if (max.idx == -1) {
                max.idx = m;
                max.value = values[m];
            }
        }

        for (int k = 1; k < 4; k++) {
            for (int n = 0; n < params.population_size; n++) {
                action_buffer[k][n].set_value(opponent_values[max.idx][n]);
            }
        }
////                offspring[m] = Utilities.add_array_to_array(action_buffer[m], offspring[m], startIdx);
////            if (m == 0) Arrays.sort(offspring[m], Comparator.reverseOrder());
////            else Arrays.sort(offspring[m], Comparator.naturalOrder());
//
//
////        for (int m=0; m<4; m++) {
////            for(int n=0; n<params.population_size; n++) {
////                action_buffer[m][n].set_value(gInterface.evaluate_a_state(action_buffer, mutationClass, n, -1));
////            }
////        }
//
////        for (int m=0; m<4; m++) {
////            if (m == 0) Arrays.sort(action_buffer[m], Comparator.reverseOrder());
////            else Arrays.sort(action_buffer[m], Comparator.naturalOrder());
////            int nextId = 0;
////            for (int i = startIdx; i < params.population_size; i++) {
////                action_buffer[m][i] = offspring[m][nextId].copy();
////                nextId++;
////            }
////        }
//
////        for (int m=0; m<4; m++) {
////            for (int n = 0; n < startIdx; n++) {
////                action_buffer[m][n].set_value(gInterface.evaluate_a_state(action_buffer, null, n, -1));
////            }
//////                if (m == 0) Arrays.sort(action_buffer[m], Comparator.reverseOrder());
//////                else Arrays.sort(action_buffer[m], Comparator.naturalOrder());
////        }
//
//        get_state_values();
//
//        for (int m = 0; m < 4; m++) {
//            for (int n = 0; n < params.population_size; n++) {
//                action_buffer[m][n].set_value(gInterface.evaluate_a_state(action_buffer, null, n, -1));
//            }
//        }
//
//        get_minimax_idx();

        Arrays.sort(action_buffer[0], Comparator.reverseOrder());

        return getBestAction(0);
    }

    private Individual[][] generate_offsprings_opp(int agent_idx) {
        Individual[][] minimax_with_offsprings = new Individual[4][params.offspring_count];
        for (int k=0; k < params.offspring_count; k++) {
            if (agent_idx == 0) {
                minimax_with_offsprings[0][k] = crossover(action_buffer[0]);
                for (int m=1; m < 4; m++) {
                    minimax_with_offsprings[m][k] = action_buffer[m][mini[m].idx];
                }
//                mutationClass.findGenesToMutate();
                minimax_with_offsprings[0][k].set_value(gInterface.evaluate_offspring(minimax_with_offsprings, mutationClass, k, 0));

            } else {
                minimax_with_offsprings[0][k] = action_buffer[0][max.idx];
                for (int m=1; m < 4; m++) {
                    minimax_with_offsprings[m][k] = crossover(action_buffer[m]);
                }
                for (int m=1; m<4; m++) {
//                    mutationClass.findGenesToMutate();
                    minimax_with_offsprings[m][k].set_value(gInterface.evaluate_offspring(minimax_with_offsprings, mutationClass, k, m));
                }
            }
        }
        return minimax_with_offsprings;
    }
    private Individual[] generate_offspring(int agent_idx) {
        Individual[] offspring = new Individual[params.offspring_count];
        Individual[][] copy_aciton_buffer = copy(action_buffer);
        int offspring_count = 0;
        for (int k=0; k< params.offspring_count; k++) {

            mutationClass.findGenesToMutate();
            offspring[k] = crossover(action_buffer[agent_idx]);
            double value;
            double new_value;
            for (int n=0; n< params.population_size; n++) {
                copy_aciton_buffer[agent_idx][n] = offspring[k].copy();
                new_value = gInterface.evaluate_a_state(copy_aciton_buffer, mutationClass, n, agent_idx);
                if (agent_idx == 0) {
                    value = -100;
                    if (new_value < value) {
                        value = new_value;
                        offspring[k].set_value(value);
                    }
                }else{
                    value = +100;
                    if(new_value < value) {
                        value = new_value;
                        offspring[k].set_value(value);
                    }
                }
            }
//        for (int n = 0; n < params.population_size; n++) {
//            if (n != to_keep[agent_idx]) {
//                mutationClass.findGenesToMutate();
//                offspring[offspring_count] = crossover(action_buffer[agent_idx]);
//                offspring[offspring_count].set_value(gInterface.evaluate_a_state(copy_aciton_buffer, mutationClass, n, agent_idx));
//                copy_aciton_buffer[agent_idx][n] = offspring[offspring_count];
//                if (action_buffer[agent_idx][n].get_value() < copy_aciton_buffer[agent_idx][n].get_value()) {
//                    if (agent_idx == 0) {
////                        action_buffer[agent_idx][n] = offspring[offspring_count].copy();
//                        to_replace[agent_idx][k] = n;
//                        k++;
//                    }else{
//                        offspring[offspring_count] = null;
//                    }
//                }else{
//                    if (agent_idx != 0) {
////                        action_buffer[agent_idx][n] = offspring[offspring_count].copy();
//                        to_replace[agent_idx][k] = n;
//                        k++;
//                    }else{
//                        offspring[offspring_count] = null;
//                    }
//                }
                offspring_count++;
            }
//        }
        return offspring;
    }

    private Individual[][] copy(Individual[][] action_buffer) {
        Individual[][] copy_action_buffer = new Individual[4][params.population_size];
        for(int m=0; m<4; m++) {
            for (int n=0; n<params.population_size; n++) {
                copy_action_buffer[m][n] = new Individual(params.individual_length, random, action_buffer[m][n].get_max_actions());
                for (int i=0; i<params.individual_length; i++) {
//                    copy_action_buffer[m][n].set_action(i, action_buffer[m][n].get_action(i));
                    copy_action_buffer[m][n] = action_buffer[m][n].copy();
                }
            }
        }
        return copy_action_buffer;
    }

    private Individual crossover(Individual[] population){
        Individual parent1 = select(population);
        Individual parent2 = select(population, parent1);

        return crossoverClass.cross(parent1, parent2);
    }

    private Individual select(Individual[] population) {
        return selectionClass.select(population);
    }

    private Individual select(Individual[] population, Individual ignore) {
        Individual[] reduced_pop = new Individual[population.length - 1];
        int idx = 0;
        for (Individual individual : population) {
            if (!individual.equals(ignore)) {
                reduced_pop[idx] = individual;
                idx++;
            }
        }

        return select(reduced_pop);
    }

    public void get_state_values() {
        this.state_values = gInterface.evaluate_states(action_buffer);
    }

    public void get_minimax_idx() {
        minimax[1] = -100.0;
        for (int m=0; m<params.population_size; m++) {
            int mini_idx = -1;
            minimax[0] = 100.0;
            for (int n=0; n<opp_pop_size; n++) {
//                if (state_values[m][n] < minimax[0]) {
                if (action_buffer[0][n].get_value() < minimax[0]) {
//                    minimax[0] = state_values[m][n];
                    minimax[0] = action_buffer[0][n].get_value();
                    mini_idx = n;
                }
//                if (state_values[m][n] == minimax[0] && random.nextDouble() < 0.5) {
                if (action_buffer[0][n].get_value() == minimax[0] && random.nextDouble() < 0.5) {
//                    minimax[0] = state_values[m][n];
                    minimax[0] = action_buffer[0][n].get_value();
                    mini_idx = n;
                }
            }
//            if (state_values[m][mini_idx] > minimax[1]) {
            for (int n=0;n< params.population_size; n++) {
                if (action_buffer[0][n].get_value() > minimax[1]) {
//                minimax[1] = state_values[m][mini_idx];
                    minimax[1] = action_buffer[0][n].get_value();
                    max_idx = m;
                }
//            if (state_values[m][mini_idx] == minimax[1] && random.nextDouble() < 0.5) {
                if (action_buffer[0][n].get_value() == minimax[1] && random.nextDouble() < 0.5) {
//                minimax[1] = state_values[m][mini_idx];
                    minimax[1] = action_buffer[0][n].get_value();
                    max_idx = m;
                }
            }
        }
    }

    public int getBestAction(int idx) {
        int act = action_buffer[0][idx].get_action(0);
        return act;
    }

    public int getNIterations() { return nIterations; }

//    public int[][] gen_offspring(){
//        int[][] offspring;
//        for (int i = 0; i< params.offspring_count; i++) {
//            mutate;
//            crossover;
//
//        }
//
//    }
//    private Individual[] generate_offspring() {
//        Individual[] offspring = new Individual[params.offspring_count];
//        for (int i = 0; i < params.offspring_count; i++) {
//                offspring[i] = crossover(population);
//                mutationClass.findGenesToMutate();
//                gInterface.evaluate(offspring[i], mutationClass, params.evaluate_update);
//        }
//        return offspring;
//    }
//
//    private Individual select(Individual[] population) {
//        return selectionClass.select(population);
//    }
//
//    private Individual select(Individual[] population, Individual ignore) {
//        Individual[] reduced_pop = new Individual[population.length - 1];
//        int idx = 0;
//        for (Individual individual : population) {
//            if (!individual.equals(ignore)) {
//                reduced_pop[idx] = individual;
//                idx++;
//            }
//        }
//
//        return select(reduced_pop);
//    }
//
//    private Individual crossover(Individual[] population){
//        Individual parent1 = select(population);
//        Individual parent2 = select(population, parent1);
//
//        return crossoverClass.cross(parent1, parent2);
//    }

}

//public class Evolution_opponent {
//    private RHEAParams_mm params;
//    private Random random;
//
//    private Mutation mutationClass;
//    private Crossover crossoverClass;
//    private Selection selectionClass;
//
//    private int nIterations;
//    private Individual[] population;
//
//    private GameInterface gInterface;
//
//
//    public Evolution_opponent(RHEAParams_mm params, Random random, GameInterface gInterface) {
//        this.params = params;
//        this.random = random;
//        mutationClass = new Mutation(params, random);
//        crossoverClass = new Crossover(params, random);
//        selectionClass = new Selection(params, random);
//        nIterations = 0;
//
//        this.gInterface = gInterface;
//    }
//
//    public void init(int max_actions) {
//        nIterations = 0;
//        if (params.shift_buffer && population != null) {
//            shift_population(max_actions);
//        } else {
//            init_population(max_actions);
//            if (params.init_type != INIT_RANDOM) {
//                seed();
//            }
//        }
//    }
//
//    /**
//     * Performs 1 iteration of EA.
//     * @return - best action after 1 iteration.
//     */
//    public int iteration() {
////        System.out.println(Arrays.toString(population));
//        nIterations++;
//
//        // Generate offspring
//        Individual[] offspring = generate_offspring();
//
//        // Update population
//        combine_and_sort_population(offspring);
//
//        return getBestAction(0);
//    }
//
//    public int getBestAction(int idx) {
//        return population[0].get_action(idx);
//    }
//
//    public int getNIterations() { return nIterations; }
//
//    //------ private
//
//    private void seed() {
//        for (int i = 0; i < params.population_size; i++) {
//            if (i > 0) {
//                population[i] = population[0].copy();
//                mutationClass.findGenesToMutate();
//                gInterface.evaluate(population[i], mutationClass, params.evaluate_update);
//            } else {
//                gInterface.seed(population[i], params.init_type);
//                gInterface.evaluate(population[i], null, params.evaluate_update);
//            }
//        }
//    }
//
//    private void init_population(int max_actions) {
//        population = new Individual[params.population_size];
//        for (int i = 0; i < params.population_size; i++) {
//            population[i] = new Individual(params.individual_length, random, max_actions);
//            if (params.init_type == INIT_RANDOM) {
//                population[i].randomize();
//                gInterface.evaluate(population[i], null, params.evaluate_update);
//            }
//        }
//    }
//
//    private Individual select(Individual[] population) {
//        return selectionClass.select(population);
//    }
//
//    private Individual select(Individual[] population, Individual ignore) {
//        Individual[] reduced_pop = new Individual[population.length - 1];
//        int idx = 0;
//        for (Individual individual : population) {
//            if (!individual.equals(ignore)) {
//                reduced_pop[idx] = individual;
//                idx++;
//            }
//        }
//
//        return select(reduced_pop);
//    }
//
//    private Individual crossover(Individual[] population){
//        Individual parent1 = select(population);
//        Individual parent2 = select(population, parent1);
//
//        return crossoverClass.cross(parent1, parent2);
//    }
//
//    private Individual[] generate_offspring() {
//        Individual[] offspring = new Individual[params.offspring_count];
//        for (int i = 0; i < params.offspring_count; i++) {
//            if (params.genetic_operator == MUTATION_ONLY || params.population_size <= 2) {
//                offspring[i] = population[random.nextInt(population.length)].copy();
//            } else {
//                offspring[i] = crossover(population);
//            }
//            if (params.genetic_operator != CROSSOVER_ONLY) {
//                mutationClass.findGenesToMutate();
//                gInterface.evaluate(offspring[i], mutationClass, params.evaluate_update);
//            } else {
//                gInterface.evaluate(offspring[i], null, params.evaluate_update);
//            }
//        }
//        return offspring;
//    }
//
//    /**
//     * Assumes population and offspring are already sorted in descending order by individual fitness
//     * @param offspring - offspring created from parents population
//     */
//    @SuppressWarnings("unchecked")
//    private void combine_and_sort_population(Individual[] offspring){
//        int startIdx = 0;
//
//        // Make sure we have enough individuals to choose from for the next population
//        if (params.offspring_count < params.population_size) params.keep_parents_next_gen = true;
//
//        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
//            // First no_elites individuals remain the same, the rest are replaced
//            startIdx = params.no_elites;
//        }
//
//        if (params.keep_parents_next_gen) {
//            // Reevaluate current population
//            if (params.reevaluate_pop) {
//                for (Individual i : population) {
//                    gInterface.evaluate(i, null, params.evaluate_update);
//                }
//            }
//            // If we should keep best individuals of parents + offspring, then combine array
//            offspring = Utilities.add_array_to_array(population, offspring, startIdx);
//            Arrays.sort(offspring, Comparator.reverseOrder());
//        }
//
//        // Combine population with offspring, we keep only best individuals. If parents should not be kept, new
//        // population is only best POP_SIZE offspring individuals.
//        int nextIdx = 0;
//        for (int i = startIdx; i < params.population_size; i++) {
//            population[i] = offspring[nextIdx].copy();
//            nextIdx ++;
//        }
//
//        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
//            // If parents were kept to new generation and we had elites, population needs sorting again
//            Arrays.sort(population, Comparator.reverseOrder());
//        }
//    }
//
//    private void shift_population(int max_actions) {
//        // Remove first action of all individuals and add a new random one at the end
//        for (int i = 0; i < params.population_size; i++) {
//            for (int j = 1; j < params.individual_length; j++) {
//                population[i].set_action(j - 1, population[i].get_action(j));
//            }
//            population[i].set_action(params.individual_length - 1, random.nextInt(max_actions));
//            gInterface.evaluate(population[i], null, EVALUATE_UPDATE_AVERAGE);
////            population[i].discount_value(params.shift_discount);
//        }
//    }
//}
