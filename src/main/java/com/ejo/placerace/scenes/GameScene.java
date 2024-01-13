package com.ejo.placerace.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.util.input.Key;
import com.ejo.glowui.util.input.Mouse;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.placerace.elements.Player;
import com.ejo.placerace.util.RenderUtil;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
import com.ejo.uiphysics.elements.PhysicsSurfaceUI;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

public class GameScene extends Scene {

    private static final double FRICTION = 1;

    private final TitleScene.Mode mode;

    private double score;
    private boolean gameOver;
    private boolean placingPlatform;

    private final int platformSize;
    private final double baseSpeed;
    private final double speedIncrease;
    private final double baseCoolDown;
    private final int maxBarrierCount;

    private double speed;
    private double coolDown;

    private final Player player;
    private final PhysicsSurfaceUI basePlatform;

    private final StopWatch addBarrierWatch = new StopWatch();

    private final ButtonUI buttonRetry = new ButtonUI("Try Again", Vector.NULL, new Vector(200, 50), ColorE.RED, ButtonUI.MouseButton.LEFT, () -> {
        resetGame();
        System.out.println("Reset Game");
    });

    public GameScene(TitleScene.Mode mode) {
        super("PlaceRace");
        this.mode = mode;
        this.player = new Player(new RectangleUI(new Vector(60, 100), new Vector(10, 10), new ColorE(0, 200, 0)));
        this.basePlatform = new PhysicsSurfaceUI(new RectangleUI(new Vector(0, 1000), new Vector(1000, 1000), ColorE.WHITE), FRICTION, FRICTION);
        switch (getMode()) {
            case EASY -> {
                this.platformSize = 250;
                this.baseSpeed = .75;
                this.speedIncrease = .03;
                this.baseCoolDown = .8;
                this.maxBarrierCount = 2;
            }
            default -> { //Medium Difficulty
                this.platformSize = 150;
                this.baseSpeed = 1;
                this.speedIncrease = .06;
                this.baseCoolDown = 1;
                this.maxBarrierCount = 2;
            }
            case HARD -> {
                this.platformSize = 75;
                this.baseSpeed = 1.5;
                this.speedIncrease = .09;
                this.baseCoolDown = 2;
                this.maxBarrierCount = 3;
            }
        }

        addElements(buttonRetry);
        buttonRetry.setRendered(false);

        resetGame();
        getPlayer().reset();
        setUpPlatforms();
    }


    @Override
    public void draw() {
        //Draw Background
        new GradientRectangleUI(Vector.NULL, getSize(), new ColorE(0, 50, 128), ColorE.BLACK.alpha(0), GradientRectangleUI.Type.VERTICAL).draw();

        //Draw Elements
        super.draw();

        QuickDraw.drawText("Score: " + (int) score, Fonts.getDefaultFont(30), new Vector(2, 2), ColorE.WHITE);

        if (!isCooledDown()) RenderUtil.drawCoolDownWheel(baseCoolDown, coolDown, getWindow().getScaledMousePos());

        if (isPlacingPlatform()) RenderUtil.drawBlockPlaceOutline(platformSize, getWindow().getScaledMousePos());

        if (isGameOver()) {
            RenderUtil.drawGameOverMenu(score, getSize());
            buttonRetry.draw();
        }

    }

    @Override
    public void tick() {
        updatePlacePlatformCoolDown(.05);

        if (updateGameOver()) return;

        ArrayList<PhysicsSurfaceUI> surfaceList = new ArrayList<>();
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsSurfaceUI platform) {
                platform.updateCollisionObjects(getPhysicsObjects());
                platform.setVelocity(new Vector(-10 * speed, 0)); //Set Platform Velocity
                if (platform.getPos().getAdded(platform.getSize()).getX() < 0)
                    queueRemoveElements(platform); //Remove off screen elements
                surfaceList.add(platform);
            }
        }

        addBarriersProgressively();

        getPlayer().update(surfaceList.toArray(new PhysicsSurfaceUI[0]));

        super.tick();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if ((isGameOver() && key == Key.KEY_ENTER.getId()) || (key == Key.KEY_R.getId() && action == Key.ACTION_PRESS))
            buttonRetry.getAction().run();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        if (!isGameOver() && isCooledDown()) {
            if (!getPlayer().isMouseOver()) {
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
        queueAddElements(getPlayer());

        setUpPlatforms();
        getPlayer().reset();

        score = 0;
        coolDown = 0;
        placingPlatform = false;

        setGameOver(false);
    }

    private void setUpPlatforms() {
        basePlatform.setPos(new Vector(0, 500));
        speed = baseSpeed;
    }

    private boolean updateGameOver() {
        if (player.getPos().getX() < 0 || player.getPos().getY() > getWindow().getScaledSize().getY())
            setGameOver(true);

        if (isGameOver()) {
            placingPlatform = false;
            buttonRetry.setPos(getWindow().getScaledSize().getMultiplied(.5).getAdded(buttonRetry.getSize().getMultiplied(-.5)));
            buttonRetry.setPos(buttonRetry.getPos().getAdded(new Vector(0, 100)));
            buttonRetry.setEnabled(true);
            buttonRetry.tick(this, getWindow().getScaledMousePos());
            return true;
        } else {
            buttonRetry.setEnabled(false);
            return false;
        }
    }

    private void updatePlacePlatformCoolDown(double speed) {
        coolDown -= speed;
        coolDown = Math.max(0, coolDown);
    }

    private void placePlatform(Vector mousePos) {
        try {
            coolDown = baseCoolDown; //Set CoolDown
            PhysicsSurfaceUI surface = new PhysicsSurfaceUI(new RectangleUI(Vector.NULL, new Vector(platformSize, 25), ColorE.WHITE), FRICTION * 1.1, FRICTION);
            surface.setCenter(mousePos);
            surface.setDeltaT(.1f);
            queueAddElements(surface);
            setPlayerLastInList();
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
    }


    private void addBarriersProgressively() {
        if (!addBarrierWatch.isStarted()) addBarrierWatch.start();
        Random random = new Random();
        if (addBarrierWatch.hasTimePassedS(2 / speed + .5)) {
            for (int i = 0; i <= random.nextInt(0, maxBarrierCount); i++) {
                PhysicsSurfaceUI surface;
                queueAddElements(surface = new PhysicsSurfaceUI(new RectangleUI(Vector.NULL, new Vector(15, random.nextInt(25, 100)), ColorE.WHITE), FRICTION, FRICTION));
                surface.setPos(new Vector(getWindow().getScaledSize().getX(), random.nextInt(0, (int) getWindow().getScaledSize().getY())));
                setPlayerLastInList();
            }
            addBarrierWatch.restart();
            score++;
            speed += speedIncrease;
        }
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
        return coolDown == 0;
    }

    public TitleScene.Mode getMode() {
        return mode;
    }

    public Player getPlayer() {
        return player;
    }

    public ArrayList<PhysicsObjectUI> getPhysicsObjects() {
        ArrayList<PhysicsObjectUI> phy = new ArrayList<>();
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI physicsObjectUI && !(element instanceof PhysicsSurfaceUI))
                phy.add(physicsObjectUI);
        }
        return phy;
    }

}
