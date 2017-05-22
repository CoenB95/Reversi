package com.cbapps.reversi;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.Serializable;

/**
 * Created by Gijs on 22-5-2017.
 */

public class PlayerInfo implements Serializable {
    private String name;
    private String colorName;

    public PlayerInfo(String name, String color) {
        this.name = name;
        this.colorName = color;
    }

    public String getName() {
        return name;
    }

    public Paint getColor() {
        return Color.web(colorName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof PlayerInfo)) return false;
		return this.getName().equals(((PlayerInfo) obj).getName());
	}
}
