package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The GameState class encapsulates all relevant game data necessary for making decisions in a Minimax algorithm.
 * This includes data such as unit positions, health, and actions available.
 */
public class GameState {

    // Maps unit IDs to SubUnit objects representing player's units
    private Map<Integer, SubUnit> my_units; 
    // Maps unit IDs to SubUnit objects representing enemy's units
    private Map<Integer, SubUnit> enemy_units;
    // List of player's unit IDs
    private List<Integer> my_unit_ID;
    // List of enemy's unit IDs
    private List<Integer> enemy_unit_ID;
    // Dimensions of the game map
    private final int xExtent, yExtent;
    // Set of obstacle locations on the map
    private Set<MapLoc> obstacles;
    // Flag indicating if it's the player's turn
    boolean isTurn;
    // Stores paths for units to follow
    private Map<Integer, List<MapLoc>> paths;
    // Current depth in the game tree
    private int depth;

     /**
     * Represents a location on the map with additional metadata such as cost and parent location for pathfinding.
     */
    class MapLoc {
        // Coordinates of the location
        int x, y;
        // Parent location in the path
        MapLoc parent;
        // Cost of moving to this location
        float cost;

        /**
         * Constructs a new MapLoc instance.
         *
         * @param x       the x-coordinate of the location
         * @param y       the y-coordinate of the location
         * @param from    the parent location
         * @param cost    the cost associated with the location
         */
        public MapLoc(int x, int y, MapLoc from, float cost) {
            this.x = x;
            this.y = y;
            this.parent = from;
            this.cost = cost;
        }
        
        /**
         * Constructs a new MapLoc instance with default parent and cost.
         *
         * @param x   the x-coordinate of the location
         * @param y   the y-coordinate of the location
         */
        public MapLoc(int x, int y) {
            this.x = x;
            this.y = y;
            this.parent = null;
            this.cost = 0;
        }

        /**
         * Constructs a new MapLoc instance as a copy of another.
         *
         * @param prev the previous MapLoc to copy
         */
        public MapLoc(MapLoc prev) {
            this.x = prev.x;
            this.y = prev.y;
            this.cost = prev.cost;
            this.parent = prev.parent;
        }

        /**
         * Determines if another location is adjacent to this one.
         *
         * @param location the location to compare
         * @return true if the location is adjacent, false otherwise
         */
        public boolean adjacent(MapLoc location) {
            return Math.abs(x - location.x) <= 1 && Math.abs(y- location.y) <= 1;
        }

        /**
         * Computes the Euclidean distance to another location.
         *
         * @param location the other location
         * @return the Euclidean distance
         */
        public int euclidean(MapLoc location) {
            return Math.abs(x - location.x) + Math.abs(y - location.y);
        }

