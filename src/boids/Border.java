package boids;

import javafx.scene.Node;
import javafx.scene.shape.Box;

public class Border {
    private final Node view;
    private String name;

    public Border(int WIDTH, int HEIGHT) {
        this.view = new Box(WIDTH, HEIGHT, 3);
    }

    public Node getView() {
        return view;
    }
}
