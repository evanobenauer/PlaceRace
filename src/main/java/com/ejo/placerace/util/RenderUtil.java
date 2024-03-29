package com.ejo.placerace.util;

import com.ejo.glowlib.math.Angle;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.scene.elements.shape.CircleUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;

import java.awt.*;

public class RenderUtil {


    public static void drawCoolDownWheel(double baseCoolDown, double currentCoolDown, Vector mousePos) {
        int size = 10;
        double coolDown = currentCoolDown / baseCoolDown;
        ColorE color = new ColorE((int) (255 * coolDown), (int) (255 * (1 - coolDown)), 0, 255);
        Vector pos = mousePos.getAdded(new Vector(size * 1.25, (double) -size / 4));
        new CircleUI(pos, ColorE.BLACK, (double) size + (double) size / 4, CircleUI.Type.MEDIUM).draw();
        new CircleUI(pos, color, size, new Angle(360 * (1 - coolDown), true), CircleUI.Type.MEDIUM).draw();
        new CircleUI(pos, ColorE.BLACK, (double) size - (double) size / 2, CircleUI.Type.MEDIUM).draw();
    }

    public static void drawBlockPlaceOutline(double platformSize, Vector mousePos) {
        RectangleUI rect = new RectangleUI(Vector.NULL,new Vector(platformSize,25),true,2,ColorE.BLUE);
        rect.setCenter(mousePos);
        rect.draw();
    }

    public static void drawGameOverMenu(double score, Vector sceneSize) {
        new GradientRectangleUI(Vector.NULL, sceneSize, new ColorE(255, 0, 0, 100), new ColorE(179, 0, 0, 100), GradientRectangleUI.Type.VERTICAL).draw();
        QuickDraw.drawTextCentered("Game Over", new Font("Arial Black", Font.PLAIN, 50), Vector.NULL, sceneSize, ColorE.RED);
        QuickDraw.drawTextCentered("Score: " + (int) score, Fonts.getDefaultFont(25), new Vector(0, 50), sceneSize, ColorE.RED);
    }

}
