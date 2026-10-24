package com.trifectumsoftware.mekapda.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ArmViewPose {

    public static final Vector3fc SHOULDER = new Vector3f(0.18F, -0.20F, 0.13F);
    public static final float MAX_REACH = 0.995F;
    public static final Vector3fc RIGHT_POLE = new Vector3f(0.6F, -1, 0.2F);
    public static final Vector3fc LEFT_POLE = new Vector3f(-0.6F, -1, 0.2F);

    private static final Vector3f DIR = new Vector3f();
    private static final Vector3f PERP = new Vector3f();
    private static final Vector3f FOREARM = new Vector3f();
    private static final Vector3f UPPER = new Vector3f();
    private static final Vector3f OUT = new Vector3f();
    private static final Vector3f HINGE = new Vector3f();

    private ArmViewPose() {
    }

    public static Vector3f shoulder(boolean left, Vector3f out) {
        return out.set(SHOULDER).mul(left ? -1 : 1, 1, 1);
    }

    public static float solve(Vector3fc wrist, Vector3f shoulder, Vector3fc pole, PdaArms.Lengths lengths,
                              Matrix4f upper, Matrix4f lower, Vector3f elbow) {
        float l1 = lengths.upper(), l2 = lengths.forearm();
        DIR.set(wrist).sub(shoulder);
        float d = DIR.length();
        if (d < 1e-6F) {
            DIR.set(0, 0, -1);
        } else {
            DIR.div(d);
        }
        float reach = Math.clamp(d, Math.abs(l1 - l2) + 1e-4F, (l1 + l2) * MAX_REACH);
        if (reach != d) {
            shoulder.set(wrist).fma(-reach, DIR);
        }
        float along = (l1 * l1 - l2 * l2 + reach * reach) / (2 * reach);
        float bend = (float) Math.sqrt(Math.max(l1 * l1 - along * along, 0));
        PERP.set(pole).fma(-pole.dot(DIR), DIR);
        if (PERP.lengthSquared() < 1e-10F) {
            PERP.set(DIR.y, -DIR.x, 0);
        }
        PERP.normalize();
        elbow.set(shoulder).fma(along, DIR).fma(bend, PERP);
        FOREARM.set(wrist).sub(elbow).normalize();
        UPPER.set(elbow).sub(shoulder).normalize();
        OUT.set(PERP).mul(along / l1).fma(-bend / l1, DIR);
        HINGE.set(UPPER).cross(OUT);
        upper.set(HINGE.x, HINGE.y, HINGE.z, 0, UPPER.x, UPPER.y, UPPER.z, 0, OUT.x, OUT.y, OUT.z, 0,
              shoulder.x(), shoulder.y(), shoulder.z(), 1);
        OUT.set(HINGE).cross(FOREARM);
        lower.set(HINGE.x, HINGE.y, HINGE.z, 0, FOREARM.x, FOREARM.y, FOREARM.z, 0, OUT.x, OUT.y, OUT.z, 0,
              elbow.x, elbow.y, elbow.z, 1);
        return 0;
    }
}
