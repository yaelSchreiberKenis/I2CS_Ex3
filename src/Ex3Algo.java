import server.Game;
import server.GhostCL;
import server.PacmanGame;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/**
 * FSM-based Pacman algorithm that maximizes score while minimizing death risk.
 * Uses BFS for distance calculations and simple heuristics for movement.
 */
public class Ex3Algo implements server.PacManAlgo {

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
		System.out.println(currentState);
		return executeState(pacmanDistances, currentState, pacmanPos, ghosts, map);
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
	private int executeState(Map2D pacmanDistances, State state, Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		return switch (state) {
			case CHASE -> chaseVulnerableGhosts(pacmanDistances, pacmanPos, ghosts, map);
			case ESCAPE -> escapeFromGhosts(pacmanPos, ghosts, map);
			case GET_POWER_PELLET -> getPowerPellet(pacmanDistances, pacmanPos, map);
			case EAT_DOTS -> eatDots(pacmanDistances, pacmanPos, ghosts, map);
		};
	}
	
	/**
	 * CHASE state: Find and chase the closest vulnerable ghost.
	 * Avoids chasing ghosts into the spawn area (middle of map).
	 */
	private int chaseVulnerableGhosts(Map2D pacmanDistances, Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
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
		if (bestGhost == null)
		{
			System.out.println("In chaseVulnerableGhosts function");
		}
		return moveTowardsTarget(pacmanPos, getGhostPosition(bestGhost), map);
	}
	
