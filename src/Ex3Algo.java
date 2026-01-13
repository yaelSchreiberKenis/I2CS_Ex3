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
	private static final int DANGER_THRESHOLD = 5;  // Distance threshold for immediate danger
	private static final int CHASE_THRESHOLD = 2;  // Max distance to chase vulnerable ghosts
	private static final int OBSTACLE_COLOR = Game.getIntColor(Color.BLUE, 0);
	private static final int DOT_COLOR = Game.getIntColor(Color.PINK, 0);
	private static final int POWER_PELLET_COLOR = Game.getIntColor(Color.GREEN, 0);

	private int boardWidth;
	private int boardHeight;

	// TODO update readme
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
		
		Map map = CreateMap(board);
		boardWidth = map.getWidth();
		boardHeight = map.getHeight();
		Map2D pacmanDistances = map.allDistance(pacmanPos, OBSTACLE_COLOR);

		// Determine state
		State currentState = determineState(pacmanDistances, ghosts, map);
		return executeState(pacmanDistances, currentState, pacmanPos, ghosts, map, OBSTACLE_COLOR);
	}
	
	/**
	 * Determines the current state based on game situation.
	 */
	private State determineState(Map2D pacmanDistances, GhostCL[] ghosts, Map map) {
		if (IsNeedToEscape(pacmanDistances, ghosts)) {return State.ESCAPE;}
		if (IsNeedToChase(pacmanDistances, ghosts)) {return State.CHASE;}
		if (IsNeedPowerPellet(pacmanDistances, ghosts, map)) {return State.GET_POWER_PELLET;}
		return State.EAT_DOTS;
	}
	
	/**
	 * Executes the behavior for the current state.
	 */
	private int executeState(Map2D pacmanDistances, State state, Pixel2D pacmanPos, GhostCL[] ghosts,
	                        Map map, int obstacleColor) {
		return switch (state) {
			case CHASE -> chaseVulnerableGhosts(pacmanDistances, pacmanPos, ghosts, map, obstacleColor);
			case ESCAPE -> escapeFromGhosts(pacmanPos, ghosts, map, obstacleColor);
			case GET_POWER_PELLET -> getPowerPellet(pacmanDistances, pacmanPos, map, obstacleColor);
			case EAT_DOTS -> eatDots(pacmanDistances, pacmanPos, map, obstacleColor);
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
				bestDir = getDirection(pacmanPos, neighbor, map.isCyclic());
			}
		}

		return bestDir;
	}
	
	/**
	 * GET_POWER_PELLET state: Find and move towards the closest power pellet.
	 * Only used when there are non-vulnerable ghosts or ghosts becoming non-vulnerable soon.
	 */
	private int getPowerPellet(Map2D pacmanDistances, Pixel2D pacmanPos, Map map, int obstacleColor) {
		// Find the closest power pellet (reuse pacmanDistances already calculated above)
		Pixel2D bestPellet = null;
		int bestDist = Integer.MAX_VALUE;

		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == POWER_PELLET_COLOR) {
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
	}
	
	/**
	 * EAT_DOTS state: Find and move towards the closest dot.
	 * Properly handles cyclic movement.
	 * Also checks for nearby power pellets (pink) and prioritizes them.
	 */
	private int eatDots(Map2D pacmanDistances, Pixel2D pacmanPos, Map map, int obstacleColor) {
		Pixel2D bestDot = null;
		int bestDist = Integer.MAX_VALUE;
		
		// Search for dots (pink cells)
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == DOT_COLOR) {
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
		return moveTowardsTarget(pacmanPos, bestDot, map, obstacleColor);
	}
	
	/**
	 * Moves towards a target position, returning the first step direction.
	 */
	private int moveTowardsTarget(Pixel2D from, Pixel2D to, Map map, int obstacleColor) {
		Pixel2D[] path = map.shortestPath(from, to, obstacleColor);
		assert path != null;
		Pixel2D nextStep = path[1];
		return getDirection(from, nextStep, map.isCyclic());
	}
	
	/**
	 * Gets the direction from one position to another.
	 */
	private int getDirection(Pixel2D from, Pixel2D to, boolean isCyclic) {
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();

		if (dy == 0 && (dx == 1 || (isCyclic && dx == 1 - boardWidth))) return Game.RIGHT;
		if (dy == 0 && (dx == -1 || (isCyclic && dx == boardWidth - 1))) return Game.LEFT;
		if (dx == 0 && (dy == 1 || (isCyclic && dy == 1 - boardHeight))) return Game.UP;
		if (dx == 0 && (dy == -1 || (isCyclic && dy == boardHeight - 1))) return Game.DOWN;
		assert false;
		return Game.DOWN;
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
	 * Checks if a ghost is vulnerable.
	 */
	private boolean isVulnerable(GhostCL ghost) {
		// The 0.5 is for safety
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

	private boolean IsNeedPowerPellet(Map2D pacmanDistances, GhostCL[] ghosts, Map map) {
		boolean hasPowerPellet = false;
		for (int x = 0; x < boardWidth && !hasPowerPellet; x++) {
			for (int y = 0; y < boardWidth; y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == POWER_PELLET_COLOR) {
					hasPowerPellet = true;
				}
			}
		}
		if (!hasPowerPellet)
		{
			return false;
		}

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
