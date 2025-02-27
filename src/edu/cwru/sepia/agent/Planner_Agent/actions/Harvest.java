package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.Resource;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.List;
import java.util.Map;

/**
 * Represents the STRIPS-like action of a peasant harvesting a resource in the game. 
 * This class encapsulates all the necessary attributes and methods for a peasant to perform
 * a harvest action, including checking preconditions and applying the action effect to the game state.
 */
public class Harvest implements StripsAction {

    // ID of the peasant that will execute the harvest
    private int peasantId;
    // Current amount of resource the peasant is carrying
    private int carryingAmount;
    // Type of resource the peasant is currently carrying
    private ResourceType carryingType;
    // Current position of the peasant
    private Position peasantPos;
    // Resource that is being harvested
    private Resource childResource;
    private Resource resource;
    // The type of the action
    SepiaActionType type;

     /**
     * Constructs a Harvest action for a specific peasant and resource.
     *
     * @param peasantId ID of the peasant executing the harvest.
     * @param carryingAmount Amount of resource currently carried by the peasant, expected to be 0 for harvest actions.
     * @param carryingType Type of the resource the peasant can carry.
     * @param peasantPos Current position of the peasant.
     * @param resource The resource to be harvested.
     */
    public Harvest(int peasantId, int carryingAmount, ResourceType carryingType, Position peasantPos, Resource resource){
        //super(units);
        this.peasantId = peasantId;
        this.carryingAmount = carryingAmount;
        this.carryingType = carryingType;
        this.resource = resource;
        this.type = SepiaActionType.HARVEST;
        this.peasantPos = peasantPos;
    }

    /**
     * Checks if the harvest action can be performed based on the game state.
     * Preconditions include the peasant carrying no resources and being adjacent to the target resource.
     *
     * @param state The current game state.
     * @return true if the preconditions are met, false otherwise.
     */
    @Override
    public boolean preconditionsMet(GameState state) {
        if (!(carryingAmount == 0 &&
                resource.getAmountRemaining() > 0 &&
                peasantPos.isAdjacent(resource.getPosition()))) {
            return false;
        }
        return true;
    }

    /**
     * Applies the harvest action to the game state, modifying resources and updating the peasant's state.
     *
     * @param state The current game state before this action.
     * @return A new GameState reflecting the changes after this action.
     */
    @Override
    public GameState apply(GameState state) {
        GameState childState = new GameState(state, this);
            childResource = childState.getResourceById(
                    resource.getID());
            if (childResource != null) {
                childState.setPeasantCarryingAmount(100);
                childState.setPeasantCarryingType(childResource.getType());
                childResource.harvestAmount(100);
                if (childResource.getAmountRemaining() <= 0) {
                    childState.removeResource(childResource);
                }
            }
        return childState;
    }

    /**
     * Returns the position of the resource that is being harvested.
     *
     * @return The position of the resource.
     */
    public Position targetPosition(){
        return resource.getPosition();
    }

    public Position getPosition(){
        return peasantPos;
    }

    /**
     * Provides a string representation of the harvest action including details about the peasant and resource.
     *
     * @return A string that represents the action.
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Harvest:");
        sb.append("\t(" + peasantId + ", " + resource.getID() + ")");
        return sb.toString();
    }

    /**
     * Retrieves the ID of the peasant performing the harvest.
     *
     * @return The peasant's ID.
     */
    public int getUnitId(){
        return this.peasantId;
    }

    /**
     * Retrieves the type of SEPIA action this harvest corresponds to.
     *
     * @return The SepiaActionType for harvest.
     */
    public SepiaActionType getType(){
        return SepiaActionType.HARVEST;
    }

    /**
     * Returns the cost associated with performing this action, typically representing the effort or resources spent.
     *
     * @return The cost of the harvest action.
     */
    public double getCost(){
        return -500;
    }

    
}
