package boids;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static boids.Boid.MAX_VELOCITY;

public class Predator {
    private Vector3D location;
    private Vector3D velocity;
    private Random random = new Random();
    private Sphere predatorSphere;

    private int ENVIRONMENTS_HEIGHT;
    private int ENVIRONMENTS_DEPTH;
    private int ENVIRONMENT_WIDTH;

    public Predator(int ENVIRONMENT_WIDTH, int ENVIRONMENTS_HEIGHT, int ENVIRONMENTS_DEPTH) {
        this.ENVIRONMENT_WIDTH = ENVIRONMENT_WIDTH;
        this.ENVIRONMENTS_HEIGHT = ENVIRONMENTS_HEIGHT;
        this.ENVIRONMENTS_DEPTH = ENVIRONMENTS_DEPTH;

        PhongMaterial phongMaterial = new PhongMaterial();
        phongMaterial.setDiffuseColor(Color.BLACK);

        predatorSphere = new Sphere(60);
        predatorSphere.setMaterial(phongMaterial);

        int x = random.nextInt(ENVIRONMENT_WIDTH);
        int y = random.nextInt(ENVIRONMENTS_HEIGHT);
        int z = random.nextInt(ENVIRONMENTS_DEPTH);

        location = new Vector3D(x, y, z);
        velocity = new Vector3D(random.nextInt(5), random.nextInt(5), random.nextInt(5));

    }

    public void update(List<Boid> boidsList) {
        Boid nearestBoid = null;
        TreeMap<Double, Boid> boidsWithDistance = new TreeMap<>();
        for (int i = 0; i < boidsList.size(); i++) {
            for (int j = 0; j < boidsList.size(); j++) {
                if (i != j) {
                    double distanceFrom = boidsList.get(i).getDistanceFrom(boidsList.get(j));
                    boidsWithDistance.put(distanceFrom, boidsList.get(i));
                }
            }
            nearestBoid = boidsWithDistance.get(boidsWithDistance.firstKey());
        }
        velocity = velocity.plus((nearestBoid.getLocation().minus(location)));

        velocity = velocity.plus(new Vector3D(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalizeTo(0.5));
        bounceOffTheWallIfCollidingWith();

        velocity = velocity.normalizeTo(MAX_VELOCITY);
        location = location.plus(velocity);

        predatorSphere.setTranslateY(location.getY());
        predatorSphere.setTranslateX(location.getX());
        predatorSphere.setTranslateZ(location.getZ());
    }

    private void bounceOffTheWallIfCollidingWith() {
        if (location.getX() < 0)
            velocity.setX(3);

        if (location.getX() > ENVIRONMENT_WIDTH)
            velocity.setX(-3);

        if (location.getY() < 0)
            velocity.setY(3);

        if (location.getY() > ENVIRONMENTS_HEIGHT)
            velocity.setY(-3);

        if (location.getZ() < 0)
            velocity.setZ(3);

        if (location.getZ() > ENVIRONMENTS_HEIGHT)
            velocity.setZ(-3);
    }

    public Vector3D getLocation() {
        return location;
    }

    public void setLocation(Vector3D location) {
        this.location = location;
    }

    public Vector3D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3D velocity) {
        this.velocity = velocity;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public Sphere getPredatorSphere() {
        return predatorSphere;
    }

    public void setPredatorSphere(Sphere predatorSphere) {
        this.predatorSphere = predatorSphere;
    }

    public int getENVIRONMENTS_HEIGHT() {
        return ENVIRONMENTS_HEIGHT;
    }

    public void setENVIRONMENTS_HEIGHT(int ENVIRONMENTS_HEIGHT) {
        this.ENVIRONMENTS_HEIGHT = ENVIRONMENTS_HEIGHT;
    }

    public int getENVIRONMENTS_DEPTH() {
        return ENVIRONMENTS_DEPTH;
    }

    public void setENVIRONMENTS_DEPTH(int ENVIRONMENTS_DEPTH) {
        this.ENVIRONMENTS_DEPTH = ENVIRONMENTS_DEPTH;
    }

    public int getENVIRONMENT_WIDTH() {
        return ENVIRONMENT_WIDTH;
    }

    public void setENVIRONMENT_WIDTH(int ENVIRONMENT_WIDTH) {
        this.ENVIRONMENT_WIDTH = ENVIRONMENT_WIDTH;
    }
}
