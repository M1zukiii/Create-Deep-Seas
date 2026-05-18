package com.maxenonyme.createsubmarine.submarine.math;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
public class OrientedBoundingBox3d {
    private final Vector3d position = new Vector3d();
    private final Vector3d dimensions = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private final LevelReusedVectors sink;
    public OrientedBoundingBox3d(LevelReusedVectors sink) {
        this.sink = sink;
    }
    public void set(Vector3dc position, Vector3dc dimensions, Quaterniondc orientation) {
        this.position.set(position);
        this.dimensions.set(dimensions);
        this.orientation.set(orientation);
    }
    public Quaterniond getOrientation() {
        return orientation;
    }
    public Vector3d getPosition() {
        return position;
    }
    public Vector3d getDimensions() {
        return dimensions;
    }
}
