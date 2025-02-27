package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Represents a forest resource within the SEPIA game environment. This class is a specialized form of the {@link Resource} class,
 * specifically for managing wood resources. Instances of this class are used to interact with and represent forest resources in the game,
 * setting the resource type to wood by default.
 *
 * @see Resource
 */
public class Forest extends Resource{
  /**
   * Constructs a new Forest resource using a view of an existing resource node. This constructor is typically used
   * when a forest resource needs to be initialized from the state of the game environment.
   *
   * @param resource The resource view from which to initialize the forest resource.
   */
  public Forest(ResourceNode.ResourceView resource) {
    super(resource);
    type = ResourceType.WOOD;
  }

  /**
   * Constructs a new Forest resource by copying properties from another resource object. This constructor is useful
   * for duplicating an existing forest resource, ensuring that the new instance is also specifically categorized as wood.
   *
   * @param resource The resource object from which to copy properties.
   */
  public Forest(Resource resource) {
    super(resource);
    type = ResourceType.WOOD;
  }
}
