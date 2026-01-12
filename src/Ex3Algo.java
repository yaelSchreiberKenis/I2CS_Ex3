import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FSM-based Pacman algorithm that maximizes score while minimizing death risk.
 * Uses BFS for distance calculations and simple heuristics for movement.
 */
public class Ex3Algo implements PacManAlgo {

	// FSM States
	private enum State {
		CHASE,      // Eat vulnerable ghosts
		ESCAPE,     // Survive immediate danger
		GET_POWER_PELLET,  // Turn danger into opportunity
		EAT_DOTS    // Score efficiently
	}
	
	// Constants
	private static final int DANGER_THRESHOLD = 3;  // Distance threshold for immediate danger
	private static final int CHASE_THRESHOLD = 2;  // Max distance to chase vulnerable ghosts
	private static final int OBSTACLE_COLOR = Game.getIntColor(Color.BLUE, 0);

	private int boardWidth;
	private int boardHeight;

	public Ex3Algo() {
	}
	
	@Override
	public String getInfo() {
		return "FSM-based Pacman algorithm using BFS distance calculations";
	}
	
	@Override
	public int move(PacmanGame game) {
		int code = 0;
		int[][] board = game.getGame(0);
		Pixel2D pacmanPos = parsePosition(game.getPos(code));
		GhostCL[] ghosts = game.getGhosts(code);
		
		// Get color codes
		// TODO calculate them when it needed, not pass them every time
		int blue = Game.getIntColor(Color.BLUE, code);
		int pink = Game.getIntColor(Color.PINK, code);
		int green = Game.getIntColor(Color.GREEN, code);

		Map map = CreateMap(board);
		boardWidth = map.getWidth();
		boardHeight = map.getHeight();
		Map2D pacmanDistances = map.allDistance(pacmanPos, OBSTACLE_COLOR);

		// Determine state
		State currentState = determineState(pacmanDistances, ghosts, map, green);
		return executeState(pacmanDistances, currentState, pacmanPos, ghosts, map, OBSTACLE_COLOR, blue, pink, green);
	}
	
	/**
	 * Determines the current state based on game situation.
	 */
	private State determineState(Map2D pacmanDistances, GhostCL[] ghosts, Map map, int green) {
		if (IsNeedToEscape(pacmanDistances, ghosts)) {return State.ESCAPE;}
		if (IsNeedToChase(pacmanDistances, ghosts)) {return State.CHASE;}
		if (IsNeedPowerPellet(pacmanDistances, ghosts, map, green)) {return State.GET_POWER_PELLET;}
		return State.EAT_DOTS;
	}
	
	/**
	 * Executes the behavior for the current state.
	 */
	private int executeState(Map2D pacmanDistances, State state, Pixel2D pacmanPos, GhostCL[] ghosts,
	                        Map map, int obstacleColor, int blue, int pink, int green) {
		return switch (state) {
			case CHASE -> chaseVulnerableGhosts(pacmanDistances, pacmanPos, ghosts, map, obstacleColor);
			case ESCAPE -> escapeFromGhosts(pacmanPos, ghosts, map, obstacleColor);
			case GET_POWER_PELLET -> getPowerPellet(pacmanDistances, pacmanPos, map, obstacleColor, green);
			case EAT_DOTS -> eatDots(pacmanPos, ghosts, map, obstacleColor, blue);
		};
	}
	
