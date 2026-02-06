package com.ejo.placerace;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.placerace.scenes.TitleScene;

public class App {

    public static Window WINDOW = new Window(
            "PlaceRace",
            new Vector(100,100),
            new Vector(1000,600),
            new TitleScene(),
            true,4,60,60
    );

    public static void main(String[] args)  {
        WINDOW.run();
        WINDOW.close();
    }
}
