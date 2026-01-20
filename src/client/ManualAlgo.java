package client;

import server.PacManAlgo;
import server.PacmanGame;

public class ManualAlgo implements server.PacManAlgo {
    public ManualAlgo() {;}
    @Override
    public String getInfo() {
        return "This is a manual algorithm for manual controlling the PacMan using w,a,x,d (up,left,down,right).";
    }

    @Override
    public int move(server.PacmanGame game) {
        int ans = server.PacmanGame.ERR;
        Character cmd = Ex3Main.getCMD();
            if (cmd != null) {
                if (cmd == 'w') {ans = server.PacmanGame.UP;}
                if (cmd == 'x') {ans = server.PacmanGame.DOWN;}
                if (cmd == 'a') {ans = server.PacmanGame.LEFT;}
                if (cmd == 'd') {ans = server.PacmanGame.RIGHT;}
            }
            return  ans;
    }
}
