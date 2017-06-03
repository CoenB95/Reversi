package com.cbapps.reversi.client;

import javafx.animation.RotateTransition;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * @author Coen Boelhouwers
 */
public class CellPane extends Pane {

	private static int flipDelay;

	private Circle stone;
	private Paint playerColor = Color.TRANSPARENT;
	private int col;
	private int row;

	public CellPane(int row, int col) {
		this.col = col;
		this.row = row;
		setPrefSize(200, 200);

		stone = new Circle(2, 2, 10);
		stone.centerXProperty().bind(stone.radiusProperty().add(2));
		stone.centerYProperty().bind(stone.radiusProperty().add(2));
		stone.setFill(playerColor);
		stone.radiusProperty().bind(Bindings.min(widthProperty(), heightProperty()).divide(2).subtract(2));
		setBorderColor(Color.DARKGREEN);
		getChildren().addAll(stone);

	}

	public void changePossession(Paint color) {
		Paint oldColor = playerColor;
		playerColor = color;
		stone.setRotate(0);
		RotateTransition transition = new RotateTransition(Duration.millis(400), stone);
		transition.setByAngle(180);
		transition.setAxis(Rotate.X_AXIS);
		stone.fillProperty().bind(Bindings.when(stone.rotateProperty().greaterThan(90))
				.then(playerColor).otherwise(oldColor));
		transition.setDelay(Duration.millis(flipDelay));
		flipDelay += 200;
		transition.playFromStart();
		transition.setOnFinished(event -> flipDelay -= 200);
	}

	public void setBorderColor(Color color) {
		setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, null, null)));
	}

	public int getColumn() {
		return col;
	}

	public int getRow() {
		return row;
	}
}
