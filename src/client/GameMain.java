package client;

import server.PacmanGameServer;
import server.PacmanGameInterface;

/**
 * Main entry point - runs server with GUI client
 * This is the integrated game that combines server logic with GUI display
 */
public class GameMain {
    public static void main(String[] args) {
        // Game configuration
        int width = 20;
        int height = 20;
        boolean cyclic = true;
        long randomSeed = 31;
        int dt = 50; // Delay between frames (milliseconds)
        
        // Create server
        PacmanGameServer server = new PacmanGameServer(width, height, cyclic, randomSeed);
        
        // Create and start GUI client
        GameClient client = new GameClient(server, dt);
        client.run();
    }
}