        /**
         * Computes the Chebyshev distance to another location.
         *
         * @param location the other location
         * @return the Chebyshev distance
         */
        public int chebyshev(MapLoc location) {
            return Math.max(Math.abs(x - location.x), Math.abs(y - location.y));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof MapLoc)) return false;
            MapLoc loc = (MapLoc) obj;
            return this.x == loc.x && this.y == loc.y;
        }

        public int hashCode() {
            return (x + y) * (x + y + 1)/2 + y;
        }

        public String toString() {
            return "(" + x + "," + y + ")";
        }
    } 

    /**
     * SubUnit represents the data of a single unit within the game including position, health, and attack strength.
     */
    class SubUnit {
        // Position of the unit
        MapLoc position;
        // ID of the unit
        int id;
        // Health points of the unit
        int hp;
        // Attack damage of the unit
        int attack;

        /**
         * Constructs a SubUnit from a unit view.
         *
         * @param unit the unit view to base the SubUnit on
         */
        public SubUnit(UnitView unit) {
            position = new MapLoc(unit.getXPosition(), unit.getYPosition());
            id = unit.getID();
            hp = unit.getHP();
            attack = unit.getTemplateView().getBasicAttack();
        }

        /**
         * Constructs a SubUnit by copying another SubUnit.
         *
         * @param prev the previous SubUnit to copy
         */
        public SubUnit(SubUnit prev) {
            this.id = prev.id;
            this.hp = prev.hp;
            this.attack = prev.attack;
            this.position = new MapLoc(prev.position.x, prev.position.y);
        }
    }

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIds(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        isTurn = true;
        xExtent = state.getXExtent();
        yExtent = state.getYExtent();
        my_units = new HashMap<>();
        enemy_units = new HashMap<>();
        my_unit_ID = state.getUnitIds(0);
        enemy_unit_ID = state.getUnitIds(1);
        obstacles = new HashSet<>();
        depth = 0;
        paths = new HashMap<>();

        // Initialize units based on the state
        for(Integer unitID: my_unit_ID) {
            my_units.put(unitID, new SubUnit(state.getUnit(unitID)));
        }
        
        for(Integer unitID: enemy_unit_ID) {
            enemy_units.put(unitID, new SubUnit(state.getUnit(unitID)));
        }

         // Initialize obstacles based on resource nodes
        for(Integer resID: state.getAllResourceIds()) {
            ResourceView obstacle = state.getResourceNode(resID);
            obstacles.add(new MapLoc(obstacle.getXPosition(), obstacle.getYPosition()));
        }
        
        // Initialize paths using A* search algorithm for each unit
        for(SubUnit unit: my_units.values()) {
            ArrayList<MapLoc> path = new ArrayList<>(AstarSearch(unit.position, nearestArcherLocation(enemy_units, unit).position, xExtent, yExtent, obstacles));
            Collections.reverse(path);
            path.add(0, unit.position);
            paths.put(unit.id, path);
        }
    }


    /**
     * Constructs a new game state by applying a set of actions to the parent state.
     * This constructor is typically used to generate a new state in the game tree after a move has been made.
     *
     * @param parent       The previous state of the game
     * @param actionsTaken The actions taken that led to this new state
     */
    public GameState(GameState parent, Map<Integer, Action> actionsTaken) {
        this.isTurn = !parent.isTurn;
        this.xExtent = parent.xExtent;
        this.yExtent = parent.yExtent;
        this.my_units = new HashMap<Integer, SubUnit>();
        this.enemy_units = new HashMap<Integer, SubUnit>(parent.enemy_units);
        this.my_unit_ID = new ArrayList<Integer>(parent.my_unit_ID);
        this.enemy_unit_ID = new ArrayList<Integer>(parent.enemy_unit_ID);
        this.obstacles = new HashSet<>(parent.obstacles);
        this.depth = parent.depth + 1;
        this.paths = new HashMap<>();
       
        // Update unit states and paths after actions are applied
        for (Integer entry : this.my_unit_ID) {
            List<MapLoc> path = new ArrayList<>();
            for (MapLoc loc : parent.paths.get(entry)) {
                path.add(new MapLoc(loc));
            }
            this.paths.put(entry, path);
        }

       // Deep copies of unit representations
       my_units = parent.my_units.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new SubUnit(e.getValue())));
       enemy_units = parent.enemy_units.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new SubUnit(e.getValue())));
        

       // Apply actions to update the positions and states of units
        for (Action action : actionsTaken.values()) {
        int unitID = action.getUnitId();

            if(this.my_unit_ID.contains(unitID)) {
                SubUnit unit = this.my_units.get(unitID);

                if(action.getType().equals(ActionType.PRIMITIVEMOVE)) {
                    DirectedAction dir = (DirectedAction) action;
                    updatePos(dir, unit);
                }

                else if (action.getType().equals(ActionType.PRIMITIVEATTACK)) {
                    SubUnit target = this.enemy_units.get(((TargetedAction)action).getTargetId());
                    performAttack(unit, target);
                }
            }
            else {
                SubUnit unit = this.enemy_units.get(unitID);

                if(action.getType().equals(ActionType.PRIMITIVEMOVE)) {
                    DirectedAction dir = (DirectedAction) action;
                    updatePos(dir, unit);
                }

                else if (action.getType().equals(ActionType.PRIMITIVEATTACK)) {
                    SubUnit target = this.my_units.get(((TargetedAction)action).getTargetId());

                    performAttack(unit, target);
                }
            }
        }
    }

    /**
     * Updates the position of a unit based on the direction of a move action.
     *
     * @param dir  The direction in which the unit moves
     * @param unit The unit that is moving
     */
    private void updatePos(DirectedAction dir, SubUnit unit) {
        unit.position.x = unit.position.x + dir.getDirection().xComponent();
        unit.position.y = unit.position.y + dir.getDirection().yComponent();
    }

    /**
     * Executes an attack from one unit to another, updating the health of the target unit.
     * Removes the unit from the game state if its health drops to zero.
     *
     * @param attacker The unit performing the attack
     * @param target   The unit being attacked
     */
    private void performAttack(SubUnit attacker, SubUnit target) {
        if (target == null) return ;
        target.hp = Math.max(target.hp - attacker.attack, 0);
        if (target.hp == 0) {
            if (my_unit_ID.contains(target.id)) {
                my_unit_ID.remove(my_unit_ID.indexOf(target.id));
                my_unit_ID.remove(target.id);
            }
            else {
                enemy_unit_ID.remove(enemy_unit_ID.indexOf(target.id));
                enemy_units.remove(target.id);
            }
        }
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        int player_HP = 0;
        int num_player_units = 0;
        int enemy_HP = 0;
        int num_enemy_units = 0;
        double validArcherMoves = 0;
        double averageDist = 0;

         // Calculate total valid archer moves
        for(SubUnit unit : enemy_units.values()){
            validArcherMoves += validAttacks(unit);
        }
        validArcherMoves = (enemy_unit_ID.size() > 0) ? validArcherMoves / enemy_unit_ID.size() : 0.0;

        // Accumulate health and count of player units
        for(SubUnit unit : my_units.values()){
            player_HP += unit.hp;
            num_player_units++;
        } 

        // Accumulate health and count of enemy units
        for(SubUnit unit : enemy_units.values()){
            enemy_HP += unit.hp;
            num_enemy_units++;
        }

        // Calculate the average distance to the target for each unit
        for(SubUnit unit : my_units.values()){
            MapLoc curPos = unit.position;
            if (paths.get(unit.id) != null){
                MapLoc targetPos = paths.get(unit.id).get(Math.min(depth, paths.get(unit.id).size()-1));
                averageDist += curPos.euclidean(targetPos);
            }
        }
        averageDist = (my_unit_ID.size() != 0)? averageDist / my_unit_ID.size() : 0;

        // Check if all enemy units are defeated
        if(enemy_HP == 0 || num_enemy_units == 0){
            return Integer.MAX_VALUE;
        }
        
        // Calculate utility based on the factors above, using weighted coefficients
        return -10 * averageDist + -20 * enemy_HP + -20 * num_enemy_units + 3 * player_HP + 3 * num_player_units + -7 * validArcherMoves; 
    }

    /* Finds the nearest archer to a specified unit by calculating the Euclidean distance between the specified unit
    * and each archer in a given set of units. This method is used to determine strategic positioning and targeting
    * within the game, helping to decide movement or attack strategies based on proximity to enemy archers.
    *
    * @param units A map of unit IDs to SubUnit objects representing all potential target archers.
    * @param from  The SubUnit (typically a footman) from which the nearest archer is to be found.
    * @return The SubUnit representing the nearest archer. Returns null if no archers are present or if the units map is empty.
    */
    private SubUnit nearestArcherLocation(Map<Integer, SubUnit> units, SubUnit from) {
        int nearestArcherDistance = Integer.MAX_VALUE;
        SubUnit location = null;

        for (SubUnit archer : units.values()) {
            if (from.position.euclidean(archer.position) < nearestArcherDistance) {
                nearestArcherDistance = from.position.euclidean(archer.position);
                location = archer;
            }
        }
        //Return the distance from footman to the nearest archer
        return location;
    }
    
    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        //List of children to be returned
        List<GameStateChild> result = new ArrayList<>();
        //List of action mappings to transform the current GameState
        List<Map<Integer, Action>> validActions = new ArrayList<>();

        if (isTurn) {
            validActions = createActionMaps(new ArrayList<Integer>(my_unit_ID), new ArrayList<Map<Integer, Action>>());
        } else {
            validActions = createActionMaps(new ArrayList<Integer>(enemy_unit_ID), new ArrayList<Map<Integer, Action>>());
        }

        for(Map<Integer, Action> actionMap: validActions) {
            if (!movesConflict(actionMap)) {
                result.add(new GameStateChild(actionMap, new GameState(this, actionMap)));
            }
        }

        return result;
    }

    /**
     * Recursively generates a list of all possible action maps for the current GameState.
     * Each action map represents a possible set of actions for all units in the list.
     *
     * @param units The list of unit IDs that need actions to be generated.
     * @param actionMaps Accumulates the list of possible action maps.
     * @return A list of possible action maps where each map corresponds to a possible set of actions for all units.
     */
    private List<Map<Integer, Action>> createActionMaps(List<Integer> units, List<Map<Integer, Action>> actionMaps){
        if(units.size() == 0){ // If we have applied all of the actions of all units.
            return actionMaps;
        }

        // Get the first unit ID from the list
        int unitId = units.remove(0);
        if (actionMaps.size() == 0){ // If we are generating the first set of actions.
            Map<Integer, Action> actionMap = new HashMap<>();
            generateValidActions(actionMaps, actionMap, unitId, isTurn);
            
            return createActionMaps(units, actionMaps);
        }

        // If action maps already exist, generate valid actions for each existing action map
        else{
            int originalSize = actionMaps.size(); // If we have generated the actions for some units, but not all.
            // We take every current action mapping and generate all possible new mappings with the new unit.
            for(int i = 0; i < originalSize; i++){
                Map<Integer, Action> actionMap = actionMaps.remove(0);
                generateValidActions(actionMaps, actionMap, unitId, isTurn);
            }
            return createActionMaps(units, actionMaps);
        }
        
    }

    /**
     * Generates all valid actions for a given unit and adds them to the list of action maps.
     *
     * @param maps The list of action maps to which new action maps will be added.
     * @param actions The current action map to be updated.
     * @param unitId The ID of the unit for which actions are to be generated.
     * @param isTurn Boolean indicating if it's the current unit's turn.
     */
    private void generateValidActions(List<Map<Integer, Action>> maps, Map<Integer, Action> actions, int unitId, boolean isTurn){
        SubUnit unit = (my_unit_ID.contains(unitId)) ? my_units.get(unitId) : enemy_units.get(unitId);
        Collection<SubUnit> opposingUnits = (my_unit_ID.contains(unitId)) ? enemy_units.values() : my_units.values();

        // Generate all valid movements and attack actions
        Map<Integer, Action> temp = new HashMap<>(actions);
        if (unit.position.y > 0){
            MapLoc newLoc = new MapLoc(unit.position.x, unit.position.y-1);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                temp.put(unitId, Action.createPrimitiveMove(unitId, Direction.NORTH));
                maps.add(temp);
            }
        }
        if (unit.position.x < xExtent){
            MapLoc newLoc = new MapLoc(unit.position.x + 1, unit.position.y);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                temp = new HashMap<>(actions);
                temp.put(unitId, Action.createPrimitiveMove(unitId, Direction.EAST));
                maps.add(temp);
            }
        }
        if (unit.position.y < yExtent){
            MapLoc newLoc = new MapLoc(unit.position.x, unit.position.y + 1);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                temp = new HashMap<>(actions);
                temp.put(unitId, Action.createPrimitiveMove(unitId, Direction.SOUTH));
                maps.add(temp);
            }
        }
        if (unit.position.x > 0){
            MapLoc newLoc = new MapLoc(unit.position.x - 1, unit.position.y);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                temp = new HashMap<>(actions);
                temp.put(unitId, Action.createPrimitiveMove(unitId, Direction.WEST));
                maps.add(temp);
            }
        }

        // Generate attack actions if it is the unit's turn
        if(isTurn){
            MapLoc footmanLoc = my_units.get(unitId).position;
            for(SubUnit enemyUnit : enemy_units.values()){
                MapLoc enemyLoc = enemyUnit.position;
                if (footmanLoc.adjacent(enemyLoc)){
                    temp = new HashMap<>(actions);
                    temp.put(unitId, Action.createPrimitiveAttack(unitId, enemyUnit.id));
                    maps.add(temp);
                }
            }
            
        }
        if(!isTurn){
            for(SubUnit target: my_units.values()){
                int dist = target.position.euclidean(enemy_units.get(unitId).position);
                if(dist < 10 && dist >= 4){
                    temp = new HashMap<>(actions);
                    temp.put(unitId, Action.createPrimitiveAttack(unitId, target.id));
                    maps.add(temp);
                }
            }
        }
    }

    /**
     * A* search algorithm to find the shortest path from a start location to a goal location avoiding obstacles.
     *
     * @param start The starting location.
     * @param goal The goal location.
     * @param xExtent The maximum x-coordinate of the map.
     * @param yExtent The maximum y-coordinate of the map.
     * @param resourceLocations Locations of obstacles.
     * @return A stack representing the path from start to goal.
     */
    private Stack<MapLoc> AstarSearch(MapLoc start, MapLoc goal, int xExtent, int yExtent, Set<MapLoc> resourceLocations) {

        // Create a custom comparator that compares costs of states
        Comparator<MapLoc> comparator = new StateComparator();
        // Initialize a priority queue to store MapLocations, ordered by lowest cost using the custom comparator
        PriorityQueue<MapLoc> openList = new PriorityQueue<MapLoc>(comparator);
        // Initialize a hash map to store visited MapLocations, mapping a unique integer reference to each location using the Cantor function
        // Allows constant-time lookup
        HashMap<Integer, MapLoc> closedList = new HashMap<Integer, MapLoc>();

        // Add initial states to the openList
        openList.add(start);

        // While there are states still in the open list...
        while(!openList.isEmpty()){
            // Retrieve and remove the states with the lowest current cost + heuristic cost from the openList
            MapLoc state = openList.poll();

            // Check if the current location is the goal location
            if (state.x == goal.x && state.y == goal.y){

                // If true, we trace the path from the goal state back to the start and return it to user as a stack
                return findPath(state);
            }
            else{
                
                // Add the current state to the closedList
                closedList.put(cantor(state.x, state.y), state);

                // Expand and visit neighbors
                for(int i = -1; i <= 1; i++){
                    for(int j = -1; j <= 1; j++){
                        if((i == 0 || j == 0) && !(i == 0 && j == 0)){
                            float cost = state.cost + 1 + Math.max(Math.abs(goal.x - state.x), Math.abs(goal.y - state.y));

                            MapLoc child = new MapLoc(state.x + i, state.y + j, state, cost);
                            // If we have already visited this state...
                            if(closedList.containsKey(cantor(state.x+i,state.y+j))){
                                // Update its cost if we have found one that is lower
                                float curCost = closedList.get(cantor(state.x+i,state.y+j)).cost;
                                if (cost < curCost){
                                    closedList.get(cantor(state.x+i,state.y+j)).cost = cost;
                                }
                            }
                            else if (!resourceLocations.contains(child)
                            && state.x+i < xExtent && state.x+i >= 0 
                            && state.y+j < yExtent && state.y+j >=0){
                                openList.add(new MapLoc(state.x+i, state.y+j, state, cost));
                            }
                        }
                        
                    }
                }
            }
        }
        return new Stack<MapLoc>();
    }

    /**
     * Checks for movement conflicts in a given action map.
     *
     * @param moves The map of actions to check for conflicts.
     * @return true if there are any conflicts, false otherwise.
     */
    private boolean movesConflict(Map<Integer, Action> moves){
        Set<MapLoc> locs = new HashSet<>();
        for(Action a : moves.values()){
            if(a.getType() == ActionType.PRIMITIVEMOVE ){
                Direction dir = ((DirectedAction)a).getDirection();
                MapLoc resultingLoc;
                if (my_unit_ID.contains(a.getUnitId())){
                    resultingLoc = new MapLoc(my_units.get(a.getUnitId()).position.x + dir.xComponent(), my_units.get(a.getUnitId()).position.y + dir.yComponent());
                }
                else{
                    resultingLoc = new MapLoc(enemy_units.get(a.getUnitId()).position.x + dir.xComponent(), enemy_units.get(a.getUnitId()).position.y + dir.yComponent());
                }
                if (!locs.add(resultingLoc) || isTaken(resultingLoc)){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if a location is currently occupied by any unit.
     *
     * @param loc The location to check.
     * @return true if the location is occupied, false otherwise.
     */
    private boolean isTaken(MapLoc loc){
        for(SubUnit unit : my_units.values()){
            if(unit.position.equals(loc))
                return true;
        }
        for(SubUnit unit : enemy_units.values()){
            if(unit.position.equals(loc))
                return true;
        }

        return false;
    }


    /**
     * Counts the number of valid attack positions available for a given unit.
     * This method checks each of the four cardinal directions around the unit's current position
     * to determine if moving in that direction would be unobstructed and not currently occupied by an obstacle or another unit.
     *
     * @param unit The unit for which to count valid attack positions.
     * @return The count of valid attack positions available for the specified unit.
     */
    private int validAttacks(SubUnit unit) {
        Collection<SubUnit> opposingUnits = (my_unit_ID.contains(unit.id)) ? enemy_units.values() : my_units.values();
        int ret = 0;
        if (unit.position.y > 0){
            MapLoc newLoc = new MapLoc(unit.position.x, unit.position.y-1);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                ret++;
            }
        }
        if (unit.position.x < xExtent){
            MapLoc newLoc = new MapLoc(unit.position.x + 1, unit.position.y);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                ret++;
            }
        }
        if (unit.position.y < yExtent){
            MapLoc newLoc = new MapLoc(unit.position.x, unit.position.y + 1);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                ret++;
            }
        }
        if (unit.position.x > 0){
            MapLoc newLoc = new MapLoc(unit.position.x - 1, unit.position.y);
            boolean blocked = false;
            for(SubUnit opposingUnit : opposingUnits){
                if(newLoc.equals(opposingUnit.position))
                    blocked = true;
            }
            if(!obstacles.contains(newLoc) && !blocked){
                ret++;
            }
        }

        return ret;
    }

    /**
     * Constructs a path from the goal location to the start location by tracing parent links.
     * This method is used after the A* search algorithm finds a path to the goal,
     * allowing the path to be retraced from the goal to the start.
     *
     * @param goal The goal location from which to trace back to the start.
     * @return A stack of MapLoc objects representing the path from the start to the goal. The stack is returned in the order
     *         from the start to the goal for easy traversal.
     */
    private Stack<MapLoc> findPath(MapLoc goal){
        // Initializes a stack to store path
        Stack<MapLoc> path = new Stack<MapLoc>();
        MapLoc curState = goal.parent;
        while(curState.parent !=  null){
            path.add(curState);
            curState = curState.parent;
        }
        return path;
    }

    /**
     * A Comparator class to compare MapLocations from cost
     */
    private class StateComparator implements Comparator<MapLoc>{
        // This compares MapLocations using their costs
        public int compare(MapLoc x, MapLoc y){
            return Float.compare(x.cost, y.cost);
        }
    }

    /**
     * Implementation of the Cantor Pairing Function
     * See: https://en.wikipedia.org/wiki/Pairing_function
     * @param (x,y)/loc State coordinates, or the state itself
     * @return An single integer that uniquely identifies a two tuple integer.
     */
    private int cantor(int x, int y){
        return (x + y) * ( x + y + 1)/2 + y;
    }
}

