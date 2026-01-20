package server;

import javax.sound.sampled.*;
import java.io.File;

/**
 * Manages audio playback for the Pacman game.
 * Handles sound effects for game events.
 */
public class AudioManager {
    
    private boolean audioEnabled = true;
    
    // Audio file paths
    private static final String GHOST_EATEN_FILE = "sounds/ghost_eaten.wav";
    private static final String DOT_EATEN_FILE = "sounds/dot_eaten.wav";
    private static final String POWER_PELLET_FILE = "sounds/power_pellet.wav";
    private static final String GAME_OVER_FILE = "sounds/game_over.wav";
    private static final String WIN_FILE = "sounds/win.wav";
    
    public AudioManager() {
        // Create sounds directory if it doesn't exist
        new File("sounds").mkdirs();
    }
    
    /**
     * Plays the ghost eaten sound effect.
     */
    public void playGhostEaten() {
        if (!audioEnabled) return;
        playSoundEffect(GHOST_EATEN_FILE, this::generateGhostEatenSound);
    }
    
    /**
     * Plays the dot eaten sound effect.
     */
    public void playDotEaten() {
        if (!audioEnabled) return;
        playSoundEffect(DOT_EATEN_FILE, this::generateDotSound);
    }
    
    /**
     * Plays the power pellet sound effect.
     */
    public void playPowerPellet() {
        if (!audioEnabled) return;
        playSoundEffect(POWER_PELLET_FILE, this::generatePowerPelletSound);
    }
    
    /**
     * Plays the game over sound effect.
     */
    public void playGameOver() {
        if (!audioEnabled) return;
        playSoundEffect(GAME_OVER_FILE, this::generateGameOverSound);
    }
    
    /**
     * Plays the win sound effect.
     */
    public void playWin() {
        if (!audioEnabled) return;
        playSoundEffect(WIN_FILE, this::generateWinSound);
    }
    
    /**
     * Plays a sound effect from file, or generates it if file doesn't exist.
     */
    private void playSoundEffect(String filePath, Runnable generator) {
        new Thread(() -> {
            try {
                File soundFile = new File(filePath);
                if (soundFile.exists()) {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                } else {
                    generator.run();
                }
            } catch (Exception e) {
                // Try generated sound as fallback
                try {
                    generator.run();
                } catch (Exception ex) {
                    // Audio not available
                }
            }
        }).start();
    }
    
    /**
     * Generates a "ghost eaten" sound effect - ascending triumphant tone.
     */
    private void generateGhostEatenSound() {
        try {
            float sampleRate = 44100;
            int durationMs = 300;
            byte[] buffer = new byte[(int)(sampleRate * durationMs / 1000)];
            
            for (int i = 0; i < buffer.length; i++) {
                double time = i / sampleRate;
                double progress = (double)i / buffer.length;
                // Ascending frequency sweep
                double freq = 400 + progress * 800;
                double sample = Math.sin(2 * Math.PI * freq * time) * (1 - progress) * 0.5;
                // Add harmonic
                sample += Math.sin(2 * Math.PI * freq * 1.5 * time) * (1 - progress) * 0.25;
                buffer[i] = (byte)(sample * 127);
            }
            
            playBuffer(buffer, sampleRate);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Generates dot eaten sound - soft "waka" chirp.
     */
    private void generateDotSound() {
        try {
            float sampleRate = 44100;
            int durationMs = 60;
            byte[] buffer = new byte[(int)(sampleRate * durationMs / 1000)];
            
            for (int i = 0; i < buffer.length; i++) {
                double time = i / sampleRate;
                double progress = (double)i / buffer.length;
                // Quick ascending chirp - classic Pacman "waka" sound
                double freq = 200 + progress * 400;
                double envelope = Math.sin(Math.PI * progress); // Smooth envelope
                double sample = Math.sin(2 * Math.PI * freq * time) * envelope * 0.35;
                buffer[i] = (byte)(sample * 127);
            }
            
            playBuffer(buffer, sampleRate);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Generates power pellet sound - dramatic energizing sound.
     */
    private void generatePowerPelletSound() {
        try {
            float sampleRate = 44100;
            int durationMs = 400;
            byte[] buffer = new byte[(int)(sampleRate * durationMs / 1000)];
            
            for (int i = 0; i < buffer.length; i++) {
                double time = i / sampleRate;
                double progress = (double)i / buffer.length;
                
                // Rising powerful sound with multiple harmonics
                double baseFreq = 150 + progress * 500;
                double sample = Math.sin(2 * Math.PI * baseFreq * time) * 0.4;
                sample += Math.sin(2 * Math.PI * baseFreq * 1.5 * time) * 0.25;
                sample += Math.sin(2 * Math.PI * baseFreq * 2 * time) * 0.15;
                
                // Pulsating effect
                double pulse = 0.7 + 0.3 * Math.sin(2 * Math.PI * 20 * time);
                sample *= pulse;
                
                // Envelope: quick attack, sustained, quick release
                double envelope;
                if (progress < 0.1) envelope = progress * 10;
                else if (progress > 0.8) envelope = (1 - progress) * 5;
                else envelope = 1.0;
                
                buffer[i] = (byte)(sample * envelope * 127);
            }
            
            playBuffer(buffer, sampleRate);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Generates game over sound - descending sad tone.
     */
    private void generateGameOverSound() {
        try {
            float sampleRate = 44100;
            int durationMs = 1000;
            byte[] buffer = new byte[(int)(sampleRate * durationMs / 1000)];
            
            for (int i = 0; i < buffer.length; i++) {
                double time = i / sampleRate;
                double progress = (double)i / buffer.length;
                // Descending frequency
                double freq = 400 * Math.pow(0.5, progress);
                double sample = Math.sin(2 * Math.PI * freq * time) * (1 - progress) * 0.5;
                buffer[i] = (byte)(sample * 127);
            }
            
            playBuffer(buffer, sampleRate);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Generates win sound - ascending triumphant fanfare.
     */
    private void generateWinSound() {
        try {
            float sampleRate = 44100;
            int durationMs = 1500;
            byte[] buffer = new byte[(int)(sampleRate * durationMs / 1000)];
            
            for (int i = 0; i < buffer.length; i++) {
                double time = i / sampleRate;
                double progress = (double)i / buffer.length;
                
                // Three-note fanfare effect
                double freq;
                if (progress < 0.33) freq = 523; // C5
                else if (progress < 0.66) freq = 659; // E5
                else freq = 784; // G5
                
                double sample = Math.sin(2 * Math.PI * freq * time) * 0.4;
                sample += Math.sin(2 * Math.PI * freq * 2 * time) * 0.2;
                
                // Envelope
                double envelope = Math.min(1, Math.min(progress * 10, (1 - progress) * 3));
                buffer[i] = (byte)(sample * envelope * 127);
            }
            
            playBuffer(buffer, sampleRate);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Plays a raw audio buffer.
     */
    private void playBuffer(byte[] buffer, float sampleRate) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        
        if (AudioSystem.isLineSupported(info)) {
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, buffer, 0, buffer.length);
            clip.start();
        }
    }
    
    /**
     * Enables or disables all audio.
     */
    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled = enabled;
    }
    
    /**
     * Cleans up audio resources.
     */
    public void cleanup() {
        // No persistent audio to clean up
    }
}
