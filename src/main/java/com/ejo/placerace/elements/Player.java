package com.ejo.placerace.elements;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.util.NumberUtil;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.util.Key;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
import com.ejo.uiphysics.elements.PhysicsSurfaceUI;
import com.ejo.uiphysics.util.GravityUtil;

//TODO: If I am allowed to use GlowUI, just put the entiretiy of UIPhysics in here and pretend you did that

public class Player extends PhysicsObjectUI {

    private boolean onGround;

    public Player(RectangleUI rect) {
        super(rect, 5, Vector.NULL, Vector.NULL);
        this.onGround = false;
    }

    public void update(PhysicsSurfaceUI[] surfaces) {
        //Update OnGround
        setOnGround(false);
        double platformXVelocity = 0;
        for (PhysicsSurfaceUI surface : surfaces) {
            if (surface.isColliding(this, PhysicsSurfaceUI.CollisionType.TOP)) setOnGround(true);
            platformXVelocity = surface.getVelocity().getX();
        }

        //Apply Gravity
        addForce(GravityUtil.calculateSurfaceGravity(GravityUtil.g * 2.75,this));

        //Apply Controls
        boolean isJumpKeyDown = Key.KEY_SPACE.isKeyDown() || Key.KEY_UP.isKeyDown() || Key.KEY_W.isKeyDown();
        boolean isRightKeyDown = Key.KEY_RIGHT.isKeyDown() || Key.KEY_D.isKeyDown();
        boolean isLeftKeyDown = Key.KEY_LEFT.isKeyDown() || Key.KEY_A.isKeyDown();
        control(isJumpKeyDown,isRightKeyDown,isLeftKeyDown);

        //Set Maximum X Velocity
        double bound = 25;
        setVelocity(new Vector((Double) NumberUtil.getBoundValue(getVelocity().getX(), -bound + platformXVelocity, bound), getVelocity().getY()));
    }

    private void control(boolean isJumpKeyDown, boolean isRightKeyDown, boolean isLeftKeyDown) {
        //Jump
        if (isOnGround() && getVelocity().getY() == 0 && isJumpKeyDown) jump();

        //Move
        double groundForce = 300;
        double airForce = groundForce/5;
        double force = isOnGround() ? groundForce : airForce;
        if (isRightKeyDown) addForce(new Vector(force,0));
        if (isLeftKeyDown) addForce(new Vector(-force,0));
    }

    private void jump() {
        setNetForce(getNetForce().getAdded(new Vector(0, -3600)));
    }


    public void reset() {
        setDeltaT(.1f);
        setPos(new Vector(60,100));
        setVelocity(new Vector(100,0));

        setTickNetReset(true);

        setDebugVectorCap(1000);
        setDebugVectorForceScale(.5);
    }

    @Override
    public RectangleUI getShape() {
        return (RectangleUI) super.getShape();
    }

    private void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return onGround;
    }



}
