package com.ejo.placerace.elements;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.uiphysics.elements.PhysicsObjectUI;

public class Player extends PhysicsObjectUI {

    public Player(RectangleUI rect) {
        super(rect, 10, Vector.NULL, Vector.NULL);
    }

    @Override
    public RectangleUI getShape() {
        return (RectangleUI) super.getShape();
    }
}
