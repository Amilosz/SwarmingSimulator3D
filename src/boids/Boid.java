package boids;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.StrictMath.pow;

public class Boid {
    static double MAX_VELOCITY = 2;
    private final Node boidView;
    private final Node neighborhoodView;
    List<Boid> neighborhoodList = new ArrayList<>();

    Random random = new Random();

    Sphere boidSphere;
    Sphere neighborhoodSphere;

    private final Line velocityLine;
    private String name;
    private Vector3D velocity;
    private Vector3D location;
    private final int ENVIRONMENTS_DEPTH;
    private final int ENVIRONMENT_WIDTH;
    private final int ENVIRONMENTS_HEIGHT;
    private final double boidSize = 3.5;

    public Boid(double x, double y, double z, int ENVIRONMENT_WIDTH, int ENVIRONMENTS_HEIGHT, int ENVIRONMENTS_DEPTH) {
        boidSphere = new Sphere(20);
        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseColor(Color.RED);
        boidSphere.setMaterial(phongMaterial);
        boidView = boidSphere;

        neighborhoodSphere = new Sphere(0);
        PhongMaterial phongMaterial2 = new PhongMaterial();
        phongMaterial2.setDiffuseColor(Color.GRAY);
        neighborhoodSphere.setMaterial(phongMaterial2);
        neighborhoodSphere.setOpacity(0.1);
        neighborhoodView = neighborhoodSphere;


        location = new Vector3D(x, y, z);
        velocity = new Vector3D(1, 1, 1);

        this.ENVIRONMENT_WIDTH = ENVIRONMENT_WIDTH;
        this.ENVIRONMENTS_HEIGHT = ENVIRONMENTS_HEIGHT;
        this.ENVIRONMENTS_DEPTH = ENVIRONMENTS_DEPTH;
        velocityLine = new Line();
        velocityLine.startXProperty().bind(boidSphere.layoutXProperty().add(boidSphere.translateXProperty()));
        velocityLine.startYProperty().bind(boidSphere.layoutYProperty().add(boidSphere.translateYProperty()));

    }

    public Vector3D getVelocity() {
        return velocity;
    }

    @Override
    public String toString() {
        String x = String.format("%.2f", getBoidView().getTranslateX());
        String y = String.format("%.2f", getBoidView().getTranslateY());

        return getName() + "  X:" + x + "  Y: " + y;
    }

    public void update(boolean showNeighborhood, boolean showVectors, double alignmentWeight, double separationWeight, double cohesionWeight, double distance, double maxVelocity, NeighborhoodType neighborhoodType) {
        MAX_VELOCITY = maxVelocity;

        if (!neighborhoodList.isEmpty()) {
            Vector3D cohesionForce = getCohesionControl(cohesionWeight);
            velocity = velocity.plus(cohesionForce);

            Vector3D separationForce = getSeparationControl(separationWeight);
            velocity = velocity.plus(separationForce);

            Vector3D alignmentForce = getAlignmentControl(alignmentWeight);
            velocity = velocity.plus(alignmentForce);

            Vector3D obstacleAvoidanceForce = getObstacleAvoidanceControl(distance);
            velocity = velocity.plus(obstacleAvoidanceForce);
        }

        //wander();
        //if (random.nextDouble() > 0.60d)
        //    velocity = velocity.plus(new Vector3D(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalizeTo(0.5));

        bounceOffTheWallIfCollidingWith();

        //applyMove();
        velocity = velocity.normalizeTo(MAX_VELOCITY);
        location = location.plus(velocity);


        //setEndOfVectorLine(maxVelocity);

        render(showNeighborhood, showVectors, distance);
    }

    private Vector3D getObstacleAvoidanceControl(double inDistance) {
        ObstacleDetection result = checkIsColliding(inDistance);
        Vector3D avoidanceVector = null;
        switch (result) {
            case LEFT:
                avoidanceVector = rotateLeft();
                break;
            case RIGHT:
                avoidanceVector = rotateRight();
                break;
            case NONE:
                avoidanceVector = new Vector3D(location.getX(), location.getY(), location.getZ());
                break;
        }

        return avoidanceVector;
    }

    private ObstacleDetection checkIsColliding(double inDistance) {
        return ObstacleDetection.RIGHT;
    }

    private void setEndOfVectorLine(double maxVelocity) {
        Vector3D future_location = location.plus(velocity.normalizeTo(maxVelocity + 30));
        velocityLine.setEndX(future_location.getX());
        velocityLine.setEndY(future_location.getY());
    }

    private Vector3D getAlignmentControl(double alignmentWeight) {
        Vector3D neighbourhoodVelocity = getNeighbourhoodAverageVelocity();
        return neighbourhoodVelocity.normalizeTo(alignmentWeight);
    }

    private Vector3D getCohesionControl(double forceWeight) {
        Vector3D neighbourhoodAverageLocation = getNeighbourhoodAverageLocation();
        Vector3D cohesionControl = neighbourhoodAverageLocation.minus(location);
        return cohesionControl.normalizeTo(forceWeight);
    }

