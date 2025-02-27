package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the action of depositing resources (like wood or gold) into a townhall in a game scenario.
 * This class specifically handles the Strips representation of the deposit action where a peasant
 * deposits the resources it is carrying into a townhall.
 */
public class Deposit implements StripsAction {

    // ID of the townhall where resources are to be deposited
    private final int townhallID;
    // Position of the townhall on the game map
    private final Position townhallPosition;
    // ID of the peasant carrying the resources
    private int peasantId;
    // Amount of resources being carried by the peasant
    private int carryingAmount;
    // Type of resources being carried (e.g., GOLD, WOOD)
    private ResourceType carryingType;
    // Current position of the peasant
    private Position peasantPos;

    /**
     * Construct a deposit strips action.
     *
     * @param peasantIDs A list of peasant IDs who will do the depositing
     * @param townhallID The ID of the townhall into which the resources will be deposited
     * @param townhallPosition The position of the townhall
     */
    public Deposit(int townhallID, Position townhallPosition, int peasantId, int carryingAmount, ResourceType carryingType, Position peasantPos) {
        this.townhallID = townhallID;
        this.townhallPosition = townhallPosition;
        this.peasantId = peasantId;
        this.carryingAmount = carryingAmount;
        this.carryingType = carryingType;
        this.peasantPos = peasantPos;
    }

    /**
     * Checks if the preconditions for depositing resources are met.
     * 
     * @param state The current state of the game.
     * @return true if the peasant is adjacent to the townhall and is carrying resources, false otherwise.
     */
    @Override
    public boolean preconditionsMet(GameState state) {
        if (carryingAmount > 0 && peasantPos.isAdjacent(townhallPosition)) {
            return true;
        }
        return false;
    }

    /**
     * Applies the deposit action to the given state, updating the game state accordingly.
     *
     * @param state The current state of the game before this action.
     * @return A new GameState reflecting the changes after this action.
     */
    @Override
    public GameState apply(GameState state) {
        GameState childState = new GameState(state, this);
        childState.addResource(carryingType, carryingAmount);
        childState.clearPeasantCargo(peasantId);
        return childState;
    }

    /**
     * Returns the position of the townhall where resources are being deposited.
     *
     * @return The position of the townhall.
     */
    @Override
    public Position targetPosition() {
        return townhallPosition;
    }

    /**
     * Returns the cost of performing this action.
     *
     * @return A double representing the cost of the deposit action, which is 0 in this implementation.
     */
    @Override
    public double getCost() {
        return -50;
    }

    /**
     * Provides a string representation of the deposit action.
     *
     * @return A string that describes the deposit action including peasant and townhall IDs.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Deposit:");
        sb.append("\t(" + peasantId + ", " + townhallID + ")");
        return sb.toString();
    }

    /**
     * Returns the SEPIA action type associated with deposit actions.
     *
     * @return The SepiaActionType enum value for deposit.
     */
    @Override
    public SepiaActionType getType() {
        return SepiaActionType.DEPOSIT;
    }

    /**
     * Gets the unit associated with this action. In the case of deposit, it typically refers to the peasant.
     *
     * @return A view of the peasant unit, or null if not applicable.
     */
    @Override
    public int getUnitId() {
        return peasantId;
    }

    @Override
    public Position getPosition(){
        return this.peasantPos;
    }
}
