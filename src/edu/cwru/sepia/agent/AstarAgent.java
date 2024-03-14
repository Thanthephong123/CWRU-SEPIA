package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    class MapLocation
    {
        public int x, y;
        public float cost;
        public MapLocation cameFrom;
        public float heuristic = 0;
        public float functionValue = 0;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }
        // Method to update heuristic cost and automatically update fNcost.
        public void setHeuristicCost(float heuristicCost) {
            this.heuristic = heuristicCost;
            this.functionValue = this.cost + this.heuristic;
        }
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;
    MapLocation foundGoalNode;
    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();
        
        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     * 
     * You can check the position of the enemy footman with the following code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     * 
     * There are more examples of getting the positions of objects in SEPIA in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath) {
        // Check if the current path is empty or null, which means there's no existing plan to be blocked.
        if (currentPath == null || currentPath.isEmpty()) {
            return true;
        }
    
        // Check if the enemy footman is still alive and get its location.
        if (state.getUnit(enemyFootmanID) != null) {
            int enemyX = state.getUnit(enemyFootmanID).getXPosition();
            int enemyY = state.getUnit(enemyFootmanID).getYPosition();
    
            // Iterate through the current path to see if the enemy is on it.
            for (MapLocation loc : currentPath) {
                if (loc.x == enemyX && loc.y == enemyY) {
                    // If the enemy is on our path, we may need to replan.
                    return true;
                }
            }
        }
    
        // If the enemy is not on our path, no need to replan.
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * Therefore your you need to find some possible adjacent steps which are in range 
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
        //Create Lists of MapLocations to represent the open list and closed list for A* search.
        List<MapLocation> openList = new ArrayList<MapLocation>();
        List<MapLocation> closedList = new ArrayList<MapLocation>();

        //Add the initial state to the open list.
        start.setHeuristicCost(chebyshevDistance(start, goal));
        openList.add(start);

        //A* implementation (Variable "best" here is the best node to be expanded on the open list)
        int k = 0;
        while(!openList.isEmpty()){
            float min = Float.MAX_VALUE;
            MapLocation best = new MapLocation(0, 0, null, 0);
            
            //Find the best node with smallest f(n) to expand
            for(MapLocation node : openList){
                if(node.functionValue < min){
                    min = node.functionValue;
                    best = node;
                }
            }
            //Expanding best from the open list
            openList.remove(best);
            //Add that node to the closed list
            closedList.add(best);


            //Assign coordinates/location of "best" 
            int i = best.x;
            int j = best.y;

            // Define possible directions
            int[] directions = {-1, 0, 1};

            // Expanding neighboring nodes in every direction
            for (int di : directions) {
                for (int dj : directions) {
                    if (di == 0 && dj == 0) {
                        continue; // Skip the current node itself
                    }
                    openList = expandHelper(openList, closedList, i + di, j + dj, xExtent, yExtent, best, goal, resourceLocations, enemyFootmanLoc);
                    if (openList == null) { //If openList is null before while loop terminates, we have found a goal
                        if (foundGoalNode == null) {
                            System.err.println("There is no path");
                            return null;
                        } else {
                            return stackConstructor(foundGoalNode); // Return the path to the goal as a stack
                        }
                    }
                }
            }
        }

        // While loop terminates, meaning that there is no path
        System.err.println("There is no path");
        return null;
    }

    // Helper function to calculate the heuristic function using Chebyshev distance
    private float chebyshevDistance(MapLocation from, MapLocation to) {
        return Math.max(Math.abs(from.x - to.x), Math.abs(from.y - to.y));
    }

    private List<MapLocation> expandHelper(List<MapLocation> openList, List<MapLocation> closedList, int x, int y, int xExtent, int yExtent, MapLocation best, MapLocation goal, Set<MapLocation> resourceLocations, MapLocation enemyFootmanLoc){
        if(isValid(x, y, xExtent, yExtent, resourceLocations, enemyFootmanLoc)){
            MapLocation node = new MapLocation(x, y, best, best.cost + 1);
            //If the node is the goal, we are done
            if(x == goal.x && y == goal.y){ 
                foundGoalNode = node;
                return null;
            }

            //If the node is not in the closed list, proceed
            else if(checkContains(closedList, x, y) == null){
                //Calculate Heuristic cost of the new node
                node.setHeuristicCost(chebyshevDistance(node, goal));
                MapLocation contained = checkContains(openList, x, y);
                if(contained == null){
                    openList.add(node);
                }
                else {
                    if(contained.functionValue > node.functionValue){
                        openList.remove(contained);
                        openList.add(node);
                    }
                }
            }
        }
        return openList;
    }

    private MapLocation checkContains(List<MapLocation> list, int x, int y){
        for(MapLocation node : list){
            if(node.x == x && node.y == y){
                return node;
            }
        }
        return null;
    }

    private boolean isValid(int x, int y, int xExtent, int yExtent, Set<MapLocation> trees, MapLocation enemyFootmanLoc){
        boolean isTree = false;
        boolean isEnemy = false;

        for(MapLocation tree : trees){
            if(tree.x == x && tree.y == y){
                isTree = true;
            }
        }

        // Check if the location is the enemy footman's location
        if (enemyFootmanLoc != null && enemyFootmanLoc.x == x && enemyFootmanLoc.y == y) {
            isEnemy = true;
        }

        return x >= 0 && x < xExtent && y >= 0 && y < yExtent && !isTree && !isEnemy;
    }

    //Back-tracking algorithm, add nodes into stack
    private Stack<MapLocation> stackConstructor(MapLocation goalNode) {
        Stack<MapLocation> pathToGoal = new Stack<MapLocation>();
        MapLocation currentNode = goalNode;

        while (currentNode.cameFrom != null) {  
            currentNode = currentNode.cameFrom;
            pathToGoal.push(currentNode);
        }
        return pathToGoal;
    }

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}