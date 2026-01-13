package server;

/**
 * Simple server runner for testing
 * Runs the game server standalone without GUI
 */
public class ServerRunner {
    public static void main(String[] args) {
        // Create server with default parameters
        int width = 20;
        int height = 20;
        boolean cyclic = true;
        long randomSeed = 31;
        
        PacmanGameServer server = new PacmanGameServer(width, height, cyclic, randomSeed);
        
        // Start the game
        System.out.println("Server started. Status: " + server.getStatus());
        server.play();
        System.out.println("Game started. Status: " + server.getStatus());
        
        // Simple test - get game state
        int[][] board = server.getGame(0);
        System.out.println("Board dimensions: " + board.length + "x" + board[0].length);
        System.out.println("Pacman position: " + server.getPos(0));
        System.out.println("Ghosts count: " + server.getGhosts(0).length);
        
        // Test a move
        System.out.println("\nTesting move...");
        server.move(PacmanGameInterface.RIGHT);
        System.out.println("New Pacman position: " + server.getPos(0));
        
        System.out.println("\nServer test completed successfully!");
    }
}
