package com.ejo.placerace;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowlib.util.NumberUtil;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.util.*;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.placerace.elements.Player;
import com.ejo.uiphysics.elements.PhysicsDraggableUI;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
import com.ejo.uiphysics.elements.PhysicsSurfaceUI;
import com.ejo.uiphysics.util.GravityUtil;
import com.ejo.uiphysics.util.VectorUtil;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

public class GameScene extends Scene {

    private boolean gameOver = false;
    private boolean placingBlock = false;

    private final double friction = 1;
    double speed = 1;
    private double score = 0;

    private final Player player = new Player(new RectangleUI(new Vector(60,100),new Vector(20,20), new ColorE(0,200,0)));
    private final PhysicsSurfaceUI basePlatform = new PhysicsSurfaceUI(new Vector(0,1000),new Vector(1000,1000),ColorE.WHITE, friction, friction);

    private final StopWatch watch = new StopWatch();

    private final ButtonUI retryButton = new ButtonUI("Try Again",Vector.NULL,new Vector(500,100),ColorE.RED, ButtonUI.MouseButton.LEFT,() -> {
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI p) queueRemoveElements(p);
        }
        gameOver = false;
        speed = 1;
        score = 0;
        player.setPos(new Vector(60,100));
        player.setVelocity(new Vector(100,0));
        basePlatform.setPos(new Vector(0,1000));

