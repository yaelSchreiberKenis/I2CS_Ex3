import server.Game;
import server.GhostCL;
import server.PacmanGame;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FSM-based Pacman algorithm that maximizes score while minimizing death risk.
 * 
 * States:
 * - ESCAPE: Run away from nearby non-vulnerable ghosts
 * - CHASE: Hunt vulnerable ghosts within range
 * - GET_POWER_PELLET: Move towards power pellet when ghost is approaching
 * - EAT_DOTS: Default state - collect dots efficiently
 * 
 * Key behaviors:
 * - Avoids dead-end corridors when escaping
 * - Preserves power pellets when ghosts are already vulnerable
 * - Uses BFS (via Map class) for shortest path calculations
 */
public class Ex3Algo implements server.PacManAlgo {

	// ==================== CONSTANTS ====================
	
	/** Distance at which a non-vulnerable ghost triggers ESCAPE state */
	private static final int DANGER_THRESHOLD = 5;
	
	/** Maximum distance to chase a vulnerable ghost */
	private static final int CHASE_THRESHOLD = 3;
	
	/** Distance range for proactively getting power pellet (DANGER_THRESHOLD to DANGER_THRESHOLD+3) */
	private static final int POWER_PELLET_RANGE = 3;
	
	/** Spawn area radius - ghosts inside this area are ignored for chasing */
	private static final int SPAWN_RADIUS = 3;
	
	// Cell colors for map interpretation
	private static final int OBSTACLE_COLOR = Game.getIntColor(Color.BLUE, 0);
	private static final int DOT_COLOR = Game.getIntColor(Color.PINK, 0);
	private static final int POWER_PELLET_COLOR = Game.getIntColor(Color.GREEN, 0);

	// ==================== STATE ====================
	
	private enum State { CHASE, ESCAPE, GET_POWER_PELLET, EAT_DOTS }
	
	private int boardWidth;
	private int boardHeight;

	// ==================== CONSTRUCTOR ====================
	
	public Ex3Algo() {}
	
	@Override
	public String getInfo() {
		return "FSM-based Pacman algorithm using BFS distance calculations";
	}

	// ==================== MAIN ENTRY POINT ====================
	
	@Override
	public int move(PacmanGame game) {
		// Extract game data
		int[][] board = game.getGame(0);
		Pixel2D pacmanPos = parsePosition(game.getPos(0));
		GhostCL[] ghosts = game.getGhosts(0);
		
		// Create map for pathfinding
		Map map = createMap(board);
		boardWidth = map.getWidth();
		boardHeight = map.getHeight();
		
		// Precompute distances from Pacman to all cells
		Map2D distances = map.allDistance(pacmanPos, OBSTACLE_COLOR);

		// Determine and execute state
		State state = determineState(distances, ghosts, map);
		return executeState(state, distances, pacmanPos, ghosts, map);
	}

	// ==================== STATE MACHINE ====================
	
	/**
	 * Determines the current FSM state based on game situation.
	 * Priority: ESCAPE > CHASE > GET_POWER_PELLET > EAT_DOTS
	 */
	private State determineState(Map2D distances, GhostCL[] ghosts, Map map) {
		if (shouldEscape(distances, ghosts)) {
			return State.ESCAPE;
		}
		if (shouldChase(distances, ghosts)) {
			return State.CHASE;
		}
		if (shouldGetPowerPellet(distances, ghosts, map)) {
			return State.GET_POWER_PELLET;
		}
		return State.EAT_DOTS;
	}
	
	/** Routes to appropriate state handler */
	private int executeState(State state, Map2D distances, Pixel2D pos, GhostCL[] ghosts, Map map) {
		return switch (state) {
			case ESCAPE -> doEscape(pos, ghosts, map);
			case CHASE -> doChase(distances, pos, ghosts, map);
			case GET_POWER_PELLET -> doGetPowerPellet(distances, pos, map);
			case EAT_DOTS -> doEatDots(distances, pos, ghosts, map);
		};
	}

	// ==================== STATE CONDITIONS ====================
	
