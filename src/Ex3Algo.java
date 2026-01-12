import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the major algorithmic class for Ex3 - the PacMan game:
 *
 * FSM-based Pacman algorithm that maximizes score while minimizing death risk.
 * Uses BFS for distance calculations and a cost function for target selection.
 */
public class Ex3Algo implements PacManAlgo {
	private int _count;
	
	// FSM States
	private enum State {
		CHASE,      // Eat vulnerable ghosts
		ESCAPE,     // Survive immediate danger
		GET_POWER_PELLET,  // Turn danger into opportunity
		EAT_DOTS    // Score efficiently
	}
	
	// Constants
	private static final int DANGER_THRESHOLD = 3;  // Distance threshold for immediate danger
	private static final int CHASE_THRESHOLD = 10;  // Max distance to chase vulnerable ghosts
	private static final double DANGER_WEIGHT = 2.0;  // Weight for danger in cost function
	private static final double REWARD_WEIGHT = 5.0;  // Weight for reward in cost function
	private static final int POWER_PELLET_REWARD = 50;  // Reward value for power pellets
	private static final int GHOST_REWARD = 200;  // Reward value for eating ghosts
	private static final int DOT_REWARD = 1;  // Reward value for eating dots
	
	public Ex3Algo() {
		_count = 0;
	}
	
	@Override
	/**
	 * Add a short description for the algorithm as a String.
	 */
	public String getInfo() {
		return "FSM-based Pacman algorithm using BFS distance calculations and cost-based target selection";
	}
	
	@Override
	/**
	 * This is the main method - that you should design, implement and test.
	 */
	public int move(PacmanGame game) {
		int code = 0;
		int[][] board = game.getGame(0);
		Pixel2D pacmanPos = parsePosition(game.getPos(code).toString());
		GhostCL[] ghosts = game.getGhosts(code);
		
		// Get color codes
		int blue = Game.getIntColor(Color.BLUE, code);
		int pink = Game.getIntColor(Color.PINK, code);
		int black = Game.getIntColor(Color.BLACK, code);
		int green = Game.getIntColor(Color.GREEN, code);
		
		// Create map for pathfinding
		Map map = new Map(board);
		map.setCyclic(GameInfo.CYCLIC_MODE);
		
		// Determine obstacle color (walls) - typically green
		int obstacleColor = green;
		
		// Determine state
		State currentState = determineState(pacmanPos, ghosts, map, obstacleColor);
		
		// Execute state behavior
		int direction = executeState(currentState, pacmanPos, ghosts, board, map, 
		                             obstacleColor, blue, pink, black, green, code);
		
		_count++;
		return direction;
	}
	
