package players.rhea_minimax.evo;

import players.rhea_minimax.GameInterface;
import players.rhea_minimax.utils.RHEAParams_minimax;
import players.rhea_minimax.utils.Utilities;
import utils.Types;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static players.rhea_minimax.utils.Constants.*;

public class Evolution {
    private RHEAParams_minimax params;
    private Random random;

    private Mutation mutationClass;
    private Crossover crossoverClass;
    private Selection selectionClass;

    private int nIterations;
    public Individual[] population;
    private Individual[] population_opp;
    private int playerID;

    private GameInterface gInterface;
    public int[][] predicted_actions;

    public Evolution(RHEAParams_minimax params, Random random, GameInterface gInterface, int playerID) {
        this.params = params;
        this.random = random;
        mutationClass = new Mutation(params, random);
        crossoverClass = new Crossover(params, random);
        selectionClass = new Selection(params, random);
        nIterations = 0;

        this.gInterface = gInterface;
        this.predicted_actions = new int[Types.NUM_PLAYERS][Types.MAX_GAME_TICKS];
        this.playerID = playerID - 10; //MM we need this to fill predicted actions array
    }

    public void init(int max_actions) {
        nIterations = 0;
        if (params.shift_buffer && population != null) {
            shift_population(max_actions);
        } else {
            init_population(max_actions);
            if (params.init_type != INIT_RANDOM) {
                seed();
            }
        }
    }

    /**
     * Performs 1 iteration of EA.
     * @return - best action after 1 iteration.
     */
    public int iteration(int max_actions) {
//        System.out.println(Arrays.toString(population));
        nIterations++;

        //MM Sequentially in each iteration we first optimize opponents actions
        Individual[] offspring_opponents = generate_opponent_offspring(max_actions);
        //MM sorting opponents population
        combine_and_sort_population_opp(offspring_opponents);

        // Generate offspring
        //MM optimize central agent's actions based on the actions found at the previous step
        Individual[] offspring = generate_offspring_with_opponent();

        // Update population
        //MM sorting central agent population
        combine_and_sort_population(offspring);
        return getBestAction(0);
    }

    public int getBestAction(int idx) {
        //MM filling predicted actions array with actions of opponents
        int current_tick = this.gInterface.get_current_tick();
        int pidx = 0;
        for (int p=0; p<Types.NUM_PLAYERS; p++) {
            if (p != this.playerID) {
                this.predicted_actions[p][current_tick] = population_opp[0].get_action(pidx * params.individual_length);
                pidx++;
            }
        }
        return population[0].get_action(idx);
    }

    public int getNIterations() { return nIterations; }

    //------ private

    private void seed() {
        for (int i = 0; i < params.population_size; i++) {
            if (i > 0) {
                population[i] = population[0].copy();
                mutationClass.findGenesToMutate();
                gInterface.evaluate(population[i], mutationClass, params.evaluate_update);
            } else {
                gInterface.seed(population[i], params.init_type);
                gInterface.evaluate(population[i], null, params.evaluate_update);
            }
        }
    }

