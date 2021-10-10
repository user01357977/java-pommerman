#!/usr/bin/env python
# coding: utf-8



import numpy as np
import pandas as pd
import json
import os




actions_to_integers = {
    'ACTION_STOP':0,
    'ACTION_UP':1,
    'ACTION_DOWN':2,
    'ACTION_LEFT':3,
    'ACTION_RIGHT':4,
    'ACTION_BOMB':5,
}

def get_json_from_experiment(path, game):
    if path[-1] != '/':
        path += '/'
        
    files = [file for file in os.listdir(path) if 'json' in file]
    
    print(files[game])
    with open(path+files[game], 'r') as file:
        data = json.load(file)
        
    return data



def analyse_experiment(path_to_experiment):
    # Making sure its in the right format for further analysis
    if path_to_experiment[-1] != '/':
        path_to_experiment += '/'

    files = [file for file in os.listdir(path_to_experiment) if 'json' in file]

    with open(path_to_experiment+files[0], 'r') as file:
        data = json.load(file)
    
    players = np.array(data['players_names'][0])
    players = players[players != None]
    
    number_of_players = players.size
    print("\033[1m")
    print('='*20+f'   Experiment: {players[0]} vs {players[1]}   '+'='*20)
    print("\033[0m")
    print("\033[4m")
    print(f'Log file: {path_to_experiment}')
    print("\033[0m")
    number_of_logs = len(files)
    print(f'Number of games {number_of_logs}')
    
    check_ties = []
    
    for player in range(number_of_players):
        for player_op in range(number_of_players):
            if player != player_op: opponent = player_op
    
        accurate_w = 0
        not_accurate_w = 0
        total_w = 0

        accurate_l = 0
        not_accurate_l = 0
        total_l = 0

        wins = 0
        losses = 0
        ties = 0

        accuracy_last_ticks_w = 0.
        accuracy_pre_last_ticks_w = 0.
        ticks_wm12 = 0.
        ticks_w12 = 0.

        accuracy_last_ticks_l = 0.
        accuracy_pre_last_ticks_l = 0.
        ticks_lm12 = 0.
        ticks_l12 = 0.

        # Accuracy by types of action
        accurate_by_action_w = np.zeros(6)
        actions_counter_w = np.zeros(6)

        accurate_by_action_l = np.zeros(6)
        actions_counter_l = np.zeros(6)

        print("\033[1m")
        print(f"Stats for player {player+1} [ {players[player]} ]  against {players[opponent]}")
        print("\033[0m")

        ### STATS ON ACCURACY AND RESULTS
        for log_num in range(number_of_logs):
            with open(path_to_experiment+files[log_num], 'r') as file:
                data = json.load(file)
            

            actions = data['actionsArrayList']
            predictions = data['predArrayList']
            outcomes = data['outcomes']

            total_ticks = len(actions)
            outcome = outcomes[player]


            if 'TIE' in outcomes:
                ties += 1
            elif outcome == 'WIN':
                wins += 1
            elif outcome == 'LOSS':
                losses += 1
                


            for tick in range(total_ticks):
                action = actions_to_integers[actions[tick][opponent]]
                prediction = predictions[tick][player][opponent]


                if outcome == 'WIN':
                    if action == prediction:
                        accurate_w += 1
                        accurate_by_action_w[action] += 1
                    else:

                        not_accurate_w += 1 # For testing

                    if tick < total_ticks - 12:
                        ticks_wm12 += 1
                        if action == prediction:
                            accuracy_pre_last_ticks_w += 1
                    else:
                        ticks_w12 += 1
                        if action == prediction:
                            accuracy_last_ticks_w += 1


                    actions_counter_w[action] += 1
                    total_w += 1

                if outcome == 'LOSS':
                    if action == prediction:
                        accurate_l += 1
                        accurate_by_action_l[action] += 1
                    else:
                        not_accurate_l += 1 # For testing


                    if tick < total_ticks - 12:
                        ticks_lm12 += 1.
                        if action == prediction:
                            accuracy_pre_last_ticks_l += 1

                    else:
                        ticks_l12 += 1.
                        if action == prediction:
                            accuracy_last_ticks_l += 1

                    total_l += 1
                    actions_counter_l[action] += 1


        print(f'Wins: {wins} ({wins / number_of_logs * 100:.2f}%) Losses: {losses} ({losses / number_of_logs * 100:.2f}%) Ties: {ties} ({ties / number_of_logs * 100:.2f}%) Check (should be 100.00%): {(wins+losses+ties)/number_of_logs*100:.2f}%\n')



        try:
            accuracy_w = accurate_w / total_w
            not_accuracy_w = not_accurate_w / total_w

            print('When WIN:')
            print(f'Accuracy: {accuracy_w*100:.2f}%')
            print(f'Not accurate: {not_accuracy_w*100:.2f}%')
            print(f'Check (should be 100.00%): {(accuracy_w+not_accuracy_w)*100:.2f}%')
            print(f'Accuracy pre last ticks {(accuracy_pre_last_ticks_w/ticks_wm12)*100:.2f}%')
            print(f'Accuracy last 12 ticks {(accuracy_last_ticks_w/ticks_w12)*100:.2f}%')
            accuracy_by_action_w = accurate_by_action_w / actions_counter_w
            action_keys = [a for a in actions_to_integers.keys()]
            print('Accuracy by action type:')
            for act in range(6):
                print(f'{action_keys[act]}: {accuracy_by_action_w[act]*100:.2f}%')

            print()
        except Exception as e:
            print("Exception:", e)
            print('Perhaps no wins?')

        try:
            accuracy_l = accurate_l / total_l
            not_accuracy_l = not_accurate_l / total_l
            print('When LOSS:')
            print(f'Accuracy when losing: {accuracy_l*100:.2f}%')
            print(f'Not accurate when winning: {not_accuracy_l*100:.2f}%')
            print(f'Check (should be 100.00%): {(accuracy_l+not_accuracy_l)*100:.2f}%') 
            print(f'Accuracy pre last ticks {(accuracy_pre_last_ticks_l/ticks_lm12)*100:.2f}%')
            print(f'Accuracy last 12 ticks {(accuracy_last_ticks_l/ticks_l12)*100:.2f}%')
            accuracy_by_action_l = accurate_by_action_l / actions_counter_l
            action_keys = [a for a in actions_to_integers.keys()]
            print('Accuracy by action type:')
            for act in range(6):
                print(f'{action_keys[act]}: {accuracy_by_action_l[act]*100:.2f}%')

        except Exception as e:
            print("Exception:", e)
            print('Perhaps no losses?')
    
        

        
    


if __name__ ==  "__main__":
	os.chdir("../res/gameLogs/")
	directory = os.listdir()
	experiments = [experiment for experiment in directory if os.path.isdir(experiment) and '.' not in experiment]
	print(experiments)

	for experiment in experiments:
		analyse_experiment(experiment)


	print("\033[1m")
	print("="*45+"END"+"="*45)
	print("\033[1m")
