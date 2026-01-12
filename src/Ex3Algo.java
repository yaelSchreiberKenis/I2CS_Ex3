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
		// Note: board is indexed as board[x][y], but Map expects _map[y][x]
		// So we need to transpose or create Map correctly
		// The board dimensions: board.length = width, board[0].length = height
		// Map expects: _map[height][width] = _map[y][x]
		// So we create a transposed version
		int width = board.length;
		int height = board[0].length;
		int[][] transposedBoard = new int[height][width];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				transposedBoard[y][x] = board[x][y];
			}
		}
		Map map = new Map(transposedBoard);
		map.setCyclic(GameInfo.CYCLIC_MODE);
		
		// Determine obstacle color (walls)
		// Check what color is at Pacman's position - that's definitely not a wall
		int pacmanCellValue = map.getPixel(pacmanPos);
		
		// Walls are typically green, but let's be more robust
		// Try green first, but also check if black might be walls
		int obstacleColor = green;
		
		// If green is the same as pacman's position, try black
		if (green == pacmanCellValue) {
			obstacleColor = black;
		}
		
		// Verify obstacle color by checking neighbors - if all neighbors are the same color as pacman,
		// then that color is not the obstacle
		List<Pixel2D> neighbors = getValidNeighbors(pacmanPos, map, -1); // Get all neighbors regardless of obstacle
		boolean greenIsObstacle = false;
		boolean blackIsObstacle = false;
		for (Pixel2D neighbor : neighbors) {
			int neighborValue = map.getPixel(neighbor);
			if (neighborValue == green && neighborValue != pacmanCellValue) {
				greenIsObstacle = true;
			}
			if (neighborValue == black && neighborValue != pacmanCellValue) {
				blackIsObstacle = true;
			}
		}
		
		// Use the color that appears as obstacles around pacman
		if (greenIsObstacle) {
			obstacleColor = green;
		} else if (blackIsObstacle) {
			obstacleColor = black;
		}
		
		// Determine state
		State currentState = determineState(pacmanPos, ghosts, map, obstacleColor);
		
		// Debug output
		if (_count < 10 || _count % 50 == 0) {
			System.out.println("Move " + _count + ": Pacman at " + pacmanPos + 
			                   ", State: " + currentState + 
			                   ", Obstacle color: " + obstacleColor +
			                   ", Cell value: " + map.getPixel(pacmanPos));
			List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
			System.out.println("  Valid neighbors: " + validNeighbors.size());
			for (Pixel2D n : validNeighbors) {
				System.out.println("    " + n + " = " + map.getPixel(n));
			}
		}
		
		// Execute state behavior
		int direction = executeState(currentState, pacmanPos, ghosts, board, map, 
		                             obstacleColor, blue, pink, black, green, code);
		
		// Debug direction
		if (_count < 10 || _count % 50 == 0) {
			String dirName = direction == Game.UP ? "UP" : 
			                direction == Game.DOWN ? "DOWN" :
			                direction == Game.LEFT ? "LEFT" : "RIGHT";
			System.out.println("  Chosen direction: " + dirName);
		}
		
		// Safety check: ensure we have a valid direction
		// If direction is invalid, get a random valid one
		Pixel2D nextPos = getNextPosition(pacmanPos, direction, map);
		if (!isValidPosition(nextPos, map, obstacleColor)) {
			if (_count < 10) {
				System.out.println("  WARNING: Invalid direction, getting random valid");
			}
			// Direction is invalid, get a valid one
			direction = getRandomValidDirection(pacmanPos, map, obstacleColor);
			nextPos = getNextPosition(pacmanPos, direction, map);
			if (_count < 10) {
				String dirName = direction == Game.UP ? "UP" : 
				                direction == Game.DOWN ? "DOWN" :
				                direction == Game.LEFT ? "LEFT" : "RIGHT";
				System.out.println("  New direction: " + dirName + " to " + nextPos);
			}
		}
		
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
				System.out.println("NO NO shouldn't go to defaule usa where is my stateee");
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
		// Use Map's getAllNeighbors which handles boundaries and cyclic mode
		// We need to access the private method, so we'll get neighbors manually but properly
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		if (validNeighbors.isEmpty()) {
			return Game.UP;  // Fallback if no valid neighbors
		}
		
		int bestDir = Game.UP;
		int bestMinDist = -1;
		
		for (Pixel2D neighbor : validNeighbors) {
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
		// Search through map coordinates
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == pink) {  // Assuming pink is power pellet
					Pixel2D pelletPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(pelletPos);
					if (distance != -1 && distance > 0) {  // Must be reachable
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
		// First, try to find dots in immediate neighbors (faster)
		List<Pixel2D> neighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		if (neighbors.isEmpty()) {
			// No valid neighbors - this shouldn't happen
			return Game.UP;
		}
		
		Pixel2D bestDot = null;
		double bestCost = Double.MAX_VALUE;
		
		for (Pixel2D neighbor : neighbors) {
			int cellValue = map.getPixel(neighbor);
			// Any valid neighbor that's not a wall could be a dot
			if (cellValue != obstacleColor && cellValue != -1) {
				// Prefer blue cells (likely dots), but accept any non-wall cell
				double reward = (cellValue == blue) ? DOT_REWARD : DOT_REWARD * 0.5;
				double danger = calculateDanger(neighbor, ghosts, map, obstacleColor);
				double cost = 1 + DANGER_WEIGHT * danger - REWARD_WEIGHT * reward;
				if (cost < bestCost) {
					bestCost = cost;
					bestDot = neighbor;
				}
			}
		}
		
		// If no good neighbor found, search the whole map for dots
		if (bestDot == null) {
			Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
			for (int x = 0; x < map.getWidth(); x++) {
				for (int y = 0; y < map.getHeight(); y++) {
					int cellValue = map.getPixel(x, y);
					// Skip obstacles and out of bounds
					if (cellValue == obstacleColor || cellValue == -1) {
						continue;
					}
					// Check if this cell is reachable (has a valid distance)
					Pixel2D dotPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(dotPos);
					if (distance != -1 && distance > 0) {  // Must be reachable and not current position
						// Accept any non-wall cell as a potential dot
						// Prefer blue cells (likely dots)
						double reward = (cellValue == blue) ? DOT_REWARD : DOT_REWARD * 0.5;
						// Calculate danger at this position
						double danger = calculateDanger(dotPos, ghosts, map, obstacleColor);
						// Cost = distance + danger - reward
						double cost = distance + DANGER_WEIGHT * danger - REWARD_WEIGHT * reward;
						if (cost < bestCost) {
							bestCost = cost;
							bestDot = dotPos;
						}
					}
				}
			}
		}
		
		if (bestDot != null) {
			int dir = moveTowardsTarget(pacmanPos, bestDot, map, obstacleColor);
			// Verify the direction is valid
			Pixel2D testPos = getNextPosition(pacmanPos, dir, map);
			if (isValidPosition(testPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Fallback: move to any valid neighbor (should always work)
		// Just pick the first valid neighbor
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		if (!validNeighbors.isEmpty()) {
			Pixel2D firstNeighbor = validNeighbors.get(0);
			return getDirection(pacmanPos, firstNeighbor);
		}
		
		// Last resort: try each direction
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		for (int dir : dirs) {
			Pixel2D nextPos = getNextPosition(pacmanPos, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Absolute fallback
		return Game.UP;
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
		// If already at target, move to a neighbor
		if (from.equals(to)) {
			return getRandomValidDirection(from, map, obstacleColor);
		}
		
		// If target is adjacent, go directly there
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// Check if it's an adjacent cell
		if (Math.abs(dx) + Math.abs(dy) == 1) {
			int dir = getDirection(from, to);
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Use pathfinding for non-adjacent targets
		Pixel2D[] path = map.shortestPath(from, to, obstacleColor);
		if (path != null && path.length > 1) {
			// Return direction to first step in path
			Pixel2D nextStep = path[1];
			int dir = getDirection(from, nextStep);
			// Verify the move is valid
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Fallback: try to move in the general direction if pathfinding fails
		// Handle cyclic wrapping for direction calculation
		int wrappedDx = dx;
		int wrappedDy = dy;
		if (map.isCyclic()) {
			// Check if wrapping would give a shorter path
			if (Math.abs(dx) > map.getWidth() / 2) {
				wrappedDx = dx > 0 ? dx - map.getWidth() : dx + map.getWidth();
			}
			if (Math.abs(dy) > map.getHeight() / 2) {
				wrappedDy = dy > 0 ? dy - map.getHeight() : dy + map.getHeight();
			}
		}
		
		// Try to move in the direction of the target
		if (Math.abs(wrappedDx) > Math.abs(wrappedDy)) {
			int dir = wrappedDx > 0 ? Game.RIGHT : Game.LEFT;
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		} else if (wrappedDy != 0) {
			int dir = wrappedDy > 0 ? Game.UP : Game.DOWN;  // UP is y+1, DOWN is y-1
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Final fallback: random valid direction
		return getRandomValidDirection(from, map, obstacleColor);
	}
	
	/**
	 * Gets the direction from one position to another.
	 * Uses the path from Map.shortestPath to get the correct first step.
	 * Note: In Map, UP means increasing Y (y+1), DOWN means decreasing Y (y-1)
	 */
	private int getDirection(Pixel2D from, Pixel2D to) {
		// Calculate differences
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// Handle cyclic wrapping if needed
		int width = 0, height = 0;
		try {
			// Try to get dimensions from a map if available - we'll handle this differently
		} catch (Exception e) {
			// Ignore
		}
		
		// For adjacent cells, determine direction directly
		// Note: In Map coordinate system, UP is y+1, DOWN is y-1
		if (dx == 1 && dy == 0) return Game.RIGHT;
		if (dx == -1 && dy == 0) return Game.LEFT;
		if (dx == 0 && dy == 1) return Game.UP;  // UP in Map is y+1
		if (dx == 0 && dy == -1) return Game.DOWN;  // DOWN in Map is y-1
		
		// For non-adjacent cells, use largest component
		if (Math.abs(dx) > Math.abs(dy)) {
			return dx > 0 ? Game.RIGHT : Game.LEFT;
		} else {
			return dy > 0 ? Game.UP : Game.DOWN;  // Fixed: UP is y+1, DOWN is y-1
		}
	}
	
	/**
	 * Gets all valid neighbors of a position that are not walls and are within bounds.
	 * If obstacleColor is -1, returns all neighbors regardless of obstacle status.
	 * Note: In Map coordinate system, UP is y+1, DOWN is y-1
	 */
	private List<Pixel2D> getValidNeighbors(Pixel2D pos, Map map, int obstacleColor) {
		List<Pixel2D> validNeighbors = new ArrayList<>();
		
		// Try each direction and check if it's valid
		int[] dx = {0, -1, 0, 1};   // UP, LEFT, DOWN, RIGHT
		int[] dy = {1, 0, -1, 0};   // UP (y+1), LEFT, DOWN (y-1), RIGHT
		
		for (int i = 0; i < 4; i++) {
			int newX = pos.getX() + dx[i];
			int newY = pos.getY() + dy[i];
			
			// Handle cyclic mode
			if (map.isCyclic()) {
				if (newX < 0) newX = map.getWidth() - 1;
				else if (newX >= map.getWidth()) newX = 0;
				if (newY < 0) newY = map.getHeight() - 1;
				else if (newY >= map.getHeight()) newY = 0;
			} else {
				// Non-cyclic: check bounds
				if (newX < 0 || newX >= map.getWidth() || newY < 0 || newY >= map.getHeight()) {
					continue;
				}
			}
			
			Pixel2D neighbor = new Index2D(newX, newY);
			int cellValue = map.getPixel(neighbor);
			
			// Check if it's a valid move (not a wall, not out of bounds)
			// If obstacleColor is -1, we're just checking bounds
			if (cellValue != -1 && (obstacleColor == -1 || cellValue != obstacleColor)) {
				validNeighbors.add(neighbor);
			}
		}
		
		return validNeighbors;
	}
	
	/**
	 * Gets a random valid direction (not into a wall).
	 * Always returns a valid direction if any valid neighbors exist.
	 */
	private int getRandomValidDirection(Pixel2D pacmanPos, Map map, int obstacleColor) {
		// Get all valid neighbors
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		if (validNeighbors.isEmpty()) {
			// No valid moves - this shouldn't happen in a normal game
			// Try each direction manually as last resort
			int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
			for (int dir : dirs) {
				Pixel2D nextPos = getNextPosition(pacmanPos, dir, map);
				if (isValidPosition(nextPos, map, obstacleColor)) {
					return dir;
				}
			}
			// Absolute fallback - return UP (will likely be invalid but we tried)
			return Game.UP;
		}
		
		// Pick a random valid neighbor and return its direction
		int ind = (int)(Math.random() * validNeighbors.size());
		Pixel2D chosenNeighbor = validNeighbors.get(ind);
		int dir = getDirection(pacmanPos, chosenNeighbor);
		
		// Double-check the direction is valid
		Pixel2D verifyPos = getNextPosition(pacmanPos, dir, map);
		if (!isValidPosition(verifyPos, map, obstacleColor)) {
			// If somehow invalid, try the first valid neighbor
			chosenNeighbor = validNeighbors.get(0);
			dir = getDirection(pacmanPos, chosenNeighbor);
		}
		
		return dir;
	}
	
	/**
	 * Gets the next position after moving in a direction, handling cyclic mode.
	 * Note: In Map coordinate system, UP is y+1, DOWN is y-1
	 */
	private Pixel2D getNextPosition(Pixel2D pos, int direction, Map map) {
		int x = pos.getX();
		int y = pos.getY();
		
		switch (direction) {
			case Game.UP:
				y = y + 1;  // UP is y+1 in Map
				break;
			case Game.DOWN:
				y = y - 1;  // DOWN is y-1 in Map
				break;
			case Game.LEFT:
				x = x - 1;
				break;
			case Game.RIGHT:
				x = x + 1;
				break;
			default:
				return new Index2D(x, y);
		}
		
		// Handle cyclic mode
		if (map.isCyclic()) {
			if (x < 0) x = map.getWidth() - 1;
			if (x >= map.getWidth()) x = 0;
			if (y < 0) y = map.getHeight() - 1;
			if (y >= map.getHeight()) y = 0;
		}
		
		return new Index2D(x, y);
	}
	
	/**
	 * Checks if a position is valid (not a wall, within bounds or cyclic).
	 */
	private boolean isValidPosition(Pixel2D pos, Map map, int obstacleColor) {
		int cellValue = map.getPixel(pos);
		// Valid if not out of bounds (-1) and not an obstacle
		return cellValue != -1 && cellValue != obstacleColor;
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
