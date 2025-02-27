package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;


/**
 * Represents a generic resource in the game environment. This abstract class
 * is used to define the basic properties and functionalities that all specific
 * resource types (e.g., gold mines, forests) share.
 */
public abstract class Resource {
  // Unique identifier for the resource
  public int id;
  // The amount of resource left to be harvested
  public int amountRemaining;
  // The position of the resource on the game map
  public Position position;
  // The type of resource (e.g., GOLD, WOOD)
  public ResourceType type;

  /**
   * Constructs a Resource object using a resource view from the game environment.
   * This constructor is typically used to initialize resource objects directly from game state data.
   *
   * @param resource A resource view obtained from the game environment, providing necessary details.
   */
  public Resource(ResourceNode.ResourceView resource) {
    this.id = resource.getID();
    this.amountRemaining = resource.getAmountRemaining();
    this.position = new Position(resource.getXPosition(), resource.getYPosition());
  }

  /**
   * Constructs a Resource object by copying properties from another Resource object.
   * Useful for creating a duplicate resource with the same state.
   *
   * @param resource An existing Resource object to copy properties from.
   */ 
  public Resource(Resource resource){
    this.id = resource.id;
    this.amountRemaining = resource.amountRemaining;
    this.position = new Position(resource.position);
  }

  /**
   * Determines if this Resource object is equivalent to another object.
   *
   * @param o The object to compare this Resource against.
   * @return true if the provided object is a Resource with the same id, position, type, and amount remaining.
   */
  @Override
  public boolean equals(Object o) {
      return o instanceof Resource
              && id == ((Resource) o).id
              && position.equals(((Resource) o).position)
              && type.equals(((Resource)o).type)
              && amountRemaining == ((Resource)o).amountRemaining;
  }

  /**
   * Reduces the amount of the resource available by a specified amount, simulating the harvesting process.
   *
   * @param amount The amount to harvest from this resource.
   */
  public void harvestAmount(int amount) {
    this.amountRemaining -= amount;
  }

  /**
   * Gets the position of the resource on the game map.
   *
   * @return The position of the resource.
   */
  public Position getPosition(){
    return position;
  }

  /**
   * Returns the amount of this resource that is still available for harvesting.
   *
   * @return The amount remaining of this resource.
   */
  public int getAmountRemaining(){
    return amountRemaining;
  }

  /**
   * Retrieves the unique identifier for this resource.
   *
   * @return The unique identifier of the resource.
   */
  public int getID(){
    return id;
  }

  /**
   * Gets the type of this resource.
   *
   * @return The ResourceType of this resource.
   */
  public ResourceType getType(){
    return this.type;
  }

}
