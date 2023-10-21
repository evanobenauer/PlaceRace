package com.ejo.placerace.scenes;

import com.ejo.glowlib.math.Angle;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowlib.util.NumberUtil;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.shape.CircleUI;
import com.ejo.glowui.scene.elements.shape.LineUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.util.*;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.placerace.elements.Player;
import com.ejo.placerace.elements.SemiCircleUI;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
import com.ejo.uiphysics.elements.PhysicsSurfaceUI;
import com.ejo.uiphysics.util.GravityUtil;
import com.ejo.uiphysics.util.VectorUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

public class GameScene extends Scene {

    private boolean gameOver = false;
    private boolean placingBlock = false;

    private final double friction = 1;
    double speed = 2;
    double speedIncrease = .12;
    private double score = 0;
    private double cooldown = 0;

    private final int platformSize = 300;//1000; //TODO: Make Easy, Normal, and Hard mode have different platform sizes. Easy 1000, Normal 300, Hard 100

    private final Player player = new Player(new RectangleUI(new Vector(60,100),new Vector(20,20), new ColorE(0,200,0)));
    private final PhysicsSurfaceUI basePlatform = new PhysicsSurfaceUI(new Vector(0,1000),new Vector(1000,1000),ColorE.WHITE, friction, friction);

    private final StopWatch watch = new StopWatch();

    private final ButtonUI retryButton = new ButtonUI("Try Again",Vector.NULL,new Vector(500,100),ColorE.RED, ButtonUI.MouseButton.LEFT,() -> {
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI p) queueRemoveElements(p);
        }
        gameOver = false;
        speed = 2;
        score = 0;
        player.setPos(new Vector(60,100));
        player.setVelocity(new Vector(100,0));
        basePlatform.setPos(new Vector(0,1000));