    private void init_population(int max_actions) {
        population = new Individual[params.population_size];
        population_opp = new Individual[params.population_size];
        for (int i = 0; i < params.population_size; i++) {
            population[i] = new Individual(params.individual_length, random, max_actions);
            population_opp[i] = new Individual(params.individual_length * (Types.NUM_PLAYERS-1), random, max_actions);
            if (params.init_type == INIT_RANDOM) {
                population[i].randomize();
                population_opp[i].randomize();
//                gInterface.evaluate(population[i], null, params.evaluate_update);
                gInterface.evaluate_with_opponent(population[i], null, params.evaluate_update, population_opp[0]);
            }
        }
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

    private Individual crossover(Individual[] population){
        Individual parent1 = select(population);
        Individual parent2 = select(population, parent1);

        return crossoverClass.cross(parent1, parent2);
    }

    private Individual[] generate_opponent_offspring(int max_actions) {
        Individual[] offspring = new Individual[params.offspring_count];
        for (int k = 0; k < params.offspring_count; k++) {
            offspring[k] = new Individual(params.individual_length*(Types.NUM_PLAYERS-1), random, max_actions);
        }
        for (int i = 0; i < params.offspring_count; i++) {
            if (params.genetic_operator == MUTATION_ONLY || params.population_size <= 2) {
                offspring[i] = population_opp[random.nextInt(population_opp.length)].copy();
            } else {
                offspring[i] = crossover(population_opp);
            }
            if (params.genetic_operator != CROSSOVER_ONLY) {
//                mutationClass.findGenesToMutate();
                gInterface.evaluate_opponent(population[0], mutationClass, params.evaluate_update, offspring[i]);
            } else {
                gInterface.evaluate_opponent(population[0], null, params.evaluate_update, offspring[i]);
            }
        }
        return offspring;
    }

    private Individual[] generate_offspring_with_opponent() {
        Individual[] offspring = new Individual[params.offspring_count];
        for (int i = 0; i < params.offspring_count; i++) {
            if (params.genetic_operator == MUTATION_ONLY || params.population_size <= 2) {
                offspring[i] = population[random.nextInt(population.length)].copy();
            } else {
                offspring[i] = crossover(population);
            }
            if (params.genetic_operator != CROSSOVER_ONLY) {
                mutationClass.findGenesToMutate();
                gInterface.evaluate_with_opponent(offspring[i], mutationClass, params.evaluate_update, population_opp[0]);
            } else {
                gInterface.evaluate_with_opponent(offspring[i], null, params.evaluate_update, population_opp[0]);
            }
        }
        return offspring;
    }
    private Individual[] generate_offspring() {
        Individual[] offspring = new Individual[params.offspring_count];
        for (int i = 0; i < params.offspring_count; i++) {
            if (params.genetic_operator == MUTATION_ONLY || params.population_size <= 2) {
                offspring[i] = population[random.nextInt(population.length)].copy();
            } else {
                offspring[i] = crossover(population);
            }
            if (params.genetic_operator != CROSSOVER_ONLY) {
                mutationClass.findGenesToMutate();
                gInterface.evaluate(offspring[i], mutationClass, params.evaluate_update);
            } else {
                gInterface.evaluate(offspring[i], null, params.evaluate_update);
            }
        }
        return offspring;
    }

    /**
     * Assumes population and offspring are already sorted in descending order by individual fitness
     * @param offspring - offspring created from parents population
     */
    @SuppressWarnings("unchecked")
    private void combine_and_sort_population_opp(Individual[] offspring){
        int startIdx = 0;

        // Make sure we have enough individuals to choose from for the next population
        if (params.offspring_count < params.population_size) params.keep_parents_next_gen = true;

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // First no_elites individuals remain the same, the rest are replaced
            startIdx = params.no_elites;
        }

        if (params.keep_parents_next_gen) {
            // Reevaluate current population
            if (params.reevaluate_pop) {
                for (Individual i : population_opp) {
                    gInterface.evaluate_opponent(population[0], null, params.evaluate_update, i);
                }
            }
            // If we should keep best individuals of parents + offspring, then combine array
            offspring = Utilities.add_array_to_array(population_opp, offspring, startIdx);
            Arrays.sort(offspring, Comparator.naturalOrder());
        }

        // Combine population with offspring, we keep only best individuals. If parents should not be kept, new
        // population is only best POP_SIZE offspring individuals.
        int nextIdx = 0;
        for (int i = startIdx; i < params.population_size; i++) {
            population_opp[i] = offspring[nextIdx].copy();
            nextIdx ++;
        }

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // If parents were kept to new generation and we had elites, population needs sorting again
            Arrays.sort(population_opp, Comparator.naturalOrder());
        }
    }

    private void combine_and_sort_population(Individual[] offspring){
        int startIdx = 0;

        // Make sure we have enough individuals to choose from for the next population
        if (params.offspring_count < params.population_size) params.keep_parents_next_gen = true;

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // First no_elites individuals remain the same, the rest are replaced
            startIdx = params.no_elites;
        }

        if (params.keep_parents_next_gen) {
            // Reevaluate current population
            if (params.reevaluate_pop) {
                for (Individual i : population) {
                    gInterface.evaluate_with_opponent(i, null, params.evaluate_update, population_opp[0]);
                }
            }
            // If we should keep best individuals of parents + offspring, then combine array
            offspring = Utilities.add_array_to_array(population, offspring, startIdx);
            Arrays.sort(offspring, Comparator.reverseOrder());
        }

        // Combine population with offspring, we keep only best individuals. If parents should not be kept, new
        // population is only best POP_SIZE offspring individuals.
        int nextIdx = 0;
        for (int i = startIdx; i < params.population_size; i++) {
            population[i] = offspring[nextIdx].copy();
            nextIdx ++;
        }

        if (params.elitism && params.keep_parents_next_gen && params.population_size > 1) {
            // If parents were kept to new generation and we had elites, population needs sorting again
            Arrays.sort(population, Comparator.reverseOrder());
        }
    }

    private void shift_population(int max_actions) {
        // Remove first action of all individuals and add a new random one at the end
        for (int i = 0; i < params.population_size; i++) {
            for (int j = 1; j < params.individual_length; j++) {
                population[i].set_action(j - 1, population[i].get_action(j));
                for (int k = 0; k < Types.NUM_PLAYERS-1; k++) {
                    population_opp[i].set_action(k*params.individual_length + j - 1, population_opp[i].get_action(k*params.individual_length + j));
                }
            }
            population[i].set_action(params.individual_length - 1, random.nextInt(max_actions));
            for (int k = 0; k < Types.NUM_PLAYERS-1; k++) {
                population_opp[i].set_action(params.individual_length * k + params.individual_length - 1, random.nextInt(max_actions));
            }
//            gInterface.evaluate(population[i], null, EVALUATE_UPDATE_AVERAGE);
            gInterface.evaluate_with_opponent(population[i], null, params.evaluate_update, population_opp[0]);
//            population[i].discount_value(params.shift_discount);
        }
    }
}