	/**
	 * CHASE state: Find and chase the closest vulnerable ghost.
	 * Avoids chasing ghosts into the spawn area (middle of map).
	 */
	private int chaseVulnerableGhosts(Map2D pacmanDistances, Pixel2D pacmanPos, GhostCL[] ghosts,
	                                  Map map, int obstacleColor) {
		GhostCL bestGhost = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int distance = pacmanDistances.getPixel(ghostPos);

				// Only chase if ghost is reachable, within threshold
				if (distance < bestDist && distance <= CHASE_THRESHOLD) {
					bestDist = distance;
					bestGhost = ghost;
				}
			}
		}

		assert bestGhost != null;
		return moveTowardsTarget(pacmanPos, getGhostPosition(bestGhost), map, obstacleColor);
	}
	
	/**
	 * ESCAPE state: Choose neighbor that maximizes minimum distance to ghosts.
	 * Checks if escape direction will trap Pacman.
	 */
	private int escapeFromGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, Map map, int obstacleColor) {
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		// Get all non-vulnerable ghosts
		List<Pixel2D> dangerousGhosts = new ArrayList<>();
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				dangerousGhosts.add(getGhostPosition(ghost));
			}
		}
		assert !dangerousGhosts.isEmpty();

		// Find the best escape direction
		int bestDir = Game.UP;
		double bestScore = Double.NEGATIVE_INFINITY;
		
		for (Pixel2D neighbor : validNeighbors) {
			// Calculate safety score for this neighbor
			Map2D neighborDistances = map.allDistance(neighbor, obstacleColor);
			
			// Find minimum distance to any ghost from this neighbor
			int minDist = Integer.MAX_VALUE;
			int sumDist = 0;
			int count = 0;
			
			for (Pixel2D ghostPos : dangerousGhosts) {
				int dist = neighborDistances.getPixel(ghostPos);
				if (dist < minDist) {
					minDist = dist;
				}
				sumDist += dist;
				count++;
			}
			
			// Check if this neighbor will lead to being trapped
			// Look ahead: check if from this neighbor, we can escape further
			boolean willBeTrapped = false;
			if (minDist <= 2) {
				// If we'll be very close to a ghost, check if we can escape further
				List<Pixel2D> futureNeighbors = getValidNeighbors(neighbor, map, obstacleColor);
				int futureMinDist = Integer.MAX_VALUE;
				for (Pixel2D futureNeighbor : futureNeighbors) {
					Map2D futureDistances = map.allDistance(futureNeighbor, obstacleColor);
					for (Pixel2D ghostPos : dangerousGhosts) {
						int dist = futureDistances.getPixel(ghostPos);
						if (dist < futureMinDist) {
							futureMinDist = dist;
						}
					}
				}
				// If future positions are also dangerous, we might be trapped
				if (futureMinDist <= 1) {
					willBeTrapped = true;
				}
			}
			
			if (willBeTrapped) {
				continue;  // Skip this direction, it leads to a trap
			}
			
			// Calculate score: prefer higher minimum distance and higher average distance
			double avgDist = sumDist / (double)count;
			double score = minDist * 2.0 + avgDist * 0.5;  // Weight minimum distance more
			
			// Prefer directions that increase distance from all ghosts
			if (score > bestScore) {
				bestScore = score;
				bestDir = getDirection(pacmanPos, neighbor);
			}
		}

		return bestDir;
	}
	
	/**
	 * GET_POWER_PELLET state: Find and move towards the closest power pellet.
	 * Only used when there are non-vulnerable ghosts or ghosts becoming non-vulnerable soon.
	 */
	private int getPowerPellet(Map2D pacmanDistances, Pixel2D pacmanPos, Map map, int obstacleColor, int green) {
		// Find the closest power pellet (reuse pacmanDistances already calculated above)
		Pixel2D bestPellet = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == green) {
					Pixel2D pelletPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(pelletPos);
					if (distance > 0 && distance < bestDist) {
						bestDist = distance;
						bestPellet = pelletPos;
					}
				}
			}
		}

		assert bestPellet != null;
		return moveTowardsTarget(pacmanPos, bestPellet, map, obstacleColor);
