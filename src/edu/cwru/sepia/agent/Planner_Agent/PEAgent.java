package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Implements an agent that executes a plan created by the PlannerAgent. The execution involves translating the high-level
 * Strips actions into SEPIA-specific actions and managing the execution of these actions to achieve the game objectives.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // Maps the placeholder unit IDs from the plan to the actual unit IDs assigned by SEPIA
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;
    // Counts the number of steps taken for debug purposes
    private int steps;

/**
     * Constructs a PEAgent with a specific plan.
     * 
     * @param playernum The player number this agent controls.
     * @param plan The plan to execute, composed of a stack of Strips actions.
     */
    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;

    }

/**
     * Called at the start of the agent's operation to initialize game state dependent variables.
     * 
     * @param stateView Current state of the game.
     * @param historyView Record of all past actions and their results.
     * @return Actions to be taken based on the current state and the plan.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // Initialize townhall and peasant IDs
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(0, unitId);
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
	 * To check a unit's progress on the action they were executing last turn, you can use the following:
     * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).get(unitID).getFeedback()
     * This returns an enum ActionFeedback. When the action is done, it will return ActionFeedback.COMPLETED
     *
     * Alternatively, you can see the feedback for each action being executed during the last turn. Here is a short example.
     * if (stateView.getTurnNumber() != 0) {
     *   Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     *   for (ActionResult result : actionResults.values()) {
     *     <stuff>
     *   }
     * }
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        Map<Integer, Action> actionMap = new HashMap<>();

        int i = 0;
        for (Unit.UnitView unitView: stateView.getAllUnits()) {
            peasantIdMap.putIfAbsent(i++, unitView.getID());
        }

	// Monitor actions from previous turn and reapply unfinished actions
        if (stateView.getTurnNumber() != 0) {
            historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).values().stream()
                    .filter(result -> result.getFeedback() == ActionFeedback.INCOMPLETE)
                    .forEach(result -> actionMap.put(result.getAction().getUnitId(), result.getAction()));
            // When previous actions are complete, execute the next action from the plan
		if (actionMap.isEmpty() && !plan.isEmpty()){
                StripsAction nextAction = plan.pop();
                Action newAction = createSepiaAction(nextAction); // Ensure this method returns a map
                actionMap.put(peasantIdMap.get(0), newAction);
            }
        } else {
            Action initialAction = createSepiaAction(plan.pop());
            actionMap.put(peasantIdMap.get(0), initialAction);
        }

        steps++;  // Increment step counter
        
	if (plan.empty()) System.out.println("Number of steps taken: " + steps);
       
	return actionMap;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     * 
     * Hint:
     * peasantId could be found in peasantIdMap
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action) {
        Action returnAction = null;

        switch (action.getType()) {
            case MOVE:
                returnAction = Action.createCompoundMove(action.getUnitId(),
                action.targetPosition().x, action.targetPosition().y);
                break;
            case HARVEST:
                returnAction = Action.createPrimitiveGather(action.getUnitId(),
                    action.getPosition().getDirection(action.targetPosition()));
                break;
            case DEPOSIT:
                returnAction = Action.createPrimitiveDeposit(action.getUnitId(),
                        action.getPosition().getDirection(action.targetPosition()));
                break;
        }
        return returnAction;
    }
	
/**
 * This method is called at the termination of the simulation. The current implementation
 * does not perform any actions at this stage.
 *
 * @param stateView Provides a view of the game state at the terminal step.
 * @param historyView Provides a historical view of the game up to this point.
 */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

	
/**
 * Saves the player's data to an output stream. This method is meant for serializing
 * game-related data such as player state or preferences, which might be needed between
 * sessions or for logging purposes. The current implementation does not perform any actions.
 *
 * @param outputStream The output stream to which data should be written.
 */
    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

/**
 * Loads the player's data from an input stream. This method is used for deserializing
 * game-related data such as player state or preferences that were previously saved.
 * The current implementation does not perform any actions.
 *
 * @param inputStream The input stream from which data should be read.
 */
    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
