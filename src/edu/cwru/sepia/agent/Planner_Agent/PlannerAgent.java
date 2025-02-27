package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.SepiaActionType;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.*;
import java.util.*;

/**
 * An Agent that plans using A* algorithm based on Strips actions to fulfill specified resource collection goals.
 * The agent plans the actions to be performed in the game by converting high-level STRIPS actions into low-level
 * SEPIA actions.
 */
public class PlannerAgent extends Agent {

    final int requiredWood;
    final int requiredGold;
    final boolean buildPeasants;

    // Your PEAgent implementation. This prevents you from having to parse the text file representation of your plan.
    PEAgent peAgent;

    /**
     * Constructs a PlannerAgent with specific goals.
     *
     * @param playernum The player number this agent represents
     * @param params The parameters for the game including required resources and peasant building allowance
     */
    public PlannerAgent(int playernum, String[] params) {
        super(playernum);

        if(params.length < 3) {
            System.err.println("You must specify the required wood and gold amounts and whether peasants should be built");
        }

        requiredWood = Integer.parseInt(params[0]);
        requiredGold = Integer.parseInt(params[1]);
        buildPeasants = Boolean.parseBoolean(params[2]);


        System.out.println("required wood: " + requiredWood + " required gold: " + requiredGold + " build Peasants: " + buildPeasants);
    }

     /**
     * This method is called every time the agent starts a new episode and is responsible for initializing
     * the plan.
     *
     * @param stateView The current state of the game
     * @param historyView The game history
     * @return The initial set of actions to be taken
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        Stack<StripsAction> plan = AstarSearch(new GameState(stateView, playernum, requiredGold, requiredWood, buildPeasants));

        System.out.println("Got through");
        if(plan == null) {
            System.err.println("No plan was found");
            return null;
        }

        // write the plan to a text file
        savePlan(plan);


        // Instantiates the PEAgent with the specified plan.
        peAgent = new PEAgent(playernum, plan);

        return peAgent.initialStep(stateView, historyView);
    }

    /**
     * This method is called at every step of the game except the first one and delegates the action
     * execution to the PEAgent.
     *
     * @param stateView The current state of the game
     * @param historyView The game history
     * @return The actions to be taken at the current step
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        if(peAgent == null) {
            System.err.println("Planning failed. No PEAgent initialized.");
            return null;
        }

        return peAgent.middleStep(stateView, historyView);
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }

    /**
     * Perform an A* search of the game graph. This should return your plan as a stack of actions. This is essentially
     * the same as your first assignment. The implementations should be very similar. The difference being that your
     * nodes are now GameState objects not MapLocation objects.
     *
     * @param startState The state which is being planned from
     * @return The plan or null if no plan is found.
     */
    private Stack<StripsAction> AstarSearch(GameState startState) {
        List<GameState> listofChildren = startState.generateChildren();
        
        //Create Lists of MapLocations to represent the open list and closed list for A* search.
        PriorityQueue<GameState> openList = new PriorityQueue<>();
        List<GameState> closedList = new ArrayList<GameState>();

        //Add the initial state to the open list.
        openList.add(startState);
        
        //Used to store the GameState being explored at a given point during A* search.
        GameState hold;
        int count = 0;
        //A* implementation (Variable "hold" here is the node to be expanded on the open list)
        while(!openList.isEmpty()){
            //Find the node on the openList with the best heuristic to be expanded.
            hold = openList.poll();
            openList.clear();

            System.out.print("Move chosen: ");
            if (hold.getParentAction() != null) {
                if (hold.getParentAction().getType() == SepiaActionType.MOVE) {
                    System.out.println("Move: " + hold.getPeasantPosition().x + ", " + hold.getPeasantPosition().y);
                }
                if (hold.getParentAction().getType() == SepiaActionType.HARVEST) {
                    System.out.println("HARVESTING " + hold.getPeasantPosition().x + ", " + hold.getPeasantPosition().y);
                }
                if (hold.getParentAction().getType() == SepiaActionType.DEPOSIT) {
                    System.out.println("DEPOSITING " + hold.getPeasantPosition().x + ", " + hold.getPeasantPosition().y);
                }
            }

            System.out.println("____________________________");

            if (hold.getPeasantPosition().x == 8 && hold.getPeasantPosition().y == 8) count++;
            if (count == 10) break;
            //If the node being checked is the goal, then construct the plan from it's ancestors.
            if(hold.isGoal()){
                return makePlan(hold);
            }
            
            //Find the children of the current node for expansion.
            List<GameState> children = hold.generateChildren();
            //Add the children that have not already been visited to the openList.
            for(GameState child : children){
                if(!closedList.contains(child) && !openList.contains(child)){
                    openList.add(child);
                }
            }

            //Add the expanded node to the closed list (already been removed from open by poll())
            closedList.add(hold);

            
            System.out.println("Inside the open list: ");
            for(GameState child : openList){    
                if (child.getParentAction().getType() == SepiaActionType.MOVE) {
                    System.out.println("Move: " + child.getPeasantPosition().x + ", " + child.getPeasantPosition().y + ". Heuristic: " + child.heuristic() + child.getCost());
                    System.out.println("Carrying: " + hold.getPeasantCargoAmount());
                }
                if (child.getParentAction().getType() == SepiaActionType.HARVEST) {
                    System.out.println("HARVESTING " + child.getPeasantPosition().x + ", " + child.getPeasantPosition().y + ". Heuristic: " + child.heuristic() + child.getCost());
                    System.out.println("Carrying: " + hold.getPeasantCargoAmount());
                }
                if (child.getParentAction().getType() == SepiaActionType.DEPOSIT) {
                    System.out.println("DEPOSITING " + child.getPeasantPosition().x + ", " + child.getPeasantPosition().y + ". Heuristic: " + child.heuristic() + child.getCost());
                    System.out.println("Carrying: " + hold.getPeasantCargoAmount());
                }

            }
        }

        // While loop terminates, meaning that there is no plan
        System.err.println("There is no plan");
        return null;
    }

    /**
     * Make a strips action plan that achieves the specified goal state.
     *
     * @param goal The goal state from which to trace back
     * @return A complete stack of strips actions that make up the plan
     */
    private Stack<StripsAction> makePlan(GameState goal) {
        Stack<StripsAction> plan = new Stack<>();
        GameState parent = goal;
        // Go to each node's parent and add it to the stack of moves
        while(parent.getParent() != null){
            plan.push(parent.getParentAction());
            parent = parent.getParent();
        }
        return plan;
    }

    /**
     * This has been provided for you. Each strips action is converted to a string with the toString method. This means
     * each class implementing the StripsAction interface should override toString. Your strips actions should have a
     * form matching your included Strips definition writeup. That is <action name>(<param1>, ...). So for instance the
     * move action might have the form of Move(peasantID, X, Y) and when grounded and written to the file
     * Move(1, 10, 15).
     *
     * @param plan Stack of Strips Actions that are written to the text file.
     */
    private void savePlan(Stack<StripsAction> plan) {
        if (plan == null) {
            System.err.println("Cannot save null plan");
            return;
        }

        File outputDir = new File("saves");
        outputDir.mkdirs();

        File outputFile = new File(outputDir, "plan.txt");

        PrintWriter outputWriter = null;
        try {
            outputFile.createNewFile();

            outputWriter = new PrintWriter(outputFile.getAbsolutePath());

            Stack<StripsAction> tempPlan = (Stack<StripsAction>) plan.clone();
            while(!tempPlan.isEmpty()) {
                outputWriter.println(tempPlan.pop().toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputWriter != null)
                outputWriter.close();
        }
    }
}
