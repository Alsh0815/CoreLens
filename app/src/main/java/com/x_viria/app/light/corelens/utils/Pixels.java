package com.x_viria.app.light.corelens.utils;

public class Pixels {

    private static float _gcd(float x, float y) {
        if (y == 0) return x;
        return _gcd(y, x % y);
    }

    public static float[] getAspectRatio(float width, float height) {
        float g = _gcd(width, height);
        return new float[] {width / g, height / g};
    }

    public static class Info {
        public int width = 0;
        public int height = 0;
        public float aspectRatio = 0.0f;
        public float aspectX = 0.0f;
        public float aspectY = 0.0f;
    }

}