//		return getDirectionToTarget(pacmanPos, bestPellet, map); // TODO understand if needed
	}
	
	/**
	 * EAT_DOTS state: Find and move towards the closest dot.
	 * Properly handles cyclic movement.
	 * Also checks for nearby power pellets (pink) and prioritizes them.
	 */
	private int eatDots(Pixel2D pacmanPos, GhostCL[] ghosts, Map map, int obstacleColor, int blue) {
		// First check immediate neighbors - prioritize power pellets (pink), then dots (blue)
		List<Pixel2D> neighbors = getValidNeighbors(pacmanPos, map, obstacleColor);

		// Get pink color for power pellets
		int pink = Game.getIntColor(Color.PINK, 0);
		
		// First priority: power pellets in immediate neighbors
		for (Pixel2D neighbor : neighbors) {
			int cellValue = map.getPixel(neighbor);
			if (cellValue == pink) {
				// Found a power pellet in neighbor - go there immediately!
				// Yael: Maybe more efficient because its a neighbor
				return moveTowardsTarget(pacmanPos, neighbor, map, obstacleColor);
//				return getDirection(pacmanPos, neighbor);
			}
		}
		
		// Second priority: dots (blue) in immediate neighbors
		Pixel2D bestNeighbor = null;
		for (Pixel2D neighbor : neighbors) {
			int cellValue = map.getPixel(neighbor);
			if (cellValue == blue) {
				// TODO yael delete this
				// Found a dot in neighbor - go there immediately
				return getDirection(pacmanPos, neighbor);
			}
			// Keep track of first valid neighbor as fallback
			if (bestNeighbor == null) {
				bestNeighbor = neighbor;
			}
		}
		
		// If no dot in immediate neighbors, find closest dot using BFS
		// This will properly handle cyclic mode
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		Pixel2D bestDot = null;
		int bestDist = Integer.MAX_VALUE;
		
		// Search for dots (blue cells) or any non-obstacle, non-empty cell
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				// Look for pink (dots) or any non-obstacle cell
				if (cellValue == pink) {
					Pixel2D dotPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(dotPos);
					if (distance > 0 && distance < bestDist) {
						bestDist = distance;
						bestDot = dotPos;
					}
				}
			}
		}

		assert bestDot != null;
		// Move towards the closest dot - this will handle cyclic properly
		return moveTowardsTarget(pacmanPos, bestDot, map, obstacleColor);
	}
	
	/**
	 * Gets direction to target with proper cyclic support.
	 */
	private int getDirectionToTarget(Pixel2D from, Pixel2D to, Map map) {
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// Handle cyclic wrapping
		if (map.isCyclic()) {
			if (Math.abs(dx) > map.getWidth() / 2) {
				dx = dx > 0 ? dx - map.getWidth() : dx + map.getWidth();
			}
			if (Math.abs(dy) > map.getHeight() / 2) {
				dy = dy > 0 ? dy - map.getHeight() : dy + map.getHeight();
			}
		}
		
		// Choose direction based on largest component
		if (Math.abs(dx) > Math.abs(dy)) {
			return dx > 0 ? Game.RIGHT : Game.LEFT;
		} else if (dy != 0) {
			return dy > 0 ? Game.UP : Game.DOWN;
		}
		
		// Default
		return Game.UP;
	}
	
	/**
	 * Moves towards a target position, returning the first step direction.
	 */
	private int moveTowardsTarget(Pixel2D from, Pixel2D to, Map map, int obstacleColor) {
		if (from.equals(to)) {
			// Can't pass ghosts here, so just return a default direction
			List<Pixel2D> neighbors = getValidNeighbors(from, map, obstacleColor);
			if (!neighbors.isEmpty()) {
				return getDirection(from, neighbors.get(0));
			}
			return Game.UP;
		}
		
		// Calculate distance with cyclic wrapping support
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// Handle cyclic wrapping for direction calculation
		if (map.isCyclic()) {
			// Check if wrapping would give a shorter path
			if (Math.abs(dx) > map.getWidth() / 2) {
				dx = dx > 0 ? dx - map.getWidth() : dx + map.getWidth();
			}
			if (Math.abs(dy) > map.getHeight() / 2) {
				dy = dy > 0 ? dy - map.getHeight() : dy + map.getHeight();
			}
		}
		
		// If adjacent (after wrapping), go directly
		if (Math.abs(dx) + Math.abs(dy) == 1) {
			return getDirectionWithWrapping(from, to, dx, dy);
		}
		
		// Use pathfinding (handles cyclic automatically)
		Pixel2D[] path = map.shortestPath(from, to, obstacleColor);
		if (path != null && path.length > 1) {
			Pixel2D nextStep = path[1];
			
			// Calculate the actual direction needed to reach nextStep
			// Handle cyclic wrapping properly
			int stepDx = nextStep.getX() - from.getX();
			int stepDy = nextStep.getY() - from.getY();
			
			// In cyclic mode, check if wrapping gives a shorter path
			if (map.isCyclic()) {
				int wrappedDx = stepDx;
				int wrappedDy = stepDy;
				
				if (Math.abs(stepDx) > map.getWidth() / 2) {
					wrappedDx = stepDx > 0 ? stepDx - map.getWidth() : stepDx + map.getWidth();
				}
				if (Math.abs(stepDy) > map.getHeight() / 2) {
					wrappedDy = stepDy > 0 ? stepDy - map.getHeight() : stepDy + map.getHeight();
				}
				
				// Use wrapped direction if it's shorter
				if (Math.abs(wrappedDx) < Math.abs(stepDx)) stepDx = wrappedDx;
				if (Math.abs(wrappedDy) < Math.abs(stepDy)) stepDy = wrappedDy;
			}
			
			// Determine direction from stepDx and stepDy
			int dir;
			if (Math.abs(stepDx) > Math.abs(stepDy)) {
				dir = stepDx > 0 ? Game.RIGHT : Game.LEFT;
			} else if (stepDy != 0) {
				dir = stepDy > 0 ? Game.UP : Game.DOWN;
			} else {
				// Fallback to regular direction
				dir = getDirection(from, nextStep);
			}
			
			Pixel2D nextPos = getNextPosition(from, dir, map);
			return dir;
		}
		
		// Fallback: move in general direction (with cyclic support)
		// Use the wrapped dx/dy we calculated earlier
		if (Math.abs(dx) > Math.abs(dy)) {
			return dx > 0 ? Game.RIGHT : Game.LEFT;
		} else if (dy != 0) {
			return dy > 0 ? Game.UP : Game.DOWN;
		}
		
		// If still no valid direction, try all directions in order of preference
		// In cyclic mode, try the direction that moves towards target first
		int[] dirs;
		if (map.isCyclic() && Math.abs(dx) > Math.abs(dy)) {
			// Prefer horizontal movement
			dirs = dx > 0 ? 
				new int[]{Game.RIGHT, Game.LEFT, Game.UP, Game.DOWN} :
				new int[]{Game.LEFT, Game.RIGHT, Game.UP, Game.DOWN};
		} else if (map.isCyclic() && dy != 0) {
			// Prefer vertical movement
			dirs = dy > 0 ?
				new int[]{Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT} :
				new int[]{Game.DOWN, Game.UP, Game.LEFT, Game.RIGHT};
		} else {
			dirs = new int[]{Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		}

		// TODO what the hell
		return dirs[0];
	}
	
	/**
	 * Gets direction considering cyclic wrapping.
	 */
	private int getDirectionWithWrapping(Pixel2D from, Pixel2D to, int dx, int dy) {
		// For adjacent cells, use the wrapped dx/dy
		if (dx == 1 || (dx < 0 && Math.abs(dx) > 1)) return Game.RIGHT;
		if (dx == -1 || (dx > 0 && Math.abs(dx) > 1)) return Game.LEFT;
		if (dy == 1 || (dy < 0 && Math.abs(dy) > 1)) return Game.UP;
		if (dy == -1 || (dy > 0 && Math.abs(dy) > 1)) return Game.DOWN;
		
		// Fallback to regular direction
		return getDirection(from, to);
	}
	
	/**
	 * Gets the direction from one position to another.
	 */
	private int getDirection(Pixel2D from, Pixel2D to) {


		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		if (dx == 1 && dy == 0) return Game.RIGHT;
		if (dx == -1 && dy == 0) return Game.LEFT;
		if (dx == 0 && dy == 1) return Game.UP;
		if (dx == 0 && dy == -1) return Game.DOWN;
		
		if (Math.abs(dx) > Math.abs(dy)) {
			return dx > 0 ? Game.RIGHT : Game.LEFT;
		} else {
			return dy > 0 ? Game.UP : Game.DOWN;
		}
	}
	
	/**
	 * Gets all valid neighbors of a position.
	 */
	private List<Pixel2D> getValidNeighbors(Pixel2D pos, Map map, int obstacleColor) {
		List<Pixel2D> validNeighbors = new ArrayList<>();
		int[] dx = {0, -1, 0, 1};
		int[] dy = {1, 0, -1, 0};
		
		for (int i = 0; i < 4; i++) {
			int x = pos.getX() + dx[i];
			int y = pos.getY() + dy[i];
			
			if (map.isCyclic()) {
				if (x < 0) x = map.getWidth() - 1;
				else if (x >= map.getWidth()) x = 0;
				if (y < 0) y = map.getHeight() - 1;
				else if (y >= map.getHeight()) y = 0;
			} else {
				if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) continue;
			}
			
			Pixel2D neighbor = new Index2D(x, y);
			int cellValue = map.getPixel(neighbor);
			
			if (cellValue != -1 && cellValue != obstacleColor) {
				validNeighbors.add(neighbor);
			}
		}
		
		return validNeighbors;
	}
	
	/**
	 * Gets the next position after moving in a direction.
	 */
	private Pixel2D getNextPosition(Pixel2D pos, int direction, Map map) {
		int x = pos.getX();
		int y = pos.getY();

		switch (direction) {
			case Game.UP -> y = y + 1;
			case Game.DOWN -> y = y - 1;
			case Game.LEFT -> x = x - 1;
			case Game.RIGHT -> x = x + 1;
		}
		
		if (map.isCyclic()) {
			if (x < 0) x = map.getWidth() - 1;
			else if (x >= map.getWidth()) x = 0;
			if (y < 0) y = map.getHeight() - 1;
			else if (y >= map.getHeight()) y = 0;
		}
		
		return new Index2D(x, y);
	}
	
	/**
	 * Checks if a ghost is vulnerable.
	 */
	private boolean isVulnerable(GhostCL ghost) {
		return ghost.remainTimeAsEatable(0) > 0.5;
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
				return new Index2D(0, 0);
			}
		}
		return new Index2D(0, 0);
	}
	
	/**
	 * Gets ghost position as Pixel2D.
	 */
	private Pixel2D getGhostPosition(GhostCL ghost) {
		return parsePosition(ghost.getPos(0));
	}

	private Map CreateMap(int [][] board) {
		// Create map - need to handle coordinate system
		// Board is board[x][y] where x=width, y=height
		// Map stores as _map[y][x] and getPixel(x,y) returns _map[y][x]
		// So we need: _map[y][x] = board[x][y], which means we transpose
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

		return map;
	}

	private boolean IsNeedToEscape(Map2D pacmanDistances, GhostCL[] ghosts) {
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);
				if (dist <= DANGER_THRESHOLD) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean IsNeedToChase(Map2D pacmanDistances, GhostCL[] ghosts) {
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);

				if (dist <= CHASE_THRESHOLD && IsGhostOutsideSpawnArea(ghostPos)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean IsNeedPowerPellet(Map2D pacmanDistances, GhostCL[] ghosts, Map map, int green) {
		boolean hasPowerPellet = false;
		for (int x = 0; x < boardWidth && !hasPowerPellet; x++) {
			for (int y = 0; y < boardWidth; y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == green) {
					hasPowerPellet = true;
				}
			}
		}
		if (!hasPowerPellet)
		{
			return false;
		}

		// TODO maybe change it to get this more often
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);
				if (dist > DANGER_THRESHOLD && dist <= DANGER_THRESHOLD + 3 && IsGhostOutsideSpawnArea(ghostPos)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean IsGhostOutsideSpawnArea(Pixel2D ghostPos) {
		int centerX = boardWidth / 2;
		int centerY = boardHeight / 2;
		int spawnRadius = 3;

		int dx = Math.abs(ghostPos.getX() - centerX);
		int dy = Math.abs(ghostPos.getY() - centerY);
		return dx >= spawnRadius || dy >= spawnRadius;
	}
}
