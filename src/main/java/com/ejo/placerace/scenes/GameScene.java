package com.ejo.placerace.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowlib.util.NumberUtil;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.util.Key;
import com.ejo.glowui.util.Mouse;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.placerace.elements.Player;
import com.ejo.placerace.util.RenderUtil;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
import com.ejo.uiphysics.elements.PhysicsSurfaceUI;
import com.ejo.uiphysics.util.GravityUtil;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

public class GameScene extends Scene {

    private final TitleScene.Mode mode;

    private double score;
    private boolean gameOver;
    private boolean placingPlatform;

    private final double friction = 1;

    //TODO: change these variables based off of difficulty
    private final int platformSize = 300;//1000; Easy 1000, Normal 300, Hard 100
    private final double baseSpeed = 2;
    private final double speedIncrease = .12;
    private final double baseCoolDown = 1;

    private double speed;
    private double currentCoolDown = 0;

    private final Player player = new Player(new RectangleUI(new Vector(60,100),new Vector(20,20), new ColorE(0,200,0)));
    private final PhysicsSurfaceUI basePlatform = new PhysicsSurfaceUI(new Vector(0,1000),new Vector(1000,1000),ColorE.WHITE, friction, friction);

    private final StopWatch barrierWatch = new StopWatch();

    private final ButtonUI retryButton = new ButtonUI("Try Again",Vector.NULL,new Vector(500,100),ColorE.RED, ButtonUI.MouseButton.LEFT,() -> {
        resetGame();
        System.out.println("Reset Game");
    });

    public GameScene(TitleScene.Mode mode) {
        super("PlaceRace");
        this.mode = mode;
        addElements(retryButton);
        resetGame();
        setUpPlayer();
        setUpPlatforms();
        retryButton.setRendered(false);
    }


    @Override
    public void draw() {
        //Draw Background
        new GradientRectangleUI(Vector.NULL,getSize(),new ColorE(0,50, 128),ColorE.BLACK.alpha(0), GradientRectangleUI.Type.VERTICAL).draw();

        //Draw Elements
        super.draw();

        QuickDraw.drawText("Score: " + (int)score, Fonts.getDefaultFont(30),new Vector(2,2),ColorE.WHITE);

        if (!isCooledDown()) RenderUtil.drawCoolDownWheel(baseCoolDown, currentCoolDown,getWindow().getScaledMousePos());

        if (isPlacingPlatform()) RenderUtil.drawBlockPlaceOutline(platformSize,getWindow().getScaledMousePos());

        if (isGameOver()) {
            RenderUtil.drawGameOverMenu(score,getSize());
            retryButton.draw();
        }

    }

