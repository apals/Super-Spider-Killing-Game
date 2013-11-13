package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

public class CreepControl extends AbstractControl implements Savable, Cloneable {

    private final float speed_min = 0.5f;
    private GamePlayAppState game;
    private AnimControl control;
    private AnimChannel channel;
    private static final String ANI_WALK = "Walk_0";
    

    public CreepControl(GamePlayAppState game) {
        this.game = game;

    }

    @Override
    protected void controlUpdate(float tpf) {
        control = spatial.getControl(AnimControl.class);
        channel = control.createChannel();
        channel.setAnim(ANI_WALK);

        Vector3f dir = new Vector3f(game.getPlayerNode().getLocalTranslation().subtract(spatial.getLocalTranslation()));
        dir = dir.multLocal(0.1f);
        spatial.getControl(BetterCharacterControl.class).setWalkDirection(dir);
        spatial.getControl(BetterCharacterControl.class).setViewDirection(dir);
        
        

    }

    /**
     * ----------------------------------------------
     */
    public void setHealth(float h) {
        spatial.setUserData("health", h);
    }

    public float getHealth() {
        return (Float) spatial.getUserData("health");
    }

    public Boolean isAlive() {
        return getHealth() > 0f;
    }

    /**
     * @param mod (typically) a negative number by how much to decrease the
     * creep's health.
     */
    public void addHealth(float mod) {
        spatial.setUserData("health", getHealth() + mod);
    }

    /**
     * @param mod (typically) a negative number by how much to decrease the
     * creep's speed.
     */
    public void addSpeed(float mod) {
        spatial.setUserData("speed", getSpeed() + mod);
        if (getSpeed() < speed_min) {
            spatial.setUserData("speed", speed_min);
        }
    }

    public float getSpeed() {
        return (Float) spatial.getUserData("speed");
    }

    /**
     * ----------------------------------------------
     */
    public void setLoc(Vector3f loc) {
        spatial.setLocalTranslation(loc);
    }

    public Vector3f getLoc() {
        return spatial.getLocalTranslation();
    }

    /**
     * ----------------------------------------------
     */
    public int getIndex() {
        return (Integer) spatial.getUserData("index");
    }

    public void remove() {
        spatial.removeFromParent();
        spatial.removeControl(this);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    /**
     * ----------------------------------------------
     */
}