	/** Returns true if any non-vulnerable ghost is within danger threshold */
	private boolean shouldEscape(Map2D distances, GhostCL[] ghosts) {
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost) && distances.getPixel(getPosition(ghost)) <= DANGER_THRESHOLD) {
				return true;
			}
		}
		return false;
	}
	
	/** Returns true if a vulnerable ghost is close enough to chase (and outside spawn) */
	private boolean shouldChase(Map2D distances, GhostCL[] ghosts) {
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D pos = getPosition(ghost);
				if (distances.getPixel(pos) <= CHASE_THRESHOLD && isOutsideSpawn(pos)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** Returns true if we should proactively get a power pellet */
	private boolean shouldGetPowerPellet(Map2D distances, GhostCL[] ghosts, Map map) {
		// Don't get power pellet if ghosts are already vulnerable
		if (hasVulnerableGhost(ghosts)) return false;
		
		// Don't get power pellet if none exist
		if (!hasPowerPellet(map)) return false;
		
		// Get power pellet if a ghost is approaching (between danger and danger+range)
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				Pixel2D pos = getPosition(ghost);
				int dist = distances.getPixel(pos);
				if (dist > DANGER_THRESHOLD && dist <= DANGER_THRESHOLD + POWER_PELLET_RANGE && isOutsideSpawn(pos)) {
					return true;
				}
			}
		}
		return false;
	}

	// ==================== STATE ACTIONS ====================
	
	/**
	 * ESCAPE: Move to the safest neighbor (maximizes distance from ghosts).
	 * Avoids dead-ends where only exit is back to current position.
	 * Tie-breaker: if power pellet exists, prefer closer to it; otherwise closer to dot.
	 */
	private int doEscape(Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		List<Pixel2D> neighbors = getValidNeighbors(pacmanPos, map);
		List<Pixel2D> dangerousGhosts = getNonVulnerableGhostPositions(ghosts);
		
		// Filter out dead-end neighbors
		List<Pixel2D> safeNeighbors = filterDeadEnds(neighbors, pacmanPos, map);
		if (safeNeighbors.isEmpty()) safeNeighbors = neighbors;

		// Determine what to prioritize as tie-breaker
		boolean powerPelletExists = hasPowerPellet(map);
		int tieBreakColor = powerPelletExists ? POWER_PELLET_COLOR : DOT_COLOR;

		// Find neighbor that maximizes minimum distance to ghosts
		Pixel2D best = null;
		int bestMinDist = -1;
		int bestTieBreakDist = Integer.MAX_VALUE;
		
		for (Pixel2D neighbor : safeNeighbors) {
			int minGhostDist = getMinDistanceToGhosts(neighbor, dangerousGhosts, map);
			int tieBreakDist = getDistanceToClosest(neighbor, tieBreakColor, map);
			
			// Better if: farther from ghosts, or same distance but closer to tie-breaker target
			if (minGhostDist > bestMinDist || (minGhostDist == bestMinDist && tieBreakDist < bestTieBreakDist)) {
				best = neighbor;
				bestMinDist = minGhostDist;
				bestTieBreakDist = tieBreakDist;
			}
		}
		
		return getDirection(pacmanPos, best != null ? best : neighbors.get(0), map.isCyclic());
	}
	
	/**
	 * CHASE: Move towards the closest vulnerable ghost (outside spawn area).
	 */
	private int doChase(Map2D distances, Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		Pixel2D target = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) {
				Pixel2D pos = getPosition(ghost);
				int dist = distances.getPixel(pos);
				if (dist <= CHASE_THRESHOLD && dist < bestDist && isOutsideSpawn(pos)) {
					bestDist = dist;
					target = pos;
				}
			}
		}
		
		return moveTowards(pacmanPos, target, map);
	}
	
	/**
	 * GET_POWER_PELLET: Move towards the closest power pellet.
	 */
	private int doGetPowerPellet(Map2D distances, Pixel2D pacmanPos, Map map) {
		Pixel2D target = findClosest(distances, POWER_PELLET_COLOR, map);
		return moveTowards(pacmanPos, target, map);
	}
	
	/**
	 * EAT_DOTS: Move towards the closest dot.
	 * If ghosts are vulnerable, avoid stepping on power pellets - try alternative paths first.
	 */
	private int doEatDots(Map2D distances, Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		boolean avoidPowerPellets = hasVulnerableGhost(ghosts);
		Pixel2D target = findClosest(distances, DOT_COLOR, map);
		
		if (target == null) return Game.UP;
		
		// Get path towards target
		Pixel2D[] path = map.shortestPath(pacmanPos, target, OBSTACLE_COLOR);
		if (path == null || path.length < 2) return Game.UP;
		
		Pixel2D nextStep = path[1];
		
		// If ghosts are vulnerable and path goes through power pellet, try to find alternative
		if (avoidPowerPellets && pathContainsPowerPellet(path, map)) {
			// Try to find alternative neighbor that leads to same dot without power pellet
			Pixel2D alternative = findAlternativePathToDot(pacmanPos, target, map);
			if (alternative != null) {
				return getDirection(pacmanPos, alternative, map.isCyclic());
			}
			// No alternative path - go through (better than getting stuck)
		}
		
		return getDirection(pacmanPos, nextStep, map.isCyclic());
	}
	
	/** Checks if path contains any power pellet */
	private boolean pathContainsPowerPellet(Pixel2D[] path, Map map) {
		for (int i = 1; i < path.length; i++) {
			if (map.getPixel(path[i]) == POWER_PELLET_COLOR) {
				return true;
			}
		}
		return false;
	}
	
	/** Finds alternative neighbor that leads to dot without going through power pellet */
	private Pixel2D findAlternativePathToDot(Pixel2D from, Pixel2D target, Map map) {
		Pixel2D best = null;
		int bestDist = Integer.MAX_VALUE;
		
		for (Pixel2D neighbor : getValidNeighbors(from, map)) {
			// Skip if neighbor itself is a power pellet
			if (map.getPixel(neighbor) == POWER_PELLET_COLOR) continue;
			
			// Check path from this neighbor to target
			Pixel2D[] altPath = map.shortestPath(neighbor, target, OBSTACLE_COLOR);
			if (altPath == null) continue;
			
			// Check if this alternative path avoids power pellets
			boolean hasPowerPellet = false;
			for (int i = 0; i < altPath.length; i++) {
				if (map.getPixel(altPath[i]) == POWER_PELLET_COLOR) {
					hasPowerPellet = true;
					break;
				}
			}
			
			if (!hasPowerPellet && altPath.length < bestDist) {
				bestDist = altPath.length;
				best = neighbor;
			}
		}
		return best;
	}

	// ==================== HELPER METHODS ====================
	
	
	/** Filters out dead-end neighbors (where only exit is back to current position) */
	private List<Pixel2D> filterDeadEnds(List<Pixel2D> neighbors, Pixel2D current, Map map) {
		List<Pixel2D> result = new ArrayList<>();
		for (Pixel2D neighbor : neighbors) {
			List<Pixel2D> exits = getValidNeighbors(neighbor, map);
			// Not a dead-end if more than one exit, or the single exit isn't back to current
			if (exits.size() > 1 || (exits.size() == 1 && !exits.get(0).equals(current))) {
				result.add(neighbor);
			}
		}
		return result;
	}
	
	/** Gets minimum distance from a position to any ghost in the list */
	private int getMinDistanceToGhosts(Pixel2D pos, List<Pixel2D> ghosts, Map map) {
		Map2D distances = map.allDistance(pos, OBSTACLE_COLOR);
		int minDist = Integer.MAX_VALUE;
		for (Pixel2D ghost : ghosts) {
			int dist = distances.getPixel(ghost);
			if (dist < minDist) minDist = dist;
		}
		return minDist;
	}
	
	/** Gets positions of all non-vulnerable ghosts */
	private List<Pixel2D> getNonVulnerableGhostPositions(GhostCL[] ghosts) {
		List<Pixel2D> result = new ArrayList<>();
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				result.add(getPosition(ghost));
			}
		}
		return result;
	}
	
	/** Finds closest cell of given color using precomputed distances */
	private Pixel2D findClosest(Map2D distances, int color, Map map) {
		Pixel2D closest = null;
		int minDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				if (map.getPixel(x, y) == color) {
					int dist = distances.getPixel(x, y);
					if (dist > 0 && dist < minDist) {
						minDist = dist;
						closest = new Index2D(x, y);
					}
				}
			}
		}
		return closest;
	}
	
	/** Gets distance to closest cell of given color from a position */
	private int getDistanceToClosest(Pixel2D pos, int color, Map map) {
		Map2D distances = map.allDistance(pos, OBSTACLE_COLOR);
		int minDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				if (map.getPixel(x, y) == color) {
					int dist = distances.getPixel(x, y);
					if (dist > 0 && dist < minDist) minDist = dist;
				}
			}
		}
		return minDist;
	}
	
	/** Returns first step direction towards target */
	private int moveTowards(Pixel2D from, Pixel2D to, Map map) {
		if (to == null) return Game.UP;
		Pixel2D[] path = map.shortestPath(from, to, OBSTACLE_COLOR);
		if (path == null || path.length < 2) return Game.UP;
		return getDirection(from, path[1], map.isCyclic());
	}

	// ==================== GHOST UTILITIES ====================
	
	private boolean isVulnerable(GhostCL ghost) {
		return ghost.remainTimeAsEatable(0) > 0;
	}
	
	private boolean hasVulnerableGhost(GhostCL[] ghosts) {
		for (GhostCL ghost : ghosts) {
			if (isVulnerable(ghost)) return true;
		}
		return false;
	}
	
	private Pixel2D getPosition(GhostCL ghost) {
		return parsePosition(ghost.getPos(0));
	}
	
	private boolean isOutsideSpawn(Pixel2D pos) {
		int dx = Math.abs(pos.getX() - boardWidth / 2);
		int dy = Math.abs(pos.getY() - boardHeight / 2);
		return dx >= SPAWN_RADIUS || dy >= SPAWN_RADIUS;
	}

	// ==================== MAP UTILITIES ====================
	
	private boolean hasPowerPellet(Map map) {
		for (int x = 0; x < boardWidth; x++) {
			for (int y = 0; y < boardHeight; y++) {
				if (map.getPixel(x, y) == POWER_PELLET_COLOR) return true;
			}
		}
		return false;
	}
	
	/** Creates Map from game board (handles coordinate transposition) */
	private Map createMap(int[][] board) {
		int width = board.length;
		int height = board[0].length;
		int[][] transposed = new int[height][width];
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				transposed[y][x] = board[x][y];
			}
		}
		
		Map map = new Map(transposed);
		map.setCyclic(GameInfo.CYCLIC_MODE);
		return map;
	}
	
	/** Gets all walkable neighbors of a position */
	private List<Pixel2D> getValidNeighbors(Pixel2D pos, Map map) {
		List<Pixel2D> result = new ArrayList<>();
		int[][] deltas = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
		
		for (int[] d : deltas) {
			int x = pos.getX() + d[0];
			int y = pos.getY() + d[1];
			
			// Handle cyclic wrapping
			if (map.isCyclic()) {
				x = (x + map.getWidth()) % map.getWidth();
				y = (y + map.getHeight()) % map.getHeight();
			} else if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
				continue;
			}
			
			int cell = map.getPixel(x, y);
			if (cell != -1 && cell != OBSTACLE_COLOR) {
				result.add(new Index2D(x, y));
			}
		}
		return result;
	}
	
	/** Converts position delta to direction constant */
	private int getDirection(Pixel2D from, Pixel2D to, boolean cyclic) {
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		
		// Handle cyclic wrapping
		if (cyclic) {
			if (dx > boardWidth / 2) dx -= boardWidth;
			else if (dx < -boardWidth / 2) dx += boardWidth;
			if (dy > boardHeight / 2) dy -= boardHeight;
			else if (dy < -boardHeight / 2) dy += boardHeight;
		}
		
		if (dx > 0) return Game.RIGHT;
		if (dx < 0) return Game.LEFT;
		if (dy > 0) return Game.UP;
		if (dy < 0) return Game.DOWN;
		return Game.UP;
	}
	
	/** Parses "x,y" string to Pixel2D */
	private Pixel2D parsePosition(String posStr) {
		String[] parts = posStr.split(",");
		if (parts.length == 2) {
			try {
				return new Index2D(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
			} catch (NumberFormatException e) {
				return new Index2D(0, 0);
			}
		}
		return new Index2D(0, 0);
	}
}