    private Vector3D getSeparationControl(double forceWeight) {
        Vector3D thisBoidLocation = location;
        Vector3D control = new Vector3D(0d, 0d, 0d);
        for (Boid neighbor : neighborhoodList) {
            control = control.plus(thisBoidLocation.minus(neighbor.location));
        }
        return control.normalizeTo(forceWeight);
    }

    public Vector3D getNeighbourhoodAverageLocation() {
        double centerX = 0d;
        double centerY = 0d;
        double centerZ = 0d;
        for (Boid neighbor : neighborhoodList) {
            centerX += neighbor.getLocationVector().getX();
            centerY += neighbor.getLocationVector().getY();
            centerZ += neighbor.getLocationVector().getZ();
        }
        return new Vector3D(centerX / neighborhoodList.size(), centerY / neighborhoodList.size(), centerZ / neighborhoodList.size());
    }

    public Vector3D getNeighbourhoodAverageVelocity() {
        double centerX = 0d;
        double centerY = 0d;
        double centerZ = 0d;
        for (Boid neighbor : neighborhoodList) {
            centerX += neighbor.getVelocity().getX();
            centerY += neighbor.getVelocity().getY();
            centerZ += neighbor.getVelocity().getZ();
        }
        return new Vector3D(centerX / neighborhoodList.size(), centerY / neighborhoodList.size(), centerZ / neighborhoodList.size());
    }

    private void render(boolean showNeighborhood, boolean showVectors, double distance) {
        boidView.setTranslateY(location.getY());
        boidView.setTranslateX(location.getX());
        boidView.setTranslateZ(location.getZ());

        neighborhoodView.setTranslateX(location.getX());
        neighborhoodView.setTranslateY(location.getY());
        neighborhoodView.setTranslateZ(location.getZ());
        neighborhoodSphere.setRadius(distance);


        // double r = Math.toDegrees(Math.atan(velocity.getY() / velocity.getX()));
        // r = velocity.getX() < 0.0d ? r - 180d : r;
        // this.boidView.setRotate(r);

        if (!showNeighborhood) {
            neighborhoodView.setOpacity(0);
        } else {
            neighborhoodView.setOpacity(0.7);
        }
        if (!showVectors) {
            velocityLine.setOpacity(0);
        } else {
            velocityLine.setOpacity(1);
        }
    }

    public double getDistanceFrom(Boid boid) {
        double differenceOfX = this.getBoidView().getTranslateX() - boid.getBoidView().getTranslateX();
        double differenceOfY = this.getBoidView().getTranslateY() - boid.getBoidView().getTranslateY();
        double differenceOfZ = this.getBoidView().getTranslateZ() - boid.getBoidView().getTranslateZ();

        double result_c = Math.sqrt(pow(differenceOfX, 2) + pow(differenceOfY, 2) + pow(differenceOfZ, 2));
        return result_c;
    }

    private void bounceOffTheWallIfCollidingWith() {
        if (location.getX() < 0)
            velocity.setX(1);

        if (location.getX() > ENVIRONMENT_WIDTH)
            velocity.setX(-1);

        if (location.getY() < 0)
            velocity.setY(1);

        if (location.getY() > ENVIRONMENTS_HEIGHT)
            velocity.setY(-1);

        if (location.getZ() < 0)
            velocity.setZ(1);

        if (location.getZ() > ENVIRONMENTS_HEIGHT)
            velocity.setZ(-1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector3D getLocationVector() {
        return location;
    }

    public void setVector(Vector3D vector) {
        this.location = vector;
    }

    public Node getNeighborhoodView() {
        return neighborhoodView;
    }

    public Node getBoidView() {
        return boidView;
    }

    public double getRotate() {
        return boidView.getRotate();
    }

    public Node getVelocityLine() {
        return velocityLine;
    }

    public Vector3D rotateRight() {
        boidView.setRotate(boidView.getRotate() + 5);
        return ((new Vector3D(Math.cos(Math.toRadians(getRotate())), Math.cos(Math.toRadians(getRotate())), Math.sin((Math.toRadians(getRotate()))))));
    }

    public Vector3D rotateLeft() {
        boidView.setRotate(boidView.getRotate() - 5);
        return ((new Vector3D(Math.cos(Math.toRadians(getRotate())), Math.cos(Math.toRadians(getRotate())), Math.sin((Math.toRadians(getRotate()))))));
    }

    public void clearNeighborhoodList() {
        neighborhoodList.clear();
    }

    public void addNeighbor(Boid neighbor) {
        neighborhoodList.add(neighbor);
    }

    public void showNeighbors() {
        if (!neighborhoodList.isEmpty()) {
            System.out.println("=============Neighborhood of " + this.getName() + "=============");
            System.out.println("=============Neighborhood of " + this.getName() + "=============");
        }
        for (Boid neighbors : neighborhoodList) {
            System.out.println(neighbors.toString());
        }
    }


}
