package boids;

public class Vector3D {
    private double x;
    private double y;
    private double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D plus(Vector3D arg) {
        return new Vector3D(x + arg.getX(), y + arg.getY(), z + arg.getZ());
    }

    public Vector3D minus(Vector3D arg) {
        return new Vector3D(x - arg.getX(), y - arg.getY(), z - arg.getZ());
    }


    public Vector3D normalizeTo(double to) {
        double magnitude = getMagnitude();
        if (0d == magnitude) {
            return new Vector3D(0d, 0d, 0d);
        }
        double xi = x / magnitude * to;
        double yi = y / magnitude * to;
        double zi = z / magnitude * to;
        Vector3D n = new Vector3D(xi, yi, zi);
        return n;
    }

    public double getMagnitude() {
        double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        return magnitude;
    }

    public double setX(double x) {
        return this.x = x;
    }

    public double getX() {
        return x;
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