	/**
	 * ESCAPE state: Choose neighbor that maximizes minimum distance to ghosts.
	 * Prevents going to "stuck" places (where only move is back to current).
	 * When multiple equally safe directions, prefers one closer to a dot.
	 */
	private int escapeFromGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map);
		
		// Get all non-vulnerable ghosts
		List<Pixel2D> dangerousGhosts = new ArrayList<>();
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				dangerousGhosts.add(getGhostPosition(ghost));
			}
		}
		assert !dangerousGhosts.isEmpty();

		// Filter out "stuck" neighbors (where only possible next move is back to current)
		List<Pixel2D> nonStuckNeighbors = new ArrayList<>();
		for (Pixel2D neighbor : validNeighbors) {
			if (!isStuckPlace(neighbor, pacmanPos, map)) {
				nonStuckNeighbors.add(neighbor);
			}
		}
		
		// If all neighbors are stuck, use all neighbors (better than nothing)
		if (nonStuckNeighbors.isEmpty()) {
			nonStuckNeighbors = validNeighbors;
		}

		// Find the best escape direction
		Pixel2D bestNeighbor = null;
		int bestMinDist = -1;
		double bestAvgDist = -1;
		int bestDotDist = Integer.MAX_VALUE;
		
		for (Pixel2D neighbor : nonStuckNeighbors) {
			// Calculate safety score for this neighbor
			Map2D neighborDistances = map.allDistance(neighbor, OBSTACLE_COLOR);
			
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
			
			double avgDist = count > 0 ? sumDist / (double)count : 0;
			
			// Check if this is better than current best
			boolean isBetter = false;
			if (minDist > bestMinDist) {
				isBetter = true;
			} else if (minDist == bestMinDist) {
				if (avgDist > bestAvgDist) {
					isBetter = true;
				} else if (avgDist == bestAvgDist) {
					// Equally safe - prefer direction closer to a dot
					int dotDist = getDistanceToClosestDot(neighbor, map);
					if (dotDist < bestDotDist) {
						isBetter = true;
					}
				}
			}
			
			if (isBetter) {
				bestMinDist = minDist;
				bestAvgDist = avgDist;
				bestNeighbor = neighbor;
				bestDotDist = getDistanceToClosestDot(neighbor, map);
			}
		}

		if (bestNeighbor == null) {
			// Fallback
			bestNeighbor = nonStuckNeighbors.isEmpty() ? validNeighbors.get(0) : nonStuckNeighbors.get(0);
		}
		
		return getDirection(pacmanPos, bestNeighbor, map.isCyclic());
	}
	
	/**
	 * Checks if a place is "stuck" - where the only possible next move is back to the current position.
	 */
	private boolean isStuckPlace(Pixel2D place, Pixel2D currentPos, Map map) {
		List<Pixel2D> neighbors = getValidNeighbors(place, map);
		
		// If there's only one neighbor and it's the current position, we're stuck
		if (neighbors.size() == 1 && neighbors.get(0).equals(currentPos)) {
			return true;
		}
		
		// If there are no neighbors (shouldn't happen for valid positions), we're stuck
		if (neighbors.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the distance to the closest dot from a position.
	 */
	private int getDistanceToClosestDot(Pixel2D pos, Map map) {
		Map2D distances = map.allDistance(pos, OBSTACLE_COLOR);
		int minDist = Integer.MAX_VALUE;
		
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == DOT_COLOR) {
					Pixel2D dotPos = new Index2D(x, y);
					int dist = distances.getPixel(dotPos);
					if (dist > 0 && dist < minDist) {
						minDist = dist;
					}
				}
			}
		}
		
		return minDist == Integer.MAX_VALUE ? Integer.MAX_VALUE : minDist;
	}
	
	/**
	 * GET_POWER_PELLET state: Find and move towards the closest power pellet.
	 * Only used when there are non-vulnerable ghosts or ghosts becoming non-vulnerable soon.
	 */
	private int getPowerPellet(Map2D pacmanDistances, Pixel2D pacmanPos, Map map) {
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
		return moveTowardsTarget(pacmanPos, bestPellet, map);
	}
	
	/**
	 * EAT_DOTS state: Find and move towards the closest dot.
	 * Sorts dots by distance, checks for vulnerable ghosts on path (distance 2).
	 * If all dots have vulnerable ghosts on path, goes to farthest place from ghosts.
	 */
	private int eatDots(Map2D pacmanDistances, Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		// Collect all dots with their distances
		List<DotInfo> dots = new ArrayList<>();
		for (int x = 0; x < map.getWidth(); x++) {
			for (int y = 0; y < map.getHeight(); y++) {
				int cellValue = map.getPixel(x, y);
				if (cellValue == DOT_COLOR) {
					Pixel2D dotPos = new Index2D(x, y);
					int distance = pacmanDistances.getPixel(dotPos);
					if (distance > 0) {
						dots.add(new DotInfo(dotPos, distance));
					}
				}
			}
		}
		
		if (dots.isEmpty()) {
			// No dots found, return a default direction
			return Game.UP;
		}
		
		// Sort dots by distance (closest first)
		Collections.sort(dots, Comparator.comparingInt(d -> d.distance));
		
		// Check each dot from closest to farthest
		for (DotInfo dotInfo : dots) {
			Pixel2D dotPos = dotInfo.position;
			Pixel2D[] path = map.shortestPath(pacmanPos, dotPos, OBSTACLE_COLOR);
			
			if (path == null || path.length == 0) {
				continue; // No path to this dot
			}
			
			// Check if there's a vulnerable ghost within distance 2 on the path
			boolean hasVulnerableGhostOnPath = false;
			for (GhostCL ghost : ghosts) {
				if (isVulnerable(ghost)) {
					Pixel2D ghostPos = getGhostPosition(ghost);
					
					// Check if ghost is on the path within distance 2 from Pacman
					// path[0] is Pacman's position, path[1] is first step, etc.
					for (int i = 0; i < path.length && i <= 2; i++) {
						if (path[i].equals(ghostPos)) {
							hasVulnerableGhostOnPath = true;
							break;
						}
					}
					if (hasVulnerableGhostOnPath) {
						break;
					}
				}
			}
			
			// If no vulnerable ghost on path, pick this dot
			if (!hasVulnerableGhostOnPath) {
				return moveTowardsTarget(pacmanPos, dotPos, map);
			}
		}
		
		// All dots have vulnerable ghosts on path - go to farthest place from ghosts
		return goToFarthestFromGhosts(pacmanPos, ghosts, map);
	}
	
	/**
	 * Helper class to store dot position and distance.
	 */
	private static class DotInfo {
		Pixel2D position;
		int distance;
		
		DotInfo(Pixel2D position, int distance) {
			this.position = position;
			this.distance = distance;
		}
	}
	
	/**
	 * Goes to the farthest place from all ghosts.
	 */
	private int goToFarthestFromGhosts(Pixel2D pacmanPos, GhostCL[] ghosts, Map map) {
		List<Pixel2D> validNeighbors = getValidNeighbors(pacmanPos, map);
		
		if (validNeighbors.isEmpty()) {
			return Game.UP; // Fallback
		}
		
		// Get all non-vulnerable ghosts
		List<Pixel2D> dangerousGhosts = new ArrayList<>();
		for (GhostCL ghost : ghosts) {
			if (!isVulnerable(ghost)) {
				dangerousGhosts.add(getGhostPosition(ghost));
			}
		}
		
		Pixel2D bestNeighbor = null;
		int maxMinDist = -1;
		
		for (Pixel2D neighbor : validNeighbors) {
			Map2D neighborDistances = map.allDistance(neighbor, OBSTACLE_COLOR);
			
			// Find minimum distance to any ghost from this neighbor
			int minDist = Integer.MAX_VALUE;
			for (Pixel2D ghostPos : dangerousGhosts) {
				int dist = neighborDistances.getPixel(ghostPos);
				if (dist < minDist) {
					minDist = dist;
				}
			}
			
			// Prefer neighbor with maximum minimum distance
			if (minDist > maxMinDist) {
				maxMinDist = minDist;
				bestNeighbor = neighbor;
			}
		}
		
		if (bestNeighbor == null) {
			bestNeighbor = validNeighbors.get(0); // Fallback
		}
		
		return getDirection(pacmanPos, bestNeighbor, map.isCyclic());
	}
	
	/**
	 * Moves towards a target position, returning the first step direction.
	 */
	private int moveTowardsTarget(Pixel2D from, Pixel2D to, Map map) {
		Pixel2D[] path = map.shortestPath(from, to, OBSTACLE_COLOR);
		if (path == null)
		{
			System.out.println("from = " + from.toString());
			System.out.println("to = " + to.toString());
			return Game.UP;
		}
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
	private List<Pixel2D> getValidNeighbors(Pixel2D pos, Map map) {
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
			
			if (cellValue != -1 && cellValue != OBSTACLE_COLOR) {
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
		return ghost.remainTimeAsEatable(0) > 40;
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
