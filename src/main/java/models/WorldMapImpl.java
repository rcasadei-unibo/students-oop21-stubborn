package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * WorldMapImpl is a class that implements WorldMap and its contracts.
 * It generates the actual game map, setting its boundaries,
 * the number of entities that it contains, what kind of collision and stategy of spawning
 * will use, etc
 */
public class WorldMapImpl implements WorldMap{

    private int board_width;
    private int board_height;
    private int num_enemies;
    private int num_collectables;
    private Map<Point2D,Optional<Entity>> board;
    private Point2D playerPosition;
    private SpawnStrategy spawnStrategy;
    private CollisionStrategy collisionStrategy;
    
    /**
     * This is the constructor of WorldMapImpl. It creates at first an empty Map the represents
     * the game map, it then calls another private method spawnEntity() to fill it 
     * with number of entities required
     * 
     * @param The width of the map
     * @param The height of the map
     * @param The number of enemies to spawn at the start
     * @param The number of enemies to spawn at the start
     * @param The strategy that the class will use to spawn the entities
     */
    public WorldMapImpl(int width, int height, int enemies, int collectables, SpawnStrategy strategy) {
        this.board_width = width;
        this.board_height = height;
        this.num_enemies = enemies;
        this.num_collectables = collectables;
        this.spawnStrategy = strategy;
        this.collisionStrategy = new CollisionImpl();
        this.playerPosition = new Point2D(board_width/2, board_height/2);
        this.board = IntStream.range(0, board_width).boxed()
                    .flatMap(x -> IntStream.range(0, board_height).boxed()
                    .map(y -> new Point2D(x,y))).collect(Collectors.toMap(x -> x, x -> Optional.empty()));
        this.board.put(this.playerPosition, Optional.of(new PlayerImpl(this.playerPosition, 3)));
        this.spawnEntity();
    }
    
    /*
     * pattern strategy utilized in order to spawn each entity in the worldmap at the start of the game.
     * we decided to use a RandomSpawnStrategy to create random possible positions where our entities
     * will be instantiated. It internally uses a 2-way combinator pattern: at first, it generates
     * a Set of positions for the enemies, then for the collectables; after that, it combines
     * this two sets and checks if there are duplicates, regenerating new positions if so.
     */
    /**
     * Simple method used to spawn entities. It applies methods contained in SpawnStrategy to get 
     * the number of positions required for enemies and collectables, avoiding duplicates.
     * It then puts in board pairs key-value (position of entity, new Entity of a specific type). 
     */
    private void spawnEntity() {
        if(this.spawnStrategy.checkNumPoints(this.board_width * this.board_height, this.num_enemies + this.num_collectables)) {
            Set<Point2D> enSpawnPoints = this.spawnStrategy.getSpawnPoints(this.board_width, this.board_height, this.num_enemies);
            Set<Point2D> collectSpawnPoints = this.spawnStrategy.getSpawnPoints(this.board_width, this.board_height, this.num_collectables);
            Set<Point2D> everyPoint = this.spawnStrategy.getDoubleSpawnPoints(this.board_width, this.board_height, enSpawnPoints, collectSpawnPoints);
            Iterator<Point2D> pointIterator = everyPoint.iterator();
            for(int i = 0; i < this.num_enemies; i ++) {
                Point2D enemyPos = pointIterator.next();
                this.board.put(enemyPos, Optional.of(new EnemyImpl(enemyPos, 1, this.getEnemyAi())));
            }
            for(int i = this.num_enemies; i < (this.num_enemies + this.num_collectables); i ++) {
                this.board.put(pointIterator.next(), Optional.of(new CollectableImpl()));
            }
        }
    }
    
    /**
     * This is the logical method used to move the player character.
     * Before moving the player, this method also calls private void moveEnemies(). After that,
     * it checks if there is a possible collision with an enemy or the boundaries of the map.
     * This is done thanks to checkCollisions() method from Collision class.
     * If there is no collision, it moves the player by one tile in the map in a given direction.
     * 
     * @param Movement for the player based on key typed by the user
     */
    @Override
    public void movePlayer(MOVEMENT movement) {
        this.moveEnemies();
        Point2D newPos = Point2D.sum(this.playerPosition, movement.movement);
        if(!this.collisionStrategy.checkCollisions(this.getBoard(), newPos, this.board_width, this.board_height)){
            Entity player = this.board.replace(this.playerPosition, Optional.empty()).get();
            this.playerPosition = newPos;
            player.setPosition(this.playerPosition);
            this.board.put(this.playerPosition, Optional.of(player));
        }
    }
    
    /**
     * Moves each Enemy by one tile based on movement given its Ai. For each enemy,it checks 
     * at first using methods from Collision class if said movement is possible; if it is,
     * it moves said enemy.
     */
    private void moveEnemies() {
        List<Pair<Point2D,Class<? extends Entity>>> entitiesPos = this.getEntitiesPos();
        entitiesPos.removeIf(el -> !el.getY().equals(EnemyImpl.class));
        if(entitiesPos.size() > 0) {
            for(Pair<Point2D,Class<? extends Entity>> enemy : entitiesPos) {
                Enemy en = (Enemy) this.board.get(enemy.getX()).get();
                Point2D newPos = en.getAi().move(this.getBoard(), this.playerPosition, enemy.getX());
                if(!this.collisionStrategy.checkCollisions(this.getBoard(), newPos, this.board_width, this.board_height)) {
                    this.board.replace(enemy.getX(), Optional.empty());
                    en.setPosition(newPos);
                    this.board.put(newPos, Optional.of(en));
                }
            }
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Point2D,Optional<Entity>> getBoard() {
        return this.board;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Point2D getPlayerPos() {
        return this.playerPosition;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pair<Point2D,Class<? extends Entity>>> getEntitiesPos() {
        List<Pair<Point2D,Class<? extends Entity>>> entitiesPos = new ArrayList<>();
        for(Entry<Point2D, Optional<Entity>> i : this.board.entrySet()) {
            if(i.getValue().isPresent() && !i.getKey().equals(this.getPlayerPos())) {
                entitiesPos.add(new Pair<>(i.getKey(), i.getValue().get().getClass()));
            }
        }
        return entitiesPos;
    }
    
    /**
     * Set one of the possible AIs to an enemy
     * 
     * @return One possible Ai for the Enemy
     */
    private AiEnemy getEnemyAi() {
        Random r = new Random();
        int randomSelect = r.nextInt(2);
        return randomSelect == 0 ? new RandomAiEnemy() : new FocusAiEnemy();
    }       
}
