package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
/**
 * Represents a gold mine resource in the game, extending the generic Resource class.
 * This class specifically handles resources of type gold, setting the resource type
 */
public class GoldMine extends Resource{

/**
 * Constructs a new GoldMine object based on a view of a resource node, setting the
 * resource type to gold. This constructor is typically used when a new game state
 * is being initialized and gold mines are being populated from the game's resource views.
 *
 * @param resource A view of the resource node from the game environment, providing
 *                 necessary details like location and remaining quantity.
 */
  public GoldMine(ResourceNode.ResourceView resource) {
    super(resource);
    type = ResourceType.GOLD;
  }

/**
 * Constructs a new GoldMine object by copying the state from another resource instance.
 * This is useful for creating a new instance of GoldMine with the same properties as
 * an existing one, particularly in methods that need to clone or modify game state
 * without altering original objects.
 *
 * @param resource An existing Resource object whose properties are to be copied.
 */
  public GoldMine(Resource resource) {
    super(resource);
    type = ResourceType.GOLD;
  }
}
