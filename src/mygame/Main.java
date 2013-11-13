package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Network;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends SimpleApplication implements ActionListener, AnalogListener {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private Node sceneNode;
    private BulletAppState bulletAppState;
    private RigidBodyControl scenePhy;
    private Node playerNode;
    private BetterCharacterControl playerControl;
    private CameraNode camNode;
    private Material boxMat;
    private Material selectedBoxMat;
    private int selected = -1;
    BitmapText fancyText;
    /* PLAYER LOCATION AND DIRECTION */
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private Vector3f viewDirection = new Vector3f(0, 0, 1);
    private boolean rotateLeft = false, rotateRight = false,
            forward = false, backward = false;
    private float speed = 8;
    private GamePlayAppState game;
    private UIAppState ui;
    private Factory factory;

    public void mapKeys() {

        /* KEY MAPPINGS */
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Rotate Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rotate Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Select", new MouseButtonTrigger(0)); // click
        inputManager.addMapping("Lift", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("rotateRight", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("rotateLeft", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("rotateUp", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("rotateDown", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("start", new KeyTrigger(KeyInput.KEY_RETURN));
    }

    public void addListeners() {
        /* INPUT LISTENERS */
        inputManager.addListener(this, "start");
        inputManager.addListener(this, "Rotate Left", "Rotate Right");
        inputManager.addListener(this, "Forward", "Back", "Jump");
        inputManager.addListener(this, "Select");
        inputManager.addListener(this, "Lift");
        inputManager.addListener(this, "Shoot");
        inputManager.addListener(this, "rotateRight", "rotateLeft", "rotateUp", "rotateDown");
    }

    public void createPlayer() {
        playerNode = (Node) assetManager.loadModel("Models/goblin.j3o");
        playerNode.setLocalTranslation(new Vector3f(10, 10, 10));
        rootNode.attachChild(playerNode);
    }

    public void applyPhysicsToPlayer() {
        playerControl = new BetterCharacterControl(1.5f, 4f, 30f);
        playerControl.setJumpForce(new Vector3f(0, 300, 0));
        playerControl.setGravity(new Vector3f(0, -9.81f, 0));
        playerNode.addControl(playerControl);
    }

    public void createTown() {
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneNode = (Node) assetManager.loadModel("main.scene");
        sceneNode.scale(1.5f);
        rootNode.attachChild(sceneNode);
    }

    public void initLight() {
        AmbientLight ambient = new AmbientLight();
        rootNode.addLight(ambient);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1.4f, -1.4f, -1.4f));
        rootNode.addLight(sun);
    }

    public void initMaterials() {
        boxMat = assetManager.loadMaterial("Materials/brick.j3m");
        selectedBoxMat = assetManager.loadMaterial("Materials/pebbles.j3m");
    }

    public void initCamera() {
        camNode = new CameraNode("CamNode", cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 4, 0));
        Quaternion quat = new Quaternion();
        quat.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        camNode.setLocalRotation(quat);
        playerNode.attachChild(camNode);
        camNode.setEnabled(true);
        inputManager.setCursorVisible(false);
        flyCam.setEnabled(false);
    }

    public void initGUI() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        fancyText = new BitmapText(guiFont);
        fancyText.setSize(guiFont.getCharSet().getRenderedSize());
        fancyText.move(settings.getWidth() / 2, fancyText.getLineHeight(), 0);
        guiNode.attachChild(fancyText);
    }

    public void applyPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        scenePhy = new RigidBodyControl(0f);
        sceneNode.addControl(scenePhy);

    }

    @Override
    public void simpleInitApp() {


        //setDisplayStatView(false);
        //setDisplayFps(false);

        inputManager.setCursorVisible(false);

        createTown();
        /* PHYSICS */
        applyPhysics();
        initGUI();
        mapKeys();
        addListeners();
        createPlayer();
        applyPhysicsToPlayer();

        initLight();
        initMaterials();
        //addCrates(10);
        attachCenterMark();

        factory = new Factory(assetManager, bulletAppState);
        ui = new UIAppState(guiNode, guiFont);
        game = new GamePlayAppState(rootNode, factory, playerNode);
        stateManager.attach(ui);

        bulletAppState.getPhysicsSpace().add(sceneNode);
        bulletAppState.getPhysicsSpace().add(playerControl);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.81f, 0));

        startGame(1); // start game with level 1
    }

    private void attachCenterMark() {
        Box mesh = new Box(Vector3f.ZERO, 1, 1, 1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Geometry geom = new Geometry("aim", mesh);
        geom.setMaterial(mat);
        geom.scale(4);
        geom.setLocalTranslation(settings.getWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(geom); // attach to 2D user interface
    }

    public void updateUI() {
        fancyText.setText("Health: ");
    }

    public void checkMovement() {
        Vector3f modelForwardDir = playerNode.getWorldRotation().mult(Vector3f.UNIT_Z);
        Vector3f modelLeftDir = playerNode.getWorldRotation().mult(Vector3f.UNIT_X);
        walkDirection.set(0, 0, 0);


        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(speed));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.mult(speed).negate());
        }

        if (rotateLeft) {
            walkDirection.addLocal(modelLeftDir.mult(speed));
        } else if (rotateRight) {
            walkDirection.addLocal(modelLeftDir.mult(speed).negate());
        }

        playerControl.setWalkDirection(walkDirection);
        playerControl.setViewDirection(viewDirection);
    }

    @Override
    public void simpleUpdate(float tpf) {
        initCamera();
        updateUI();
        checkMovement();


    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public void shootCannonBall() {
        Sphere ballMesh = new Sphere(32, 32, 0.25f, true, false);

        Geometry ballGeo = new Geometry("cannon ball", ballMesh);
        ballGeo.setMaterial(boxMat);
        ballGeo.setLocalTranslation(cam.getLocation().x, cam.getLocation().y + 1, cam.getLocation().z);
        rootNode.attachChild(ballGeo);

        RigidBodyControl ballPhy = new RigidBodyControl(5f);
        ballGeo.addControl(ballPhy);
        bulletAppState.getPhysicsSpace().add(ballPhy);
        ballPhy.setCcdSweptSphereRadius(.1f);
        ballPhy.setCcdMotionThreshold(0.001f);
        ballPhy.setLinearVelocity(cam.getDirection().mult(50));
    }

    public void onAction(String binding, boolean isPressed, float tpf) {
        if (stateManager.hasState(game)) {
            if (binding.equals("Shoot") && !isPressed) {
                shootCannonBall();
            }
            if (binding.equals("Rotate Left")) {
                rotateLeft = isPressed;
            } else if (binding.equals("Rotate Right")) {
                rotateRight = isPressed;
            } else if (binding.equals("Forward")) {
                forward = isPressed;
            } else if (binding.equals("Back")) {
                backward = isPressed;
            } else if (binding.equals("Jump")) {
                playerControl.jump();
            } else if (binding.equals("Select") && !isPressed) {

                if (selected != -1) {
                    Spatial prevBox = rootNode.getChild("box-" + selected);
                    prevBox.setMaterial(boxMat);
                    selected = -1;
                }

                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                rootNode.getChild("Box node").collideWith(ray, results);
                // Determine what the user selected
                if (results.size() > 0) {
                    CollisionResult closest = results.getClosestCollision();
                    selected = extractInt(closest.getGeometry().getName());
                    Spatial selectedBox = rootNode.getChild("box-" + selected);
                    selectedBox.setMaterial(selectedBoxMat);
                }
            }
        } else {
            if (binding.equals("start")) {
                if (game.isLastGameWon()) {
                    // if last game won, then next level
                    startGame(game.getLevel() + 1);
                } else {
                    // if last game lost, then restart from level 1

                    startGame(1);
                }
            }
        }
    }

    public static int extractInt(String str) {
        Matcher matcher = Pattern.compile("\\d+").matcher(str);

        if (!matcher.find()) {
            throw new NumberFormatException("For input string [" + str + "]");
        }

        return Integer.parseInt(matcher.group());
    }

    public void onAnalog(String name, float value, float tpf) {
        if (stateManager.hasState(game)) {
            if (name.equals("Lift")) {
                if (selected == -1) {
                    return;
                }
                Spatial selectedBox = rootNode.getChild("box-" + selected);
                if (selectedBox.getLocalTranslation().y < 10) {
                    selectedBox.getControl(RigidBodyControl.class).applyImpulse(new Vector3f(0, 10f, 0), new Vector3f(0, 0, 0));
                }
            }
            if (name.equals("rotateLeft")) {
                Quaternion rotateR = new Quaternion().fromAngleAxis(-FastMath.PI * tpf, Vector3f.UNIT_Y);
                rotateR.multLocal(viewDirection);
                playerControl.setViewDirection(viewDirection);

            }
            if (name.equals("rotateRight")) {
                Quaternion rotateR = new Quaternion().fromAngleAxis(FastMath.PI * tpf, Vector3f.UNIT_Y);
                rotateR.multLocal(viewDirection);
                playerControl.setViewDirection(viewDirection);
            }

            if (name.equals("rotateUp")) {
            }
            if (name.equals("rotateDown")) {
            }
            
            walkDirection.set(cam.getDirection()).normalizeLocal();
        }
    }

    private void startGame(int level) {
        game.setLevel(level);
        stateManager.attach(game);
        selected = -1;
    }
}
