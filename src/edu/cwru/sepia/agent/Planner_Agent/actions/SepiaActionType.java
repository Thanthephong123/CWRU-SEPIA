package edu.cwru.sepia.agent.planner.actions;

/**
 * Enumerates the types of actions that can be performed in the SEP-IA game environment.
 * Each action type corresponds to a specific set of operations that can be carried out by a unit.
 */
public enum SepiaActionType {
    // Represents the action of a unit moving from one location to another.
    MOVE,
    // Represents the action of a unit harvesting resources like wood or gold.
    HARVEST,
    // Represents the action of a unit depositing collected resources into a townhall.
    DEPOSIT,
    // Represents the action of a unit building structures or creating other units.
    BUILD
}
