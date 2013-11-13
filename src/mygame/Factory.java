package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;


public final class Factory {

    // Geometry size ratios 
    public static final float CREEP_RADIUS = 0.3f;
    // assetmanager
    private AssetManager assetManager;
    // materials
    private Material creep_mat;
    private Material boxMat;
    private Material playerbase_mat;
    private BulletAppState bulletAppState;

    public Factory(AssetManager as, BulletAppState bulletAppState) {
        this.assetManager = as;
        this.bulletAppState = bulletAppState;
        initMaterials();
    }

    public Geometry makeBox(Vector3f loc, int index) {
        Box boxMesh = new Box(Vector3f.ZERO, 3, 3, 3);
        Geometry boxGeo = new Geometry("box-" + index, boxMesh);
        boxGeo.move(loc);

        boxGeo.setMaterial(boxMat);

        /* Set physics */
        RigidBodyControl boxPhy = new RigidBodyControl(5f);
        boxGeo.addControl(boxPhy);
        bulletAppState.getPhysicsSpace().add(boxPhy);

        return boxGeo;


    }

    public Node makeCreep(Vector3f loc, int index, Vector3f dir) {
        Node creep = (Node) assetManager.loadModel("Models/spider.j3o");
       //creep.scale(0.1f);
        creep.setLocalTranslation(loc);
        creep.rotate(0, FastMath.DEG_TO_RAD * 180, 0);
        
        BetterCharacterControl creepControl = new BetterCharacterControl(1.5f, 4f, 30f);
        creepControl.setJumpForce(new Vector3f(0, 300, 0));
        creepControl.setGravity(new Vector3f(0, -9.81f, 0));
        bulletAppState.getPhysicsSpace().add(creepControl);
        creep.addControl(creepControl);

        return creep;
    }

    /**
     * ---------------------------------------------------------
     */
    private void initMaterials() {
        // creep material
        creep_mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        creep_mat.setColor("Diffuse", ColorRGBA.Black);
        creep_mat.setColor("Ambient", ColorRGBA.Black);
        creep_mat.setBoolean("UseMaterialColors", true);
        // floor material
        boxMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        boxMat.setColor("Diffuse", ColorRGBA.Orange);
        boxMat.setColor("Ambient", ColorRGBA.Orange);
        boxMat.setBoolean("UseMaterialColors", true);
        // player material
        playerbase_mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        playerbase_mat.setColor("Diffuse", ColorRGBA.Yellow);
        playerbase_mat.setColor("Ambient", ColorRGBA.Yellow);
        playerbase_mat.setBoolean("UseMaterialColors", true);
        // tower SelectedMaterial
    }
}