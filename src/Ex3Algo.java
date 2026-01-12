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
	
	public Ex3Algo() {
		_count = 0;
	}
	
	@Override
	public String getInfo() {
		return "FSM-based Pacman algorithm using BFS distance calculations";
	}
	
	@Override
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
		
		// Find obstacle color by checking what's around pacman
		int obstacleColor = findObstacleColor(pacmanPos, map, green, black, blue);
		
		// Determine state
		State currentState = determineState(pacmanPos, ghosts, map, obstacleColor);
		
		// Execute state behavior
		int direction = executeState(currentState, pacmanPos, ghosts, map, obstacleColor, blue, pink);
		
		// CRITICAL: Always verify direction is valid, if not get a valid one
		Pixel2D nextPos = getNextPosition(pacmanPos, direction, map);
		if (!isValidPosition(nextPos, map, obstacleColor)) {
			// Get any valid direction
			List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
			if (!validNeighbors.isEmpty()) {
				direction = getDirection(pacmanPos, validNeighbors.get(0));
			} else {
				// Last resort: try each direction
				int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
				for (int dir : dirs) {
					nextPos = getNextPosition(pacmanPos, dir, map);
					if (isValidPosition(nextPos, map, obstacleColor)) {
						direction = dir;
						break;
					}
				}
			}
		}
		
		_count++;
		return direction;
	}
	
	/**
	 * Finds the obstacle color by checking neighbors around pacman.
	 */
	private int findObstacleColor(Pixel2D pacmanPos, Map map, int green, int black, int blue) {
		int pacmanCell = map.getPixel(pacmanPos);
		
		// Check all neighbors
		int[] dx = {0, -1, 0, 1};
		int[] dy = {1, 0, -1, 0};
		
		for (int i = 0; i < 4; i++) {
			int x = pacmanPos.getX() + dx[i];
			int y = pacmanPos.getY() + dy[i];
			
			// Handle cyclic
			if (map.isCyclic()) {
				if (x < 0) x = map.getWidth() - 1;
				else if (x >= map.getWidth()) x = 0;
				if (y < 0) y = map.getHeight() - 1;
				else if (y >= map.getHeight()) y = 0;
			} else {
				if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) continue;
			}
			
			int cellValue = map.getPixel(x, y);
			if (cellValue == -1) continue;
			
			// If this cell is different from pacman's cell and is green or black, it might be a wall
			if (cellValue != pacmanCell) {
				if (cellValue == green) return green;
				if (cellValue == black && pacmanCell != black) return black;
			}
		}
		
		// Default to green
		return green;
	}
	
	/**
	 * Determines the current state based on game situation.
	 */
	private State determineState(Pixel2D pacmanPos, GhostCL[] ghosts, Map map, int obstacleColor) {
		// Check for vulnerable ghosts (highest priority)
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);
				if (dist != -1 && dist <= CHASE_THRESHOLD && dist > 0) {
					return State.CHASE;
				}
			}
		}
		
		// Check for immediate danger (escape priority)
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);
				if (dist != -1 && dist <= DANGER_THRESHOLD && dist > 0) {
					return State.ESCAPE;
				}
			}
		}
		
		// Check if danger is near but manageable (get power pellet)
		boolean dangerNear = false;
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int dist = pacmanDistances.getPixel(ghostPos);
				if (dist != -1 && dist <= DANGER_THRESHOLD + 3 && dist > 0) {
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
	                        Map map, int obstacleColor, int blue, int pink) {
		switch (state) {
			case CHASE:
				return chaseVulnerableGhosts(pacmanPos, ghosts, map, obstacleColor);
			case ESCAPE:
				return escapeFromGhosts(pacmanPos, ghosts, map, obstacleColor);
			case GET_POWER_PELLET:
				return getPowerPellet(pacmanPos, map, obstacleColor, pink);
			case EAT_DOTS:
				return eatDots(pacmanPos, map, obstacleColor, blue);
			default:
				return eatDots(pacmanPos, map, obstacleColor, blue);
		}
	}
	
	/**
	 * CHASE state: Find and chase the closest vulnerable ghost.
	 */
	private int chaseVulnerableGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, 
	                                  Map map, int obstacleColor) {
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		GhostCL bestGhost = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D ghostPos = getGhostPosition(ghost);
				int distance = pacmanDistances.getPixel(ghostPos);
				if (distance != -1 && distance > 0 && distance < bestDist && distance <= CHASE_THRESHOLD) {
					bestDist = distance;
					bestGhost = ghost;
				}
			}
		}
		
		if (bestGhost != null) {
			return moveTowardsTarget(pacmanPos, getGhostPosition(bestGhost), map, obstacleColor);
		}
		
		// Fallback
		return eatDots(pacmanPos, map, obstacleColor, 0);
	}
	
	/**
	 * ESCAPE state: Choose neighbor that maximizes minimum distance to ghosts.
	 */
	private int escapeFromGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, 
	                             Map map, int obstacleColor) {
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		if (validNeighbors.isEmpty()) {
			return Game.UP;
		}
		
		int bestDir = Game.UP;
		int bestMinDist = -1;
		
		for (Pixel2D neighbor : validNeighbors) {
			Map2D neighborDistances = map.allDistance(neighbor, obstacleColor);
			int minDist = Integer.MAX_VALUE;
			
			for (GhostCL ghost : ghosts) {
				if (!isVulnerable(ghost)) {
					Pixel2D ghostPos = getGhostPosition(ghost);
					int dist = neighborDistances.getPixel(ghostPos);
					if (dist != -1 && dist < minDist) {
						minDist = dist;
					}
				}
			}
			
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
	private int getPowerPellet(Pixel2D pacmanPos, Map map, int obstacleColor, int pink) {
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		Pixel2D bestPellet = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == pink) {
					Pixel2D pelletPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(pelletPos);
					if (distance != -1 && distance > 0 && distance < bestDist) {
						bestDist = distance;
						bestPellet = pelletPos;
					}
				}
			}
		}
		
		if (bestPellet != null) {
			return moveTowardsTarget(pacmanPos, bestPellet, map, obstacleColor);
		}
		
		// Fallback
		return eatDots(pacmanPos, map, obstacleColor, 0);
	}
	
	/**
	 * EAT_DOTS state: Find and move towards the closest dot.
	 */
	private int eatDots(Pixel2D pacmanPos, Map map, int obstacleColor, int blue) {
		// First check immediate neighbors
		List<Pixel2D> neighbors = getValidNeighbors(pacmanPos, map, obstacleColor);
		
		// Prefer neighbors with dots (blue)
		for (Pixel2D neighbor : neighbors) {
			int cellValue = map.getPixel(neighbor);
			if (cellValue == blue) {
				return getDirection(pacmanPos, neighbor);
			}
		}
		
		// If no dot in neighbors, find closest dot
		Map2D pacmanDistances = map.allDistance(pacmanPos, obstacleColor);
		Pixel2D bestDot = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == blue || (cellValue != obstacleColor && cellValue != -1)) {
					Pixel2D dotPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(dotPos);
					if (distance != -1 && distance > 0 && distance < bestDist) {
						bestDist = distance;
						bestDot = dotPos;
					}
				}
			}
		}
		
		if (bestDot != null) {
			return moveTowardsTarget(pacmanPos, bestDot, map, obstacleColor);
		}
		
		// Fallback: move to any valid neighbor
		if (!neighbors.isEmpty()) {
			return getDirection(pacmanPos, neighbors.get(0));
		}
		
		// Last resort
		return Game.UP;
	}
	
	/**
	 * Moves towards a target position, returning the first step direction.
	 */
	private int moveTowardsTarget(Pixel2D from, Pixel2D to, Map map, int obstacleColor) {
		if (from.equals(to)) {
			return eatDots(from, map, obstacleColor, 0);
		}
		
		// If adjacent, go directly
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		if (Math.abs(dx) + Math.abs(dy) == 1) {
			int dir = getDirection(from, to);
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Use pathfinding
		Pixel2D[] path = map.shortestPath(from, to, obstacleColor);
		if (path != null && path.length > 1) {
			Pixel2D nextStep = path[1];
			int dir = getDirection(from, nextStep);
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Fallback: move in general direction
		if (Math.abs(dx) > Math.abs(dy)) {
			int dir = dx > 0 ? Game.RIGHT : Game.LEFT;
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		} else if (dy != 0) {
			int dir = dy > 0 ? Game.UP : Game.DOWN;
			Pixel2D nextPos = getNextPosition(from, dir, map);
			if (isValidPosition(nextPos, map, obstacleColor)) {
				return dir;
			}
		}
		
		// Final fallback
		return eatDots(from, map, obstacleColor, 0);
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
			case Game.UP:
				y = y + 1;
				break;
			case Game.DOWN:
				y = y - 1;
				break;
			case Game.LEFT:
				x = x - 1;
				break;
			case Game.RIGHT:
				x = x + 1;
				break;
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
	 * Checks if a position is valid.
	 */
	private boolean isValidPosition(Pixel2D pos, Map map, int obstacleColor) {
		if (!map.isInside(pos) && !map.isCyclic()) {
			return false;
		}
		int cellValue = map.getPixel(pos);
		return cellValue != -1 && cellValue != obstacleColor;
	}
	
	/**
	 * Checks if a ghost is vulnerable.
	 */
	private boolean isVulnerable(GhostCL ghost) {
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
}