        queueAddElements(basePlatform);
        queueAddElements(player);
    });

    public GameScene() {
        super("PlaceRace");
        addElements(retryButton);
        addElements(basePlatform); //Add Collision Objects
        addElements(player); // Add Physics Objects
        player.setDeltaT(.1);
        player.setVelocity(new Vector(100,0));
        retryButton.setRendered(false);
    }

    @Override
    public void draw() {
        try {
            super.draw();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        QuickDraw.drawText("Score: " + (int)score, Fonts.getDefaultFont(30),new Vector(2,2),ColorE.WHITE);

        if (gameOver) {
            placingBlock = false;
            QuickDraw.drawRect(Vector.NULL,getWindow().getScaledSize(),ColorE.RED.alpha(50));
            QuickDraw.drawTextCentered("Game Over",Fonts.getDefaultFont(100),Vector.NULL,getWindow().getScaledSize(),ColorE.RED);
            QuickDraw.drawTextCentered("Score: " + (int)score,Fonts.getDefaultFont(50),new Vector(0,100),getWindow().getScaledSize(),ColorE.RED);
            retryButton.draw();
            retryButton.setRendered(true);
            retryButton.setPos(getWindow().getScaledSize().getMultiplied(.5).getAdded(retryButton.getSize().getMultiplied(-.5)));
            retryButton.setPos(retryButton.getPos().getAdded(new Vector(0,200)));
        } else {
            retryButton.setRendered(false);
        }

        //Draw Forces and Velocities
        if (getWindow().isDebug()) {
            LineUI lineUI = new LineUI(player.getCenter(), VectorUtil.getUIAngleFromVector(player.getNetForce()), player.getNetForce().getMagnitude() / 2, ColorE.BLUE, LineUI.Type.PLAIN, 4);
            lineUI.draw();

            LineUI lineUI2 = new LineUI(player.getCenter(), VectorUtil.getUIAngleFromVector(player.getVelocity()), player.getVelocity().getMagnitude(), ColorE.RED, LineUI.Type.PLAIN, 2);
            lineUI2.draw();
        }

        if (placingBlock) {
            RectangleUI rect = new RectangleUI(Vector.NULL,new Vector(300,50),true,3,ColorE.BLUE);
            rect.setCenter(getWindow().getScaledMousePos());
            rect.draw();
        }
    }

    @Override
    public void tick() {
        if (!watch.isStarted()) watch.start();

        //Reset net force every frame for recalculation
        player.setNetForce(Vector.NULL);

        if (player.getPos().getX() < 0 || player.getPos().getY() > getWindow().getScaledSize().getY()) {
            gameOver = true;
        }

        if (gameOver) {
            retryButton.setTicking(true);
            retryButton.tick(this,getWindow().getScaledMousePos());
            return;
        } else retryButton.setTicking(false);

        double platformXVelocity = -10 * speed;
        boolean isOnPlatform = false;

        //Update Collision, isOnPlatform, and Velocity of PhysicsSurface Platforms
        try {
            for (ElementUI element : getElements()) {
                if (element instanceof PhysicsSurfaceUI p) {
                    p.updateCollisionObjects(getPhysicsObjects());
                    p.setVelocity(new Vector(platformXVelocity, 0)); //Set Platform Velocity
                    if (p.isCollidingTop(player, 20, 20)) isOnPlatform = true; //Set isOnPlatform
                    if (p.getPos().getAdded(p.getSize()).getX() < 0) queueRemoveElements(p); //Remove offscreen elements
                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }

        addBarriers();

        applyNaturalForces();

        applyControls(isOnPlatform);

        //Maximum Velocity
        double bound = 40;
        player.setVelocity(new Vector((Double) NumberUtil.getBoundValue(player.getVelocity().getX(), -bound, bound), player.getVelocity().getY()));
        super.tick();
    }

    @Override
    public void onKeyPress(int key, int scancode, int action, int mods) {
        super.onKeyPress(key, scancode, action, mods);
        if ((gameOver && key == Key.KEY_ENTER.getId()) || (key == Key.KEY_R.getId() && action == Key.ACTION_PRESS))
            retryButton.getAction().run();
    }

    @Override
    public void onMouseClick(int button, int action, int mods, Vector mousePos) {
        if (!gameOver) {
            if (action == Mouse.ACTION_CLICK && !player.isMouseOver()) placingBlock = true;
            if (action == Mouse.ACTION_RELEASE) placingBlock = false;

            if (button == Mouse.BUTTON_LEFT.getId() && !player.isMouseOver() && action == Mouse.ACTION_RELEASE) {
                try {
                    PhysicsSurfaceUI surface;
                    queueAddElements(surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(300, 50), ColorE.WHITE, friction * 1.1, friction));
                    surface.setCenter(mousePos);
                    forcePlayerLastInList();
                } catch (ConcurrentModificationException ignored) {
                }
            }
        }
        super.onMouseClick(button, action, mods, mousePos);
    }

    private void applyNaturalForces() {
        //Add Gravity Force
        double g = GravityUtil.g * 4;
        player.setNetForce(player.getNetForce().getAdded(GravityUtil.calculateSurfaceGravity(g, player)));
    }

    private void applyControls(boolean isOnPlatform) {
        //Jump
        if (isOnPlatform) {
            if (Key.KEY_SPACE.isKeyDown() || Key.KEY_UP.isKeyDown() || Key.KEY_W.isKeyDown()) {
                if (player.getVelocity().getY() == 0) {
                    player.setNetForce(player.getNetForce().getAdded(new Vector(0, -10000)));
                }
            }
        }

        //Move
        double force = isOnPlatform ? 600 : (double)600 /5;
        if (Key.KEY_RIGHT.isKeyDown() || Key.KEY_D.isKeyDown()) {
            player.setNetForce(player.getNetForce().getAdded(force, 0));
        } else if (Key.KEY_LEFT.isKeyDown() || Key.KEY_A.isKeyDown()) {
            player.setNetForce(player.getNetForce().getAdded(-force, 0));
        }
    }

    private void addBarriers() {
        Random random = new Random();
        if (watch.hasTimePassedS(2/speed)) {
            for (int i = 0; i < random.nextInt(0, 3); i++) {
                PhysicsSurfaceUI surface;
                queueAddElements(surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(25, 100), ColorE.WHITE, friction, friction));
                surface.setPos(new Vector(getWindow().getScaledSize().getX(), random.nextInt(0, (int) getWindow().getScaledSize().getY())));
                forcePlayerLastInList();
            }
            watch.restart();
            score++;
            speed += .04;
        }
    }

    private void forcePlayerLastInList() {
        queueRemoveElements(player);
        queueAddElements(player);
    }

    public ArrayList<PhysicsObjectUI> getPhysicsObjects() {
        ArrayList<PhysicsObjectUI> phy = new ArrayList<>();
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI physicsObjectUI && !(element instanceof PhysicsSurfaceUI)) phy.add(physicsObjectUI);
        }
        return phy;
    }

}
