package com.ejo.placerace.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.GradientRectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class TitleScene extends Scene {

    private final Container<Mode> modeContainer = new Container<>(Mode.MEDIUM);

    private final TextUI title = new TextUI("Place Race",new Font("Arial Black",Font.BOLD,80),Vector.NULL,ColorE.WHITE);

    private final ButtonUI buttonEnter = new ButtonUI("Start!",Vector.NULL,new Vector(200,40), new ColorE(0,150,255,200), ButtonUI.MouseButton.LEFT,() -> {
        getWindow().setScene(new GameScene(modeContainer.get()));
    });

    private final ModeCycleUI<Mode> modeCycleMode = new ModeCycleUI<>("Mode: ", Vector.NULL,new Vector(200,25),ColorE.BLUE, modeContainer,Mode.EASY,Mode.MEDIUM,Mode.HARD);


    public TitleScene() {
        super("Title");
        addElements(buttonEnter, modeCycleMode,title);
    }

    @Override
    public void draw() {
        new GradientRectangleUI(Vector.NULL,getSize(),new ColorE(0,50, 128),ColorE.BLACK.alpha(0), GradientRectangleUI.Type.VERTICAL).draw();

        //Draw Enter Button
        buttonEnter.setPos(getSize().getMultiplied(.5).getAdded(-buttonEnter.getSize().getX()/2,70));

        //Draw Mode Cycle
        modeCycleMode.setPos(getSize().getMultiplied(.5).getAdded(-modeCycleMode.getSize().getX()/2,140));

        super.draw();
    }

    private double step = 0;
    @Override
    public void tick() {
        super.tick();
        double yOffset = 0;
        //Set Floating Title
        title.setPos(getSize().getMultiplied(.5d).getAdded(title.getSize().getMultiplied(-.5)).getAdded(0,yOffset));
        title.setPos(title.getPos().getAdded(new Vector(0,Math.sin(step) * 8)));
        step += 0.05;
        if (step >= Math.PI*2) step = 0;
    }

    public enum Mode {
        EASY,
        MEDIUM,
        HARD
    }
}
