package com.cbapps.reversi.client;

import javafx.animation.RotateTransition;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
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

	private Circle stone;
	private Paint playerColor = Color.BLACK;

	public CellPane() {
		setPrefSize(200, 200);

		stone = new Circle(10, 10, 10);
		stone.centerXProperty().bind(stone.radiusProperty().add(10));
		stone.centerYProperty().bind(stone.radiusProperty().add(10));
		stone.setFill(playerColor);
		stone.radiusProperty().bind(Bindings.min(widthProperty(), heightProperty()).divide(2).subtract(10));

		setOnMouseClicked(event -> changePossession(Color.hsb(Math.random() * 360, 1.0, 1.0)));
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
		transition.playFromStart();
	}
}
