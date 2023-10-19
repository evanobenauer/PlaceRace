package com.ejo.placerace.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;

public class TitleScene extends Scene {

    private final TextUI title = new TextUI("Place Race",new Font("Arial Black",Font.BOLD,80),Vector.NULL,ColorE.WHITE);

    private final ButtonUI buttonEnter = new ButtonUI("Start!",Vector.NULL,new Vector(200,40), new ColorE(0,150,255,200), ButtonUI.MouseButton.LEFT,() -> {
        getWindow().setScene(new GameScene());
    });


    public TitleScene() {
        super("Title");
        addElements(buttonEnter,title);
    }

    @Override
    public void draw() {
        drawBackground();

        //Draw Enter Button
        buttonEnter.setPos(getSize().getMultiplied(.5).getAdded(-buttonEnter.getSize().getX()/2,70));

        super.draw();
    }

    private double step = 0;
    @Override
    public void tick() {
        super.tick();
        double yOffset = 0;//-40;
        //Set Floating Title
        title.setPos(getSize().getMultiplied(.5d).getAdded(title.getSize().getMultiplied(-.5)).getAdded(0,yOffset));
        title.setPos(title.getPos().getAdded(new Vector(0,Math.sin(step) * 8)));
        step += 0.05;
        if (step >= Math.PI*2) step = 0;
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
}
