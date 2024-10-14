package com.x_viria.app.light.corelens.utils;

import android.content.Context;

public class Unit {

    public static class Pixel {

        public static int dp2px(Context context, float dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }

    }

}