        queueAddElements(basePlatform);
        queueRemoveElements(player);
        queueAddElements(player);
    });

    public GameScene() {
        super("PlaceRace");
        addElements(retryButton);
        addElements(basePlatform); //Add Collision Objects
        addElements(player); // Add Physics Objects
        player.setDeltaT(.1f);
        player.setVelocity(new Vector(100,0));
        player.setDebugVectorCap(1000);
        player.setDebugVectorForceScale(.5);
        retryButton.setRendered(false);
    }


    @Override
    public void draw() {
        drawBackground();
        try {
            super.draw();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        QuickDraw.drawText("Score: " + (int)score, Fonts.getDefaultFont(30),new Vector(2,2),ColorE.WHITE);

        drawCooldownWheel(20);

        if (placingBlock) {
            RectangleUI rect = new RectangleUI(Vector.NULL,new Vector(platformSize,50),true,3,ColorE.BLUE);
            rect.setCenter(getWindow().getScaledMousePos());
            rect.draw();
        }

        drawGameOverMenu();

    }

    @Override
    public void tick() {
        if (!watch.isStarted()) watch.start();
        cooldown -= .05;
        cooldown = Math.max(0,cooldown);

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
                    if (p.getPos().getAdded(p.getSize()).getX() < 0) queueRemoveElements(p); //Remove off screen elements
                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }

        addBarriersProgressively();

        applyNaturalForces();

        applyControls(isOnPlatform);

        //Maximum Velocity
        double bound = 50;
        player.setVelocity(new Vector((Double) NumberUtil.getBoundValue(player.getVelocity().getX(), -bound + platformXVelocity, bound), player.getVelocity().getY()));
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
        if (!gameOver && cooldown == 0) {
            if (action == Mouse.ACTION_CLICK && !player.isMouseOver()) placingBlock = true;
            if (action == Mouse.ACTION_RELEASE) placingBlock = false;

            if (button == Mouse.BUTTON_LEFT.getId() && !player.isMouseOver() && action == Mouse.ACTION_RELEASE) {
                try {
                    PhysicsSurfaceUI surface;
                    queueAddElements(surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(platformSize, 50), ColorE.WHITE, friction * 1.1, friction));
                    surface.setCenter(mousePos);
                    surface.setDeltaT(.1f);
                    cooldown = 1;
                    forcePlayerLastElement();
                } catch (ConcurrentModificationException ignored) {
                }
            }
        }
        super.onMouseClick(button, action, mods, mousePos);
    }

    private void drawCooldownWheel(int size) {
        if (cooldown > 0) {
            ColorE color = new ColorE((int)(255 * cooldown),(int)(255 * (1-cooldown)),0,255);
            Vector pos = getWindow().getScaledMousePos().getAdded(new Vector(25, -5));
            new CircleUI(pos,ColorE.BLACK, (double) size + 5, CircleUI.Type.MEDIUM).draw();
            new SemiCircleUI(pos, color, size, new Angle(360 * (1 - cooldown), true), CircleUI.Type.MEDIUM).draw();
            new CircleUI(pos,ColorE.BLACK, (double) size - 10, CircleUI.Type.MEDIUM).draw();
        }
    }

    private void drawGameOverMenu() {
        if (gameOver) {
            placingBlock = false;
            drawGameOverBackground();
            QuickDraw.drawTextCentered("Game Over",new Font("Arial Black",Font.PLAIN,100),Vector.NULL,getWindow().getScaledSize(),ColorE.RED);
            QuickDraw.drawTextCentered("Score: " + (int)score,Fonts.getDefaultFont(50),new Vector(0,100),getWindow().getScaledSize(),ColorE.RED);
            retryButton.draw();
            retryButton.setRendered(true);
            retryButton.setPos(getWindow().getScaledSize().getMultiplied(.5).getAdded(retryButton.getSize().getMultiplied(-.5)));
            retryButton.setPos(retryButton.getPos().getAdded(new Vector(0,200)));
        } else {
            retryButton.setRendered(false);
        }
    }

    private void drawBackground() {
        Vector pos = Vector.NULL;
        Vector size = getSize();
        GL11.glBegin(GL11.GL_QUADS);

        GL11.glColor4f(0,0,0,0);
        GL11.glVertex2f((float) pos.getX(), (float) pos.getY());
        GL11.glVertex2f((float) pos.getX() + (float) size.getX(), (float) pos.getY());
        GL11.glColor4f(0f, .2f,.5f,1);
        GL11.glVertex2f((float) pos.getX() + (float) size.getX(), (float) pos.getY() + (float) size.getY());
        GL11.glVertex2f((float) pos.getX(), (float) pos.getY() + (float) size.getY());

        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void drawGameOverBackground() {
        Vector pos = Vector.NULL;
        Vector size = getSize();
        GL11.glBegin(GL11.GL_QUADS);

        GL11.glColor4f(.7f,0,0,100/255f);
        GL11.glVertex2f((float) pos.getX(), (float) pos.getY());
        GL11.glVertex2f((float) pos.getX() + (float) size.getX(), (float) pos.getY());
        GL11.glColor4f(1f, 0f,0f,100/255f);
        GL11.glVertex2f((float) pos.getX() + (float) size.getX(), (float) pos.getY() + (float) size.getY());
        GL11.glVertex2f((float) pos.getX(), (float) pos.getY() + (float) size.getY());

        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
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

    private void addBarriersProgressively() {
        Random random = new Random();
        if (watch.hasTimePassedS(2/speed + .5)) {
            for (int i = 0; i < random.nextInt(0, 3); i++) {
                PhysicsSurfaceUI surface;
                queueAddElements(surface = new PhysicsSurfaceUI(Vector.NULL, new Vector(25, random.nextInt(50,200)), ColorE.WHITE, friction, friction));
                surface.setPos(new Vector(getWindow().getScaledSize().getX(), random.nextInt(0, (int) getWindow().getScaledSize().getY())));
                forcePlayerLastElement();
            }
            watch.restart();
            score++;
            speed += speedIncrease;
        }
    }

    private void forcePlayerLastElement() {
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
