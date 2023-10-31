package com.ejo.placerace.elements;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.uiphysics.elements.PhysicsObjectUI;

public class Player extends PhysicsObjectUI {

    private boolean onGround;

    public Player(RectangleUI rect) {
        super(rect, 10, Vector.NULL, Vector.NULL);
        this.onGround = false;
    }

    @Override
    public RectangleUI getShape() {
        return (RectangleUI) super.getShape();
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return onGround;
    }



}
