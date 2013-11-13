package mygame;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.Random;

public class GamePlayAppState extends AbstractAppState implements PhysicsCollisionListener {

    private AppStateManager stateManager;
    private final Node rootNode;
    // nodes
    private final Node creepNode;
    private final Node boxNode;
    private Node sceneNode;
    private Node playerNode;
    // Factory creates creeps, towers
    Factory factory;
    // timers reset laser beam visualizations and dispense player budget
    private float timer_beam = 0f;
    private float timer_budget = 0f;
    // player control manages player health and budget
    private int level = 0;
    private int score = 0;
    private float health = 0;
    private int budget = 0;
    private boolean lastGameWon = false;
    private int CREEP_INIT_NUM;
    private int TOWER_INIT_NUM;
    private float CREEP_INIT_HEALTH;
    private float CREEP_INIT_SPEED;
    
    Random rand = new Random();

    public GamePlayAppState(Node rootNode, Factory factory, Node playerNode) {
        this.rootNode = rootNode;
        this.factory = factory;
        this.playerNode = playerNode;
        creepNode = new Node("CreepNode");
        boxNode = new Node("Box node");
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;

        stateManager.getState(BulletAppState.class).getPhysicsSpace().addCollisionListener(this);
        // configurable factors depend on level
        this.budget = 5 + level * 2;
        this.health = 2f + level;
        this.CREEP_INIT_NUM = 2 + level * 2;
        this.TOWER_INIT_NUM = 4 + level / 2;
        this.CREEP_INIT_HEALTH = 20f + level * 2;
        this.CREEP_INIT_SPEED = 0.8f + level / 10;
        rootNode.attachChild(creepNode);
        rootNode.attachChild(boxNode);

        addCreeps();
        //addCrates(10);
    }

    @Override
    public void cleanup() {
        creepNode.detachAllChildren();
        boxNode.detachAllChildren();
        rootNode.detachChild(creepNode);
        super.cleanup();
    }

    /**
     * ---------------------------------------------------------
     */
    /**
     * Towers stand in two rows to the left and right of the positive z axis
     * along the "tower-protected valley". They shoot beams at creeps.
     */
    public void addCrates(int numberOfCrates) {
        Geometry crate = null;

        for (int i = 0; i < numberOfCrates; i++) {
            crate = factory.makeBox(new Vector3f(3 + i * 6, 10, -3 + 2 * i), i);
            crate.setUserData("index", i);
            boxNode.attachChild(crate);
        }
    }

    private void addCreeps() {
        // generate a pack of creesp
        for (int index = 0; index < CREEP_INIT_NUM; index++) {
            // distribute creeps to the left and right of the positive x axis
            int leftOrRight = (index % 2 == 0 ? 1 : -1); // +1 or -1
            float offset_x = 1.75f * leftOrRight * FastMath.rand.nextFloat();
            float offset_y = 0;
            float offset_z = 2.5f * ((TOWER_INIT_NUM / 2f) + 6f);
            Vector3f spawnloc = new Vector3f(offset_x, offset_y, offset_z);
            // creep geometry
            spawnloc = new Vector3f(-index * 10 + 20, 5, -index*6 + 20);

            /*while (spawnloc.distance(getPlayerNode().getLocalTranslation()) < 10)  {
                spawnloc = new Vector3f(3*rand.nextFloat() * 20 + index, 10 * rand.nextFloat() * 20, 4 * rand.nextFloat() * 20 + index * 10);
            }*/


            Node creep_geo = factory.makeCreep(spawnloc, index, playerNode.getLocalTranslation());

            creep_geo.setUserData("index", index);
            creep_geo.setUserData("health", CREEP_INIT_HEALTH);
            creep_geo.setUserData("speed", CREEP_INIT_SPEED);
            creep_geo.addControl(new CreepControl(this));
            creepNode.attachChild(creep_geo);
        }
    }

    public void setLevel(int level) {
        creepNode.detachAllChildren();
        
        for(int i = -1; rootNode.detachChildNamed("cannon ball") != -1;) {
            System.out.println(rootNode.getChildren().size());
        }
        
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Modifies the player score by adding to it.
     *
     * @param mod is (typically) a positive value added to the player score.
     */
    public void addScoreMod(int mod) {
        score += mod;
    }

    public float getHealth() {
        return Math.round(health * 10) / 10; // drop the decimals
    }

    /**
     * How many creeps are still in the game?
     */
    public int getCreepNum() {
        return creepNode.getChildren().size();
    }

    public List<Spatial> getCreeps() {
        return creepNode.getChildren();
    }

    public Node getPlayerNode() {
        return playerNode;
    }

    public boolean isLastGameWon() {
        return lastGameWon;
    }

    @Override
    public void update(float tpf) {
        // Test whether player wins or loses
        if (getHealth() <= 0) {
            
            creepNode.detachAllChildren();
            lastGameWon = false;
            stateManager.detach(this);
            getPlayerNode().getControl(BetterCharacterControl.class).setWalkDirection(new Vector3f(0, 0, 0));
            

        } else if ((getCreepNum() == 0) && getHealth() > 0) {
            creepNode.detachAllChildren();
            lastGameWon = true;
            stateManager.detach(this);
            getPlayerNode().getControl(BetterCharacterControl.class).setWalkDirection(new Vector3f(0, 0, 0));
        }

    }

    public void collision(PhysicsCollisionEvent event) {

        /* PLAYER WAS ATTACKED BY SPIDER */
        if ((event.getNodeA().getName().contains("goblin-ogremesh")
                && event.getNodeB().getName().contains("spider-ogremesh"))
                || (event.getNodeA().getName().contains("spider-ogremesh")
                && event.getNodeB().getName().contains("goblin-ogremesh"))) {
            health = health - 1;
            
        } 
        
        /* A SPIDER WAS SHOT */ 
        else if ((event.getNodeA().getName().contains("spider-ogremesh")
                && event.getNodeB().getName().equals("cannon ball"))) {
            creepNode.detachChild(event.getNodeA());
            rootNode.detachChild(event.getNodeB());
        } else if ((event.getNodeB().getName().contains("spider-ogremesh")
                && event.getNodeA().getName().equals("cannon ball"))) {
            creepNode.detachChild(event.getNodeB());
            rootNode.detachChild(event.getNodeA());
        }

    }
}