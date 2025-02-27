package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.agent.planner.actions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
  * Note that SEPIA saves the townhall as a unit. Therefore when you create a GameState instance,
 * you must be able to distinguish the townhall from a peasant. This can be done by getting
 * the name of the unit type from that unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns "townhall" or "peasant"
 * 
 * You will also need to distinguish between gold mines and trees.
 * state.getResourceNode(id).getType(): returns the type of the given resource
 * 
 * You can compare these types to values in the ResourceNode.Type enum:
 * ResourceNode.Type.GOLD_MINE and ResourceNode.Type.TREE
 * 
 * You can check how much of a resource is remaining with the following:
 * state.getResourceNode(id).getAmountRemaining()
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {

    //Peasant information
    public HashMap<Integer, Peasant> peasants;
    public Stack<StripsAction> actions;
    public boolean buildPeasants;
    public int highestID;

    //Resource information
	public HashMap<Integer, ResourceNodeWrapper> resources;
	public PriorityQueue<ResourceNodeWrapper> closestTree;
	public PriorityQueue<ResourceNodeWrapper> closestGoldMine;
    public int requiredGold;
	public int requiredWood;
	public int obtainedGold;
	public int obtainedWood;
	public int foodAmount;

    //Map information
	public int xExtent;
	public int yExtent;
	public Position townhallLocation;
	public int townhallID;
    
    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
        //Get world information from the state.
        this.yExtent = state.getYExtent();
        this.xExtent = state.getXExtent();
        
        //Initialize structures for storing peasants, resources, and actions.
        this.peasants = new HashMap<Integer,Peasant>();
    	this.resources = new HashMap<Integer, ResourceNodeWrapper>();
    	this.actions = new Stack<StripsAction>();
    	this.closestGoldMine = new PriorityQueue<ResourceNodeWrapper>();
    	this.closestTree = new PriorityQueue<ResourceNodeWrapper>();

        //Sort the peasants from the townhall. Add the peasants to the HashMap and set up townhall information.
        this.highestID = -1;
        for(Unit.UnitView unit : state.getUnits(playernum)) {
        	if(unit.getTemplateView().getName().equals("Peasant"))
        		peasants.put(unit.getID(), new Peasant(unit));
        	if(unit.getTemplateView().getName().equals("TownHall")) {
        		townhallLocation = new Position(unit.getXPosition(), unit.getYPosition());
        		townhallID = unit.getID();
        	}
        	int currentID = unit.getID();
        	if (currentID > this.highestID) {
        		this.highestID = currentID;
        	}
        }

        //Sort the mines from the forests and add the resources to their respective queue.
        for(ResourceNode.ResourceView resource : state.getAllResourceNodes()) {
        	resources.put(resource.getID(), new ResourceNodeWrapper(resource));
        	
        	if(resource.getType().equals(ResourceNode.Type.GOLD_MINE)) {
        		closestGoldMine.add(new ResourceNodeWrapper(resource));
        	} else if(resource.getType().equals(ResourceNode.Type.TREE)) {
        		closestTree.add(new ResourceNodeWrapper(resource));
        	}
        	if (resource.getID() > this.highestID) {
        		this.highestID = resource.getID();
        	}
        }

        //Set information for problem definition.
        this.requiredGold = requiredGold;
        this.requiredWood = requiredWood;
        this.buildPeasants = buildPeasants;
        
        //Set information for state tracking.
        obtainedGold = state.getResourceAmount(playernum, ResourceType.GOLD);
        obtainedWood = state.getResourceAmount(playernum, ResourceType.WOOD);
        foodAmount = state.getSupplyCap(playernum)-1;
    }

    /**
     * Constructor for making an immutable copy of a given GameState
     * @param stateToCopy The GameState that is being copied.
     */
    public GameState(GameState stateToCopy) {
        //Setting map information from the state being copied.
    	xExtent = stateToCopy.xExtent;
    	yExtent = stateToCopy.yExtent;
    	
    	//Set townhall information from the state being copied.
        townhallLocation = stateToCopy.townhallLocation;
        townhallID = stateToCopy.townhallID;
    	
        //Create structures for storing peasants, resources, and actions.
    	peasants = new HashMap<Integer,Peasant>();
    	resources = new HashMap<Integer, ResourceNodeWrapper>();
    	actions = new Stack<StripsAction>();
    	closestGoldMine = new PriorityQueue<ResourceNodeWrapper>();
    	closestTree = new PriorityQueue<ResourceNodeWrapper>();
    	
    	//Copy the peasants from the GameState being copied.
    	for(Peasant worker : stateToCopy.peasants.values()) {
        	peasants.put(worker.id, new Peasant(worker));
        }

        //Copy the resources from the GameState being copied.
        for(ResourceNodeWrapper resource : stateToCopy.resources.values()) {
        	ResourceNodeWrapper resourceCopy = new ResourceNodeWrapper(resource);
        	resources.put(resource.id, resourceCopy);
        	if(resourceCopy.type.equals(ResourceNode.Type.GOLD_MINE) && resourceCopy.remainingResources>0) {
        		closestGoldMine.add(resourceCopy);
        	} else if (resourceCopy.type.equals(ResourceNode.Type.TREE) && resourceCopy.remainingResources>0) {
        		closestTree.add(resourceCopy);
        	}
        }

        //Copy problem definition info from the GameState being copied.
        this.requiredGold = stateToCopy.requiredGold;
        this.requiredWood = stateToCopy.requiredWood;
        this.buildPeasants = stateToCopy.buildPeasants;
        this.highestID = stateToCopy.highestID;
        
        //Copy state tracking information from the GameState being copied.
        obtainedGold = stateToCopy.obtainedGold;
        obtainedWood = stateToCopy.obtainedWood;
        foodAmount = stateToCopy.foodAmount;

        //Copy actions from the GameState being copied.
        for(StripsAction action : stateToCopy.actions) {
        	actions.push(action);
        }
    }

    public GameState getParent(){
        return this.parent;
    }

    public StripsAction getParentAction(){
        return this.parentAction;
    }

    public int getXExtent(){
        return this.xExtent;
    }
    
    public int getYExtent(){
        return this.yExtent;
    }

    public void setPeasantPosition(Position pos){
        this.peasantPosition = pos;
    }

    public Resource getResourceById(int id){
        for(Forest forest : forests){
            if(forest.id == id){
                return forest;
            }
        }
        for(GoldMine mine : goldmines){
            if(mine.id == id){
                return mine;
            }
        }
        return null;
    }

    public void setPeasantCarryingType(ResourceType type){
        this.peasantCarryingType = type;
    }

    public void setPeasantCarryingAmount(int amount){
        this.peasantCarryingAmount = amount;
    }

     /**
     * Get the peasant.
     *
     * @param id The unit ID of the peasant
     * @return The identified peasant
     */
    public Unit.UnitView getPeasant(){
        return this.peasant;
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return (currentWoodAmount >= requiredWood) && (currentGoldAmount >= requiredGold);
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        // TODO: Implement me!
        List<GameState> children = new ArrayList<>();


        // Add all possible move states
        List<Position> positions = (generatePosition());
        for(Position position : positions){
        Move move = new Move(this.peasantID,this.peasantPosition,position);
        if (move.preconditionsMet(this)) children.add(move.apply(this));
        }
        /* for (Position position : generatePosition()) {
            Move move = new Move(this.peasantID,this.peasantPosition,position);
            if (move.preconditionsMet(this)) children.add(move.apply(this));
        } */

        //Add all possible deposit states
        Deposit deposit = new Deposit(this.townhallID, this.townhallPosition, this.peasantID, this.peasantCarryingAmount, this.peasantCarryingType, this.peasantPosition);
        if (deposit.preconditionsMet(this))
            children.add(deposit.apply(this));

        //Add all possible harvest states
        if (generateResource() != null) {
            Harvest harvest = new Harvest(this.peasantID, this.peasantCarryingAmount, this.peasantCarryingType, this.peasantPosition, generateResource());
            if (harvest.preconditionsMet(this))
                children.add(harvest.apply(this));
        }
        return children;
    }

    /**
     * Identifies the closest resource (either forest or gold mine) that is directly adjacent to the peasant.
     * This method is useful for determining which resource the peasant should interact with next, based on proximity.
     *
     * @return The Resource object adjacent to the peasant, or null if no such resource exists.
     */
    public Resource generateResource() {
        for (Position position : peasantPosition.getAdjacentPositions()) {  
            for (Resource resource : this.getAllResources()) {
                if (resource.position.equals(position)) {
                    return resource;
                }
            }
        }
        return null;
    }

    /**
   * Generates a list of viable positions for the peasant to move to, based on the current game state and
   * the resources that need to be collected. If carrying a resource, it suggests positions adjacent to the townhall;
   * otherwise, it suggests positions adjacent to needed resources.
   *
   * @return A list of potentially beneficial positions for the peasant to move towards.
   */
    private List<Position> generatePosition() {
        List<Position> positions = new ArrayList<>();
            //if we need wood, then add wood
            if (needWood()) {
                positions.addAll(this.forests.stream().filter(resource -> resource.getAmountRemaining() > 0).map(resource -> getBestPosition(resource.getPosition().getAdjacentPositions())).collect(Collectors.toList()));
            }
            // Else add gold
            if (needGold()) {
                positions.addAll(this.goldmines.stream().filter(resource -> resource.
                getAmountRemaining() > 0).map(resource -> getBestPosition(resource.getPosition().getAdjacentPositions())).collect(Collectors.toList()));
            }
            // The peasant is carrying cargo, hence, return to townhall
            positions.addAll((this.townhallPosition.getAdjacentPositions()));
        return positions;
    }

 /**
     * Determines the best position from a list of possible positions based on proximity
     * to the peasant's current position. It calculates the Chebyshev distance for each
     * position and returns the position with the shortest distance.
     *
     * @param positions A list of positions to evaluate.
     * @return The position from the list that is closest to the peasant's current position.
     */
    private Position getBestPosition(List<Position> positions) {
        Position currentPosition = this.peasantPosition;
        if (positions.isEmpty()) return peasantPosition;

        //Find the closest position available
        Position bestPosition = positions.get(0);

        for (Position position : positions) {
            if (position.chebyshevDistance(currentPosition) < bestPosition.chebyshevDistance(currentPosition)) {
                bestPosition = position;
            }
        }
        return bestPosition;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */

    // Work on improving later
    public double heuristic() {
        double heuristic = 0.0;
        heuristic += (requiredGold + requiredWood) * 10;
        heuristic -= peasantCarryingAmount * 10;
        heuristic -= (currentGoldAmount + currentWoodAmount) * 10;

        Forest forest = findClosestForest();
        GoldMine mine = findClosestGoldMine();

        if(peasantCarryingAmount == 0){
            heuristic -= peasantCarryingAmount * 10;
            heuristic += forest.position.chebyshevDistance(peasantPosition);
            heuristic += mine.position.chebyshevDistance(peasantPosition);
            if(!needGold()){
                heuristic += forest.position.chebyshevDistance(peasantPosition);
            }
            if(!needWood()){
                heuristic += mine.position.chebyshevDistance(peasantPosition);
            }
            if(townhallPosition.getAdjacentPositions().contains(peasantPosition)){
                heuristic += (forest.position.chebyshevDistance(peasantPosition)) + mine.position.chebyshevDistance(peasantPosition);
            }
            heuristic -= peasantCarryingAmount;
        }
        else{
            heuristic += townhallPosition.chebyshevDistance(peasantPosition) * 100;
        }
        return heuristic;
    }

    /**
     * Finds the closest gold mine to the peasant's current position.
     * This method iterates through all the gold mines in the game state,
     * calculating the Chebyshev distance between each gold mine and the peasant,
     * and returns the gold mine with the smallest distance.
     *
     * @return The closest GoldMine object to the peasant.
     */
    public GoldMine findClosestGoldMine() {
        GoldMine closestGoldMine = goldmines.get(0);
        int thisDistance, closestDistance;

        for (GoldMine goldmine: goldmines) {
            thisDistance = goldmine.position.chebyshevDistance(peasantPosition);
            closestDistance = closestGoldMine.position.chebyshevDistance(peasantPosition);
            if (thisDistance < closestDistance) closestGoldMine = goldmine;
        }
        return closestGoldMine;
    }

 /**
     * Finds the closest forest to the peasant's current position.
     * This method iterates through all forests in the game state,
     * calculating the Chebyshev distance between each forest and the peasant,
     * and returns the forest with the smallest distance.
     *
     * @return The closest Forest object to the peasant.
     */
    public Forest findClosestForest() {
        Forest closestForest = forests.get(0);
        int thisDistance, closestDistance;

        for (Forest forest: forests) {
            thisDistance = forest.position.chebyshevDistance(peasantPosition);
            closestDistance = closestForest.position.chebyshevDistance(peasantPosition);
            if (thisDistance < closestDistance) closestForest = forest;
        }
        return closestForest;
    }
    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        //If the state has no parent, then return 0
        return parentAction == null && parent == null ? 0 : parent.getCost() + parentAction.getCost();
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        double thisUtility = this.getCost() + this.heuristic();
        double thatUtility =    o.getCost() +    o.heuristic();
        if (thisUtility > thatUtility)
            return 1;
        else if (thisUtility == thatUtility)
            return 0;
        else
            return -1;
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        // TODO: Implement me!
        return o instanceof GameState &&
        this.currentWoodAmount == ((GameState) o).currentWoodAmount &&
        this.currentGoldAmount == ((GameState) o).currentGoldAmount &&

        this.peasantID == ((GameState) o).peasantID &&
        this.peasantPosition == ((GameState) o).peasantPosition &&
        this.peasantCarryingAmount == ((GameState) o).peasantCarryingAmount &&
        this.peasantCarryingType == ((GameState) o).peasantCarryingType &&

        this.townhallID == ((GameState) o).townhallID &&
        this.townhallPosition == ((GameState) o).townhallPosition &&

        this.goldmines.equals(((GameState) o).goldmines) &&
        this.forests.equals(((GameState) o).forests);


    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        //Implement later
        return 0;
    }

    * Adds resources to the current tally based on the type specified.
     *
     * @param carryingType The type of resource to add (e.g., GOLD or WOOD).
     * @param carryingAmount The amount of the resource to add.
     */
    public void addResource(ResourceType carryingType, int carryingAmount) {
        switch (carryingType) {
            case GOLD:
                currentGoldAmount += carryingAmount;
                break;
            case WOOD:
                currentWoodAmount += carryingAmount;
        }
    }

 /**
    * Removes a specified resource from the game state.
    *
    * @param resource The resource to remove, which can either be a forest or a goldmine.
    */
    public void removeResource(Resource resource) {
        switch (resource.type) {
            case WOOD:
                forests.remove(resource);
                break;
            case GOLD:
                goldmines.remove(resource);
                break;
        }
    }

  /**
   * Checks if additional gold is needed to meet the goal.
   *
   * @return true if the current gold amount is less than the required gold amount.
   */
    public boolean needGold() {
        return currentGoldAmount < requiredGold;
    }

 /**
     * Checks if additional wood is needed to meet the goal.
     *
     * @return true if the current wood amount is less than the required wood amount.
     */
    public boolean needWood() {
        return currentWoodAmount < requiredWood;
    }

  /**
    * Retrieves the amount of resource currently being carried by the peasant.
    *
    * @return The amount of resource being carried.
    */
    public int getPeasantCargoAmount() {
        return peasantCarryingAmount;
    }

 /**
    * Retrieves the type of resource currently being carried by the peasant.
    *
    * @param peasantID The ID of the peasant whose cargo type is to be retrieved.
    * @return The type of resource being carried.
    */
    public ResourceType getPeasantCargoType(Integer peasantID) {
        return peasantCarryingType;
    }

 /**
    * Retrieves the current position of the peasant.
    *
    * @return The current position of the peasant.
    */
    public Position getPeasantPosition() {
        return peasantPosition;
    }

 /**
    * Clears the cargo currently being carried by the specified peasant.
    *
    * @param peasantID The ID of the peasant whose cargo is to be cleared.
    */
    public void clearPeasantCargo(int peasantID) {
        this.peasantCarryingAmount = 0;
        this.peasantCarryingType = null;
    }

 /**
   * Retrieves a combined list of all resource locations (forests and goldmines) in the game state.
   *
   * @return A list containing all resource locations.
   */
    public List<Resource> getAllResources() {
        List<Resource> resources = new ArrayList<>();
        resources.addAll(this.goldmines);
        resources.addAll(this.forests);
        return resources;
    }

    /**
     * A representation of a Peasant.
     */
    public class Peasant {
    	public Position position;
    	public boolean hasLoad;
    	public ResourceType loadType;
    	public int id;
    	
    	public Peasant(UnitView unit) {
    		position = new Position(unit.getXPosition(), unit.getYPosition());
    		hasLoad = unit.getCargoAmount() != 0;
    		loadType = unit.getCargoType();
    		id = unit.getID();
    	}
    	
    	/**
    	 * Used to create new peasants without a UnitView or duplicate.
    	 * @param position the position of the new worker
    	 * @param id the id of the new worker
    	 */
    	public Peasant(Position position, int id) {
    		this.position = position;
    		this.id = id;
    		this.hasLoad = false;
    		this.loadType = null;
    	}
    	
    	/**
    	 * Acts as a cloning constructor to create an immutable copy of the given worker
    	 * @param workerToClone
    	 */
    	public Peasant(Peasant workerToClone) {
    		position = workerToClone.position.clone();
    		hasLoad = workerToClone.hasLoad;
    		loadType = workerToClone.loadType;
    		id = workerToClone.id;
    	}
    	
    	@Override
    	public String toString() {
    		return "worker-" + id;
    	}
    	
        @Override
        public boolean equals(Object o) {
        	if(!(o instanceof Peasant)) {
        		return false;
        	}
        	
            Peasant otherWorker = (Peasant) o;
            if(otherWorker.position.equals(position) && otherWorker.hasLoad == hasLoad) {
            	return true;
            } else {
            	return false;
            }
        }
    	
        @Override
        public int hashCode() {
        	return position.hashCode() + (hasLoad ? 1231 : 1237);
        }
    }

    /**
     * Wrapper class for ResourceNodes to help manage resources.
     */
    public class ResourceNodeWrapper implements Comparable<ResourceNodeWrapper> {
    	public Position position;
    	public ResourceNode.Type type;
    	public int remainingResources;
    	public int id;
    	
        /**
         * Constructor to create a ResourceNodeWrapper from a ResourceView.
         * @param resource The ResourceView from which the wrapper is created.
         */
    	public ResourceNodeWrapper(ResourceNode.ResourceView resource) {
    		position = new Position(resource.getXPosition(), resource.getYPosition());
    		type = resource.getType();
    		remainingResources = resource.getAmountRemaining();
    		id = resource.getID();
    	}
    	
        /**
         * Constructor to make an immutable copy a ResourceNodeWrapper
         * @param resourceToCopy The ResourceNodeWrapper being copied.
         */
    	public ResourceNodeWrapper(ResourceNodeWrapper resourceToCopy) {
    		position = resourceToCopy.position.clone();
    		type = resourceToCopy.type;
    		remainingResources = resourceToCopy.remainingResources;
    		id = resourceToCopy.id;
    	}
    	
        /**
         * Update the amount of resources remaining in the Node.
         * @param amount The amount of resource to be removed from the Node.
         */
    	public void removeResources(int amount) {
    		remainingResources = remainingResources-amount;
    	}
    	
        /**
         * Compare a ResourceNodeWrapper to another Node.
         * @param otherNode The ResourceNodeWrapper being compared to.
         * @return The difference in position between this ResourceNodeWrapper and otherNode.
         */
		@Override
		public int compareTo(ResourceNodeWrapper otherNode) {
			return (int) (position.chebyshevDistance(townhallLocation)-otherNode.position.chebyshevDistance(townhallLocation));
		}
    	
        /**
         * Create and return a String representation of a ResourceNodeWrapper.
         * @return The String representation of the ResourceNodeWrapper.
         */
    	@Override
    	public String toString() {
    		return type.toString() + "-" + id;
    	}
    	
        /**
         * Check the equality of this ResourceNodeWrapper to a provided Object, o.
         * @param o The object to be checked for equality.
         */
        @Override
        public boolean equals(Object o) {
        	if(!(o instanceof ResourceNodeWrapper)) {
        		return false;
        	}
        	
        	ResourceNodeWrapper otherResource = (ResourceNodeWrapper) o;
            if(otherResource.position.equals(position) && otherResource.type == type 
            		&& otherResource.remainingResources == remainingResources) {
            	return true;
            } else {
            	return false;
            }
        }
    	
        /**
         * Calculate and return a hashcode to be associated with this ResourceNodeType
         * @return An int hashcode to represent the ResourceNodeWrapper.
         */
        @Override
        public int hashCode() {
        	int hash = position.hashCode();
        	hash = hash + (type.equals(ResourceNode.Type.TREE) ? 1231 : 1237);
        	hash = hash*31+remainingResources;
            return hash;
        }
    }
}
