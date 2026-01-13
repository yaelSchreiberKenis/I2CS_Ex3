package client;

import server.PacmanGameServer;
import server.PacmanGameInterface;
import server.GhostInterface;
import server.GameState;
import server.StdDraw;
import java.awt.Color;

/**
 * GUI Client for Pacman game using StdDraw
 * Handles rendering and user input
 */
public class GameClient {
    private PacmanGameServer server;
    private int dt; // Frame delay in milliseconds
    private boolean running;
    
    public GameClient(PacmanGameServer server, int dt) {
        this.server = server;
        this.dt = dt;
        this.running = true;
    }
    
    public void run() {
        initializeDisplay();
        
        // Main game loop
        while (running && server.getStatus() != PacmanGameInterface.DONE) {
            handleInput();
            update();
            render();
            
            StdDraw.pause(dt);
        }
        
        // Show final message
        showGameOver();
    }
    
    private void initializeDisplay() {
        int width = server.getGame(0).length;
        int height = server.getGame(0)[0].length;
        
        // Set canvas size based on board dimensions
        int canvasSize = Math.max(600, Math.max(width, height) * 30);
        StdDraw.setCanvasSize(canvasSize, canvasSize);
        
        // Set scale (0 to 1 in both directions)
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        
        // Enable double buffering for smooth animation
        StdDraw.enableDoubleBuffering();
    }
    
    private void handleInput() {
        // Check for keyboard input using StdDraw
        if (StdDraw.hasNextKeyTyped()) {
            char key = StdDraw.nextKeyTyped();
            server.setKeyChar(key);
            
            if (key == ' ') {
                server.play(); // Toggle pause
            } else if (key == 'q' || key == 'Q') {
                server.end(-1);
                running = false;
            }
        }
    }
    
    private void update() {
        // Game updates are handled by the server
        // This is where we could add client-side animations, etc.
    }
    
    private void render() {
        // Clear screen
        StdDraw.clear(Color.BLACK);
        
        // Draw game elements
        drawBoard();
        drawPacman();
        drawGhosts();
        drawUI();
        
        // Show the buffer
        StdDraw.show();
    }
    
    private void drawBoard() {
        int[][] board = server.getGame(0);
        int width = board.length;
        int height = board[0].length;
        
        double cellWidth = 1.0 / width;
        double cellHeight = 1.0 / height;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double centerX = (x + 0.5) * cellWidth;
                double centerY = 1.0 - (y + 0.5) * cellHeight; // Flip Y axis (StdDraw origin is bottom-left)
                
                int cell = board[x][y];
                if (cell == GameState.DOT) {
                    StdDraw.setPenColor(Color.YELLOW);
                    StdDraw.filledCircle(centerX, centerY, Math.min(cellWidth, cellHeight) * 0.15);
                } else if (cell == GameState.POWER_PELLET) {
                    StdDraw.setPenColor(Color.PINK);
                    StdDraw.filledCircle(centerX, centerY, Math.min(cellWidth, cellHeight) * 0.25);
                }
            }
        }
    }
    
    private void drawPacman() {
        String posStr = server.getPos(0);
        String[] parts = posStr.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        
        int[][] board = server.getGame(0);
        double cellWidth = 1.0 / board.length;
        double cellHeight = 1.0 / board[0].length;
        
        double centerX = (x + 0.5) * cellWidth;
        double centerY = 1.0 - (y + 0.5) * cellHeight;
        
        StdDraw.setPenColor(Color.YELLOW);
        double radius = Math.min(cellWidth, cellHeight) * 0.4;
        StdDraw.filledCircle(centerX, centerY, radius);
    }
    
    private void drawGhosts() {
        GhostInterface[] ghosts = server.getGhosts(0);
        int[][] board = server.getGame(0);
        double cellWidth = 1.0 / board.length;
        double cellHeight = 1.0 / board[0].length;
        
        for (GhostInterface ghost : ghosts) {
            String posStr = ghost.getPos(0);
            String[] parts = posStr.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            
            // Skip if ghost is off screen (eaten)
            if (x < 0 || y < 0 || x >= board.length || y >= board[0].length) {
                continue;
            }
            
            double centerX = (x + 0.5) * cellWidth;
            double centerY = 1.0 - (y + 0.5) * cellHeight;
            
            // Ghost color depends on vulnerability
            if (ghost.remainTimeAsEatable(0) > 0) {
                StdDraw.setPenColor(Color.BLUE); // Vulnerable = blue
            } else {
                StdDraw.setPenColor(Color.RED); // Normal = red
            }
            
            double radius = Math.min(cellWidth, cellHeight) * 0.4;
            StdDraw.filledCircle(centerX, centerY, radius);
        }
    }
    
    private void drawUI() {
        GameState state = server.getGameState();
        
        // Draw score (top-left)
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(0.1, 0.95, "Score: " + state.getScore());
        StdDraw.text(0.1, 0.90, "Lives: " + state.getLives());
        
        // Draw status
        String statusStr = getStatusString(server.getStatus());
        StdDraw.text(0.5, 0.95, statusStr);
    }
    
    private String getStatusString(int status) {
        switch (status) {
            case PacmanGameInterface.NOT_STARTED:
                return "NOT STARTED - Press SPACE";
            case PacmanGameInterface.RUNNING:
                return "RUNNING";
            case PacmanGameInterface.PAUSED:
                return "PAUSED - Press SPACE";
            case PacmanGameInterface.WIN:
                return "YOU WIN!";
            case PacmanGameInterface.LOSE:
                return "GAME OVER";
            default:
                return "";
        }
    }
    
    private void showGameOver() {
        int status = server.getStatus();
        if (status == PacmanGameInterface.WIN) {
            System.out.println("YOU WIN! Score: " + server.getGameState().getScore());
        } else if (status == PacmanGameInterface.LOSE) {
            System.out.println("GAME OVER! Final Score: " + server.getGameState().getScore());
        }
    }
}
