package boids;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;

public class Obstacle {
    private Shape3D shape3D;
    private Vector3D location;
    private double x;
    private double y;
    private double z;

    public Obstacle(Shape3D shape3D, Color color, double x, double y, double z) {
        this.shape3D = shape3D;
        shape3D.setMaterial(new PhongMaterial(color));
        this.x = x;
        this.y = y;
        this.z = z;
        location = new Vector3D(x, y, z);
        shape3D.setTranslateX(x);
        shape3D.setTranslateY(y);
        shape3D.setTranslateZ(z);
    }

    public Shape3D getShape3D() {
        return shape3D;
    }

    public Vector3D getLocation() {
        return location;
    }

    public void setLocation(Vector3D location) {
        this.location = location;
    }

    public void setShape3D(Shape3D shape3D) {
        this.shape3D = shape3D;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
