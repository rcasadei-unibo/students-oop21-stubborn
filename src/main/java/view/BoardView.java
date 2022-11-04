package view;

import java.util.List;

import models.Entity;
import models.Pair;
import models.Point2D;

/**
 * BoardView is the graphical interface counterpart to the WorldMap containted in models package.
 * As such, its goal is to recreate a graphic game map based on data offered by its model
 */
public interface BoardView {
    
    /**
     * Initialize the view, generating the necessary graphical map based on model data
     * 
     * @param the initial position of the player in model map
     * @param the initial position of all the other entities and their types in model map
     */
    public void initializeView(Point2D playerPos, List<Pair<Point2D,Class<? extends Entity>>> allEntities);
    
    /**
     * Update the view based on movement that just occurred
     * 
     * @param the current position of the player in model map
     * @param the current number of entities remaining
     * @param the current position of all the other entities and their types in model mal
     */
    public void updateWorldMap(Point2D playerPos, int numEntitiesRemaining, List<Pair<Point2D,Class<? extends Entity>>> allEntities);
    
    /**
     * Send user to main menu after a game over
     */
    public void gameOver();
    
   
}