    @Override
    public void tick() {
        updatePlatformCoolDown(.05);

        if (updateGameOver()) return;

        double platformXVelocity = -10 * speed;
        player.setOnGround(false);

        //Update Collision, isOnPlatform, and Velocity of PhysicsSurface Platforms
        try {
            for (ElementUI element : getElements()) {
                if (element instanceof PhysicsSurfaceUI platform) {
                    platform.updateCollisionObjects(getPhysicsObjects());
                    platform.setVelocity(new Vector(platformXVelocity, 0)); //Set Platform Velocity
                    if (platform.isCollidingTop(player, 20, 20)) player.setOnGround(true);
                    if (platform.getPos().getAdded(platform.getSize()).getX() < 0) queueRemoveElements(platform); //Remove off screen elements
                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }

        addBarriersProgressively();

        applyGravity(GravityUtil.g * 4);
        applyControls();

        //Maximum Velocity
        double bound = 50;
        player.setVelocity(new Vector((Double) NumberUtil.getBoundValue(player.getVelocity().getX(), -bound + platformXVelocity, bound), player.getVelocity().getY()));
        super.tick();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if ((isGameOver() && key == Key.KEY_ENTER.getId()) || (key == Key.KEY_R.getId() && action == Key.ACTION_PRESS))
            retryButton.getAction().run();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        if (!isGameOver() && isCooledDown()) {
            if (!player.isMouseOver()) {
                if (action == Mouse.ACTION_CLICK) setPlacingPlatform(true);
                if (button == Mouse.BUTTON_LEFT.getId() && action == Mouse.ACTION_RELEASE) placePlatform(mousePos);
            }
            if (action == Mouse.ACTION_RELEASE) setPlacingPlatform(false);
        }
        super.onMouseClick(button, action, mods, mousePos);
    }


    private void resetGame() {
        for (ElementUI element : getElements()) if (element instanceof PhysicsObjectUI p) queueRemoveElements(p);
        queueAddElements(basePlatform);
        queueAddElements(player);

        setUpPlatforms();
        setUpPlayer();

        score = 0;
        placingPlatform = false;

        setGameOver(false);
    }

    private void setUpPlayer() {
        player.setDeltaT(.1f);
        player.setPos(new Vector(60,100));
        player.setVelocity(new Vector(100,0));

        player.setTickNetRecalculation(true);

        player.setDebugVectorCap(1000);
        player.setDebugVectorForceScale(.5);
    }

    private void setUpPlatforms() {
        basePlatform.setPos(new Vector(0,1000));
        speed = baseSpeed;
    }


    private boolean updateGameOver() {
        if (player.getPos().getX() < 0 || player.getPos().getY() > getWindow().getScaledSize().getY()) setGameOver(true);

        if (isGameOver()) {
            placingPlatform = false;
            retryButton.setPos(getWindow().getScaledSize().getMultiplied(.5).getAdded(retryButton.getSize().getMultiplied(-.5)));
            retryButton.setPos(retryButton.getPos().getAdded(new Vector(0,200)));
            retryButton.setEnabled(true);
            retryButton.tick(this,getWindow().getScaledMousePos());
            return true;
        } else {
            retryButton.setEnabled(false);
            return false;
        }
    }

    private void updatePlatformCoolDown(double speed) {
        currentCoolDown -= speed;
        currentCoolDown = Math.max(0, currentCoolDown);
    }

    private boolean placePlatform(Vector mousePos) {
        try {
            currentCoolDown = baseCoolDown; //Set CoolDown
            PhysicsSurfaceUI surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(platformSize, 50), ColorE.WHITE, friction * 1.1, friction);
            surface.setCenter(mousePos);
            surface.setDeltaT(.1f);
            queueAddElements(surface);
            setPlayerLastInList();
            return true;
        } catch (ConcurrentModificationException ignored) {
            return false;
        }
    }


    private void addBarriersProgressively() {
        if (!barrierWatch.isStarted()) barrierWatch.start();
        Random random = new Random();
        if (barrierWatch.hasTimePassedS(2/ speed + .5)) {
            for (int i = 0; i < random.nextInt(0, 3); i++) {
                PhysicsSurfaceUI surface;
                queueAddElements(surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(25, random.nextInt(50,200)), ColorE.WHITE, friction, friction));
                surface.setPos(new Vector(getWindow().getScaledSize().getX(), random.nextInt(0, (int) getWindow().getScaledSize().getY())));
                setPlayerLastInList();
            }
            barrierWatch.restart();
            score++;
            speed += speedIncrease;
        }
    }


    private void applyGravity(double g) {
        player.setNetForce(player.getNetForce().getAdded(GravityUtil.calculateSurfaceGravity(g, player)));
    }

    private void applyControls() {
        //Jump
        boolean isJumpKeyDown = Key.KEY_SPACE.isKeyDown() || Key.KEY_UP.isKeyDown() || Key.KEY_W.isKeyDown();
        if (player.isOnGround() && player.getVelocity().getY() == 0 && isJumpKeyDown)
            player.setNetForce(player.getNetForce().getAdded(new Vector(0, -10000)));

        //Move
        double groundForce = 600;
        double airForce = groundForce/5;
        double force = player.isOnGround() ? groundForce : airForce;

        boolean isRightKeyDown = Key.KEY_RIGHT.isKeyDown() || Key.KEY_D.isKeyDown();
        boolean isLeftKeyDown = Key.KEY_LEFT.isKeyDown() || Key.KEY_A.isKeyDown();

        player.setNetForce(player.getNetForce().getAdded(isRightKeyDown ? force : isLeftKeyDown ? -force : 0, 0));
    }


    private void setPlayerLastInList() {
        queueRemoveElements(player);
        queueAddElements(player);
    }


    public void setPlacingPlatform(boolean placingPlatform) {
        this.placingPlatform = placingPlatform;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }



    public boolean isPlacingPlatform() {
        return placingPlatform;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isCooledDown() {
        return currentCoolDown == 0;
    }

    public TitleScene.Mode getMode() {
        return mode;
    }


    public ArrayList<PhysicsObjectUI> getPhysicsObjects() {
        ArrayList<PhysicsObjectUI> phy = new ArrayList<>();
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI physicsObjectUI && !(element instanceof PhysicsSurfaceUI)) phy.add(physicsObjectUI);
        }
        return phy;
    }



}
