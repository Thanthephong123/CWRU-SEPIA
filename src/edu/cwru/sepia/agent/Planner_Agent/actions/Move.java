package edu.cwru.sepia.agent.planner.actions;

import java.util.*;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.environment.model.state.Unit;

/**
 * Represents a move action in the SEP-lA planning environment where a unit (peasant)
 * is commanded to move to a specified position on the game map.
 */
public class Move implements StripsAction{

    // Current position of the peasant
    Position currentPos;
    // Desired goal position to move the peasant
    Position goalPos;
    // The ID of the peasant that will execute the move
    int peasantId;
    // Type of the action, here it is MOVE
    SepiaActionType type;

    /**
     * Constructs a Move action with specified parameters.
     *
     * @param peasantId The ID of the peasant to move.
     * @param currentPos The current position of the peasant.
     * @param goalPos The target position to move the peasant.
     */
    public Move(int peasantId, Position currentPos, Position goalPos){
        this.peasantId = peasantId;
        this.currentPos = currentPos;
        this.goalPos = goalPos;
        this.type = SepiaActionType.MOVE;
    }

    /**
     * Checks if the move action can be performed given the current game state.
     * This includes checking if the goal position is within the boundaries of the map.
     *
     * @param state The current state of the game.
     * @return true if the goal position is within the map boundaries, false otherwise.
     */
    public boolean preconditionsMet(GameState state) {
        if (!goalPos.inBounds(state.getXExtent(),
                                state.getYExtent())){
            return false;
        }
        return true;
    }

    /**
     * Applies this move action to the given game state, resulting in a new game state
     * where the peasant has moved to the goal position.
     *
     * @param state The current game state.
     * @return A new game state reflecting the peasant's movement.
     */
    public GameState apply(GameState state) {
        GameState childState = new GameState(state, this);
        childState.setPeasantPosition(goalPos);
        return childState;
    }

    /**
     * Calculates the cost associated with moving from the current position to the goal position.
     * The cost is typically defined as the distance between these two positions.
     *
     * @return The cost of the move.
     */
    public double getCost(){
        double cost = 0.0;
        cost += (currentPos.chebyshevDistance(goalPos));
        return cost;
    }

    /**
     * Get the target position of this move.
     *
     * @param index The index of a peasant
     * @return The target position of the peasant
     */
    public Position targetPosition(){
        return goalPos;
    }

    public Position getPosition(){
        return this.currentPos;
    }

    /**
     * Retrieves the type of this action, which is MOVE.
     *
     * @return The action type of MOVE.
     */
    public SepiaActionType getType(){
        return this.type;
    }

    /**
     * Placeholder for future implementation to get the Unit associated with this action.
     * Currently not implemented.
     *
     * @return null as it is not implemented.
     */
    public int getUnitId(){
        return peasantId;
    }

    /**
     * Provides a string representation of the move action, detailing the peasant ID and the target position.
     *
     * @return A string that represents the move action.
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Move:");
        sb.append("\t(" + peasantId + ", " + goalPos + ")");
        return sb.toString();
    }
    
}