	/**
	 * Determines the current state based on game situation.
	 */
	private State determineState(Pixel2D pacmanPos, GhostCL[] ghosts, Map map, int obstacleColor) {
		// Check for vulnerable ghosts (highest priority)
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Map2D distances = map.allDistance(pacmanPos, obstacleColor);
				int dist = distances.getPixel(getGhostPosition(ghost));
				if (dist != -1 && dist <= CHASE_THRESHOLD) {
					return State.CHASE;
				}
			}
		}
		
		// Check for immediate danger (escape priority)
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				int dist = pacmanDistances.getPixel(getGhostPosition(ghost));
				if (dist != -1 && dist <= DANGER_THRESHOLD) {
					return State.ESCAPE;
				}
			}
		}
		
		// Check if danger is near but manageable (get power pellet)
		boolean dangerNear = false;
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				int dist = pacmanDistances.getPixel(getGhostPosition(ghost));
				if (dist != -1 && dist <= DANGER_THRESHOLD + 3) {
					dangerNear = true;
					break;
				}
			}
		}
		if (dangerNear) {
			return State.GET_POWER_PELLET;
		}
		
		// Default: eat dots
		return State.EAT_DOTS;
	}
	
	/**
	 * Executes the behavior for the current state.
	 */
	private int executeState(State state, Pixel2D pacmanPos, GhostCL[] ghosts, 
	                        int[][] board, Map map, int obstacleColor,
	                        int blue, int pink, int black, int green, int code) {
		switch (state) {
			case CHASE:
				return chaseVulnerableGhosts(pacmanPos, ghosts, map, obstacleColor);
			case ESCAPE:
				return escapeFromGhosts(pacmanPos, ghosts, map, obstacleColor);
			case GET_POWER_PELLET:
				return getPowerPellet(pacmanPos, ghosts, board, map, obstacleColor, blue, pink);
			case EAT_DOTS:
				return eatDots(pacmanPos, ghosts, board, map, obstacleColor, blue, pink, black);
			default:
				return Game.UP;
		}
	}
	
	/**
	 * CHASE state: Find and chase the closest vulnerable ghost.
	 */
	private int chaseVulnerableGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, 
	                                  Map map, int obstacleColor) {
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		GhostCL bestGhost = null;
		double bestCost = Double.MAX_VALUE;
		
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int distance = pacmanDistances.getPixel(ghostPos);
				if (distance != -1 && distance <= CHASE_THRESHOLD) {
					// Cost = distance - reward (lower is better)
					double cost = distance - REWARD_WEIGHT * GHOST_REWARD;
					if (cost < bestCost) {
						bestCost = cost;
						bestGhost = ghost;
					}
				}
			}
		}
		
		if (bestGhost != null) {
			return moveTowardsTarget(pacmanPos, getGhostPosition(bestGhost), map, obstacleColor);
		}
		
		// Fallback: move randomly if no target found
		return getRandomValidDirection(pacmanPos, map, obstacleColor);
	}
	
	/**
	 * ESCAPE state: Choose neighbor that maximizes minimum distance to ghosts.
	 */
	private int escapeFromGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, 
	                             Map map, int obstacleColor) {
		// Get all neighbors
		List<Pixel2D> neighbors = getNeighbors(pacmanPos, map);
		
		int bestDir = Game.UP;
		int bestMinDist = -1;
		
		for (Pixel2D neighbor : neighbors) {
			if (map.getPixel(neighbor) == obstacleColor) {
				continue;  // Skip walls
			}
			
			// Calculate minimum distance to any non-vulnerable ghost from this neighbor
			Map2D neighborDistances = map.allDistance(neighbor, obstacleColor);
			int minDist = Integer.MAX_VALUE;
			
			for (GhostCL ghost : ghosts) {
				if (!isVulnerable(ghost)) {
					int dist = neighborDistances.getPixel(getGhostPosition(ghost));
					if (dist != -1 && dist < minDist) {
						minDist = dist;
					}
				}
			}
			
			// Choose neighbor with maximum minimum distance (safest)
			if (minDist > bestMinDist) {
				bestMinDist = minDist;
				bestDir = getDirection(pacmanPos, neighbor);
			}
		}
		
		return bestDir;
	}
	
	/**
	 * GET_POWER_PELLET state: Find and move towards the closest power pellet.
	 */
	private int getPowerPellet(Pixel2D pacmanPos, GhostCL[] ghosts, int[][] board, Map map, 
	                          int obstacleColor, int blue, int pink) {
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		Pixel2D bestPellet = null;
		double bestCost = Double.MAX_VALUE;
		
		// Power pellets are typically larger dots (pink or a different color)
		// We'll search for pink cells as power pellets
		for (int x = 0; x < board.length; x++) {
			for (int y = 0; y < board[0].length; y++) {
				int cellValue = board[x][y];
				if (cellValue == pink) {  // Assuming pink is power pellet
					Pixel2D pelletPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(pelletPos);
					if (distance != -1) {
						// Calculate danger at this position
						double danger = calculateDanger(pelletPos, ghosts, map, obstacleColor);
						// Cost = distance + danger - reward
						double cost = distance + DANGER_WEIGHT * danger - REWARD_WEIGHT * POWER_PELLET_REWARD;
						if (cost < bestCost) {
							bestCost = cost;
							bestPellet = pelletPos;
						}
					}
				}
			}
		}
		
		if (bestPellet != null) {
			return moveTowardsTarget(pacmanPos, bestPellet, map, obstacleColor);
		}
		
		// Fallback: eat dots if no power pellet found
		return eatDots(pacmanPos, ghosts, board, map, obstacleColor, blue, pink, 
		               Game.getIntColor(Color.BLACK, 0));
	}
	
	/**
	 * EAT_DOTS state: Find and move towards the best dot using cost function.
	 */
	private int eatDots(Pixel2D pacmanPos, GhostCL[] ghosts, int[][] board, Map map, 
	                   int obstacleColor, int blue, int pink, int black) {
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		Pixel2D bestDot = null;
		double bestCost = Double.MAX_VALUE;
		
		// Search for dots (typically blue cells)
		for (int x = 0; x < board.length; x++) {
			for (int y = 0; y < board[0].length; y++) {
				int cellValue = board[x][y];
				// Dots are typically blue, or any non-wall, non-black cell
				if (cellValue == blue || (cellValue != black && cellValue != obstacleColor)) {
					Pixel2D dotPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(dotPos);
					if (distance != -1) {
						// Calculate danger at this position
						double danger = calculateDanger(dotPos, ghosts, map, obstacleColor);
						// Cost = distance + danger - reward
						double cost = distance + DANGER_WEIGHT * danger - REWARD_WEIGHT * DOT_REWARD;
						if (cost < bestCost) {
							bestCost = cost;
							bestDot = dotPos;
						}
					}
				}
			}
		}
		
		if (bestDot != null) {
			return moveTowardsTarget(pacmanPos, bestDot, map, obstacleColor);
		}
		
		// Fallback: move randomly if no dots found
		return getRandomValidDirection(pacmanPos, map, obstacleColor);
	}
	
	/**
	 * Calculates danger value at a position based on proximity to non-vulnerable ghosts.
	 * Closer ghosts = higher danger.
	 */
	private double calculateDanger(Pixel2D pos, GhostCL[] ghosts, Map map, int obstacleColor) {
		double totalDanger = 0.0;
		Map2D distancesFromPos = map.allDistance(pos, obstacleColor);
		
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				int dist = distancesFromPos.getPixel(getGhostPosition(ghost));
				if (dist != -1 && dist > 0) {
					// Inverse distance: closer ghosts contribute more danger
					totalDanger += 1.0 / (dist + 1);
				} else if (dist == 0) {
					// Ghost is at this position - maximum danger
					totalDanger += 100.0;
				}
			}
		}
		
		return totalDanger;
	}
	
	/**
	 * Moves towards a target position, returning the first step direction.
	 */
	private int moveTowardsTarget(Pixel2D from, Pixel2D to, Map map, int obstacleColor) {
		Pixel2D[] path = map.shortestPath(from, to, obstacleColor);
		if (path != null && path.length > 1) {
			// Return direction to first step in path
			return getDirection(from, path[1]);
		}
		// Fallback if no path found
		return getRandomValidDirection(from, map, obstacleColor);
	}
	
	/**
	 * Gets the direction from one position to another.
	 * Uses the path from Map.shortestPath to get the correct first step.
	 */
	private int getDirection(Pixel2D from, Pixel2D to) {
		// Calculate differences
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// For adjacent cells, determine direction directly
		if (dx == 1 && dy == 0) return Game.RIGHT;
		if (dx == -1 && dy == 0) return Game.LEFT;
		if (dx == 0 && dy == 1) return Game.DOWN;
		if (dx == 0 && dy == -1) return Game.UP;
		
		// For non-adjacent cells, use largest component
		if (Math.abs(dx) > Math.abs(dy)) {
			return dx > 0 ? Game.RIGHT : Game.LEFT;
		} else {
			return dy > 0 ? Game.DOWN : Game.UP;
		}
	}
	
	/**
	 * Gets all valid neighbors of a position.
	 */
	private List<Pixel2D> getNeighbors(Pixel2D pos, Map map) {
		List<Pixel2D> neighbors = new ArrayList<>();
		neighbors.add(new Index2D(pos.getX(), pos.getY() - 1));  // UP
		neighbors.add(new Index2D(pos.getX() - 1, pos.getY()));  // LEFT
		neighbors.add(new Index2D(pos.getX(), pos.getY() + 1));  // DOWN
		neighbors.add(new Index2D(pos.getX() + 1, pos.getY()));  // RIGHT
		return neighbors;
	}
	
	/**
	 * Gets a random valid direction (not into a wall).
	 */
	private int getRandomValidDirection(Pixel2D pacmanPos, Map map, int obstacleColor) {
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		List<Integer> validDirs = new ArrayList<>();
		
		for (int dir : dirs) {
			Pixel2D nextPos = getNextPosition(pacmanPos, dir);
			if (map.getPixel(nextPos) != obstacleColor && map.getPixel(nextPos) != -1) {
				validDirs.add(dir);
			}
		}
		
		if (validDirs.isEmpty()) {
			return Game.UP;  // Default fallback
		}
		
		int ind = (int)(Math.random() * validDirs.size());
		return validDirs.get(ind);
	}
	
	/**
	 * Gets the next position after moving in a direction.
	 */
	private Pixel2D getNextPosition(Pixel2D pos, int direction) {
		int x = pos.getX();
		int y = pos.getY();
		
		switch (direction) {
			case Game.UP:
				return new Index2D(x, y - 1);
			case Game.DOWN:
				return new Index2D(x, y + 1);
			case Game.LEFT:
				return new Index2D(x - 1, y);
			case Game.RIGHT:
				return new Index2D(x + 1, y);
			default:
				return new Index2D(x, y);
		}
	}
	
	/**
	 * Checks if a ghost is vulnerable (can be eaten).
	 */
	private boolean isVulnerable(GhostCL ghost) {
		// A ghost is vulnerable if it has remaining time as eatable > 0
		return ghost.remainTimeAsEatable(0) > 0;
	}
	
	/**
	 * Parses a position string "x,y" into a Pixel2D.
	 */
	private Pixel2D parsePosition(String posStr) {
		String[] parts = posStr.split(",");
		if (parts.length == 2) {
			try {
				int x = Integer.parseInt(parts[0].trim());
				int y = Integer.parseInt(parts[1].trim());
				return new Index2D(x, y);
			} catch (NumberFormatException e) {
				// Fallback to (0,0) if parsing fails
				return new Index2D(0, 0);
			}
		}
		return new Index2D(0, 0);
	}
	
	/**
	 * Gets ghost position as Pixel2D.
	 */
	private Pixel2D getGhostPosition(GhostCL ghost) {
		return parsePosition(ghost.getPos(0).toString());
	}
	
	// Debug methods (kept from original)
	private static void printBoard(int[][] b) {
		for(int y = 0; y < b[0].length; y++){
			for(int x = 0; x < b.length; x++){
				int v = b[x][y];
				System.out.print(v+"\t");
			}
			System.out.println();
		}
	}
	
	private static void printGhosts(GhostCL[] gs) {
		for(int i = 0; i < gs.length; i++){
			GhostCL g = gs[i];
			System.out.println(i+") status: "+g.getStatus()+",  type: "+g.getType()+",  pos: "+g.getPos(0)+",  time: "+g.remainTimeAsEatable(0));
		}
	}
}
