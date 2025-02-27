package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class is an implementation of the Minimax algorithm combined with
 * Alpha-Beta Pruning.
 * It extends the Agent class, which allows it to make it's own decisions.
 */
public class MinimaxAlphaBeta extends Agent {
    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args) {
        super(playernum);
        if (args.length < 1) {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }
        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {

        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides,
     * assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as
     * much SEPIA specific
     * code into other functions and methods)
     *
     * @param node  The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to
     *              the root
     * @param beta  The current best value for the minimizing node from this node to
     *              the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta) {
        // Initialize best value and child variables.
        double bestVal = Double.NEGATIVE_INFINITY;
        GameStateChild bestChild = null;
        // Iterate through the children of node to find the best child.
        for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren(), true)) {
            double utility = minimax(node, depth - 1, alpha, beta);
            // If this child's utility is better than bestChild's, update bestVal and
            // bestChild.
            if (bestVal < utility) {
                bestVal = utility;
                bestChild = child;
            }
            // If this child's utility is equal to the best, compare this child and
            // bestChild based on their utility functions.
            else if (bestVal == utility) {
                if (child.state.getUtility() > bestChild.state.getUtility()) {
                    bestChild = child;
                }
            }
            // Update alpha if necessary
            alpha = Math.max(bestVal, alpha);
        }

        return bestChild;
    }

    /**
     * The method uses the minimax algorithm to calculate the utility of a given
     * node.
     * 
     * @param node  The node we are looking for the utility of
     * @param depth The current depth of minimax search
     * @param alpha The stored alpha value for minimax
     * @param beta  The stored beta value for minimax
     * @return The utility of node as determined by the minimax algorithm
     */
    private double minimax(GameStateChild node, int depth, double alpha, double beta) {
        // Base case: return the utility of this node if we are at the intended depth
        if (depth == 0)
            return node.state.getUtility();

        // MAX node
        if (node.state.isTurn) {
            // Initialize utility to negative infinity.
            double maxUtility = Double.NEGATIVE_INFINITY;
            // Iterate through the children of this node, ordered based on heuristics
            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren(), true)) {
                // Recursively call minimax on the child
                double utility = minimax(child, depth - 1, alpha, beta);
                maxUtility = Math.max(maxUtility, utility);
                alpha = Math.max(alpha, maxUtility);
                // Check if we can prune the remaining children
                if (beta <= alpha)
                    break;
            }
            return maxUtility;
        }
        // MIN node
        else {
            // Initialize utility to infinity
            double minUtility = Double.POSITIVE_INFINITY;
            // Iterate through the children of this node, ordered according to heuristics
            for (GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren(), false)) {
                // Recursively call minimax on the child
                double utility = minimax(child, depth - 1, alpha, beta);
                minUtility = Math.min(minUtility, utility);
                beta = Math.min(beta, minUtility);
                // Check if we can prune the remaining children
                if (beta <= alpha)
                    break;
            }
            return minUtility;
        }
    }

    /**
     * We order our children simply in the order of their utility value, so that the
     * most advantageous states are expanded first.
     * The intuition behind this is that a state with a high is likely to have
     * children with high utilities, so the overall heursitic of a node follows its
     * utility.
     * 
     * @param children the children which are of a parent node
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children, boolean max) {
        if (max) {
            Collections.sort(children, new MinimaxComparator().reversed());
        } else {
            Collections.sort(children, new MinimaxComparator());
        }
        return children;
    }

    /**
     * A helper class that acts as a comparator to order GameStates based on
     * heuristics.
     */
    private class MinimaxComparator implements Comparator<GameStateChild> {

        @Override
        public int compare(GameStateChild child1, GameStateChild child2) {
            // If child1's utility is less than child2's, return -1.
            if (child1.state.getUtility() < child2.state.getUtility()) {
                return -1;
            }
            // Otherwise, if it is greater, return 1.
            else if (child1.state.getUtility() > child2.state.getUtility()) {
                return 1;
            }
            // If they are equal, return 0.
            return 0;
        }
    }

}
