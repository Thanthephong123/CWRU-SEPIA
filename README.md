# CWRU-SEPIA: AI Agent Implementations for SEPIA

CWRU-SEPIA is a project focused on implementing AI agents within the **SEPIA** platform, a simulation environment for resource collection, maze navigation, and strategic planning games. SEPIA was developed by researchers at **Case Western Reserve University (CWRU)** and provides a framework for testing AI decision-making in various game scenarios. More details about SEPIA can be found [here](http://engr.case.edu/ray_soumya/Sepia/html/index.html).

I implemented custom AI agent behaviors designed to operate within SEPIA, focusing on decision-making logic for agents in various scenarios. The logic for these agents, which includes pathfinding, adversarial game strategies, and resource collection planning, has been developed and implemented entirely within the src/ folder. These agents demonstrate various AI techniques applied to different in-game challenges.

## Overview

The project incorporates various AI techniques to create efficient and effective agents capable of tackling challenges in the SEPIA environment. It includes implementations of heuristic search algorithms, adversarial search strategies, and state-space planning methodologies to enable the agents to navigate the environment, evade obstacles, engage in combat, and optimize resource collection.

## Folder Structure

The source code of the AI agents is organized inside the `src/` folder. This contains implementations of:
- **Heuristic Search and A* Pathfinding** for maze navigation.
- **Minimax with Alpha-Beta Pruning** for combat scenarios.
- **State-Space Planning** for resource management and collection tasks.

## Implementation Details

- **Heuristic Search**: The project includes an implementation of the heuristic search algorithm, which enables the agents to explore the maze efficiently by selecting the most promising paths based on heuristic evaluations.

- **A* Search**: Additionally, the project utilizes the A* search algorithm, which combines the benefits of heuristic search with optimal pathfinding, resulting in more informed and effective navigation decisions for the agents.

- **Minimax Agent**: The Minimax Agent is designed for adversarial scenarios where two players compete, controlling different unit types in combat. It uses **alpha-beta pruning** to optimize decision-making within a game tree.
  - The agent controls **Footmen**, melee units that must engage in close combat.
  - The opponent, controlled by an **Archer Agent**, utilizes ranged attacks to outmaneuver and defeat the Footmen.
  - The agent evaluates potential moves using **game tree search**, determining the best course of action based on a given depth of search.
  - **Alpha-beta pruning** reduces the number of nodes explored, improving efficiency.
  - A **linear evaluation function** estimates the utility of a state based on factors such as proximity to enemies and the likelihood of trapping and defeating the Archers.
  - Heuristics guide move ordering to prioritize beneficial actions, such as attacking an adjacent Archer or closing the distance to the opponent.

- **Multiple State Space Planner Agent**: The Multiple State Space Planner Agent is a forward state-space planner designed for **resource collection** tasks. It utilizes **A* search** to generate an optimized sequence of actions for achieving a given resource target.
  - The environment consists of a **Townhall**, a **Peasant** unit, **gold mines**, and **forests**.
  - The agent executes actions such as **HarvestGold, HarvestWood, and Deposit** while considering optimal movement paths.
  - The **state-space planner** generates plans that minimize execution time (**makespan optimization**).
  - A heuristic guides search efficiency by evaluating the cost-effectiveness of different action sequences.
  - The agent extends its capabilities to **multi-agent coordination**, allowing for parallel execution of harvesting and depositing actions.
  - The **BuildPeasant** action introduces additional complexity by enabling the recruitment of new peasants to increase resource collection speed.
  - The agent dynamically schedules actions to optimize performance, balancing the immediate cost of hiring new peasants against long-term efficiency gains.



