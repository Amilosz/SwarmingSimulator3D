package boids;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.pow;

public class Boid {
    static double MAX_VELOCITY = 15;
    static double MIN_VELOCITY = 5;
    private final Node boidView;
    private final Node neighborhoodView;
    private final Line velocityLine;
    private final int ENVIRONMENTS_DEPTH;
    private final int ENVIRONMENT_WIDTH;
    private final int ENVIRONMENTS_HEIGHT;
    boolean isAlive = true;
    List<Boid> neighborhoodList = new ArrayList<>();
    Random random = new Random();
    Sphere boidSphere;
    Sphere neighborhoodSphere;
    private String name;
    private Vector3D velocity;
    private Vector3D location;

    public Boid(double x, double y, double z, int ENVIRONMENT_WIDTH, int ENVIRONMENTS_HEIGHT, int ENVIRONMENTS_DEPTH) {
        boidSphere = new Sphere(20);
        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseColor(Color.RED);
        boidSphere.setMaterial(phongMaterial);
        boidView = boidSphere;

        neighborhoodSphere = new Sphere(0);
        PhongMaterial phongMaterial2 = new PhongMaterial();
        phongMaterial2.setDiffuseColor(Color.GRAY.brighter());
        neighborhoodSphere.setMaterial(phongMaterial2);
        neighborhoodSphere.setOpacity(0.1);
        neighborhoodView = neighborhoodSphere;


        location = new Vector3D(x, y, z);
        velocity = new Vector3D(random.nextInt(5), random.nextInt(5), random.nextInt(5));

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
        String x = String.format("%.0f", getBoidView().getTranslateX());
        String y = String.format("%.0f", getBoidView().getTranslateY());

        return getName() + "  X:" + x + "  Y: " + y;
    }

    public void update(boolean showNeighborhood, boolean showVectors, double alignmentWeight, double separationWeight, double cohesionWeight, double distance, double maxVelocity, double minVelocity, Leader leader, Predator predator, List<Obstacle> obstacleList) {
        MAX_VELOCITY = maxVelocity;
        MIN_VELOCITY = minVelocity;

        for (Obstacle obstacle : obstacleList){
            if ( getDistanceFrom(obstacle) < 100)
                velocity = velocity.plus(location.minus(obstacle.getLocation()));
        }

        if (predator !=null && getDistanceFrom(predator) < 400)
        {
            Vector3D avoidPredatorForce = getAvoidPredatorForce(predator);
            velocity = velocity.plus(avoidPredatorForce.normalizeTo(maxVelocity*2));
        }
        if (leader != null) {
            if (getDistanceFrom(leader) > 30) {
                Vector3D followLeaderForce = getFollowLeaderForce(leader);
                velocity = velocity.plus(followLeaderForce).normalizeTo(maxVelocity);
            }
        } else {
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
        }


        //wander();
        if (random.nextDouble() > 0.60d)
            velocity = velocity.plus(new Vector3D(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalizeTo(0.5));

        bounceOffTheWallIfCollidingWith();

        //applyMove();
        if (velocity.getMagnitude() > MAX_VELOCITY)
            velocity = velocity.normalizeTo(MAX_VELOCITY);
        if (velocity.getMagnitude() < MIN_VELOCITY)
            velocity = velocity.normalizeTo(MIN_VELOCITY);

        location = location.plus(velocity);


        //setEndOfVectorLine(maxVelocity);

        render(showNeighborhood, showVectors, distance);
    }

    private Vector3D getAvoidPredatorForce(Predator predator) {
        Vector3D avoidPredatorForce = location.minus(predator.getLocation());
        avoidPredatorForce.normalizeTo(1);
        return avoidPredatorForce;
    }

    private Vector3D getFollowLeaderForce(Leader leader) {
        Vector3D followLeaderForce = leader.getLocation().minus(this.location);
        followLeaderForce.normalizeTo(1);
        return followLeaderForce;
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


    private Vector3D getAlignmentControl(double alignmentWeight) {
        Vector3D neighbourhoodVelocity = getNeighbourhoodAverageVelocity();
        return neighbourhoodVelocity.normalizeTo(alignmentWeight);
    }

    private Vector3D getCohesionControl(double forceWeight) {
        Vector3D neighbourhoodAverageLocation = getNeighbourhoodAverageLocation();
        Vector3D cohesionControl = neighbourhoodAverageLocation.minus(this.location);
        return cohesionControl.normalizeTo(forceWeight);
    }

    private Vector3D getSeparationControl(double forceWeight) {
        Vector3D thisBoidLocation = location;
        Vector3D separationControl = new Vector3D(0d, 0d, 0d);
        for (Boid neighbor : neighborhoodList) {
            separationControl = separationControl.plus(thisBoidLocation.minus(neighbor.location));
        }
        return separationControl.normalizeTo(forceWeight);
    }


    public Vector3D getNeighbourhoodAverageLocation() {
        double centerX = 0d;
        double centerY = 0d;
        double centerZ = 0d;
        for (Boid neighbor : neighborhoodList) {
            centerX += neighbor.getLocation().getX();
            centerY += neighbor.getLocation().getY();
            centerZ += neighbor.getLocation().getZ();
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

        double result_c = Math.sqrt(abs(pow(differenceOfX, 2) + pow(differenceOfY, 2) + pow(differenceOfZ, 2)));
        return result_c;
    }

    // TODO; generify boid, leader, obstacle and predator
    public double getDistanceFrom(Leader leader) {
        double differenceOfX = this.getBoidView().getTranslateX() - leader.getLeaderSphere().getTranslateX();
        double differenceOfY = this.getBoidView().getTranslateY() - leader.getLeaderSphere().getTranslateY();
        double differenceOfZ = this.getBoidView().getTranslateZ() - leader.getLeaderSphere().getTranslateZ();

        double result_c = Math.sqrt(abs(pow(differenceOfX, 2) + pow(differenceOfY, 2) + pow(differenceOfZ, 2)));
        return result_c;
    }

    public double getDistanceFrom(Predator predator) {
        double differenceOfX = this.getBoidView().getTranslateX() - predator.getPredatorSphere().getTranslateX();
        double differenceOfY = this.getBoidView().getTranslateY() - predator.getPredatorSphere().getTranslateY();
        double differenceOfZ = this.getBoidView().getTranslateZ() - predator.getPredatorSphere().getTranslateZ();

        double result_c = Math.sqrt(abs(pow(differenceOfX, 2) + pow(differenceOfY, 2) + pow(differenceOfZ, 2)));
        return result_c;
    }

    public double getDistanceFrom(Obstacle obstacle) {
        double differenceOfX = this.getBoidView().getTranslateX() - obstacle.getShape3D().getTranslateX();
        double differenceOfY = this.getBoidView().getTranslateY() - obstacle.getShape3D().getTranslateY();
        double differenceOfZ = this.getBoidView().getTranslateZ() - obstacle.getShape3D().getTranslateZ();

        double result_c = Math.sqrt(abs(pow(differenceOfX, 2) + pow(differenceOfY, 2) + pow(differenceOfZ, 2)));
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

    public Vector3D getLocation() {
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
