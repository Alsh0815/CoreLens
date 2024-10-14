package com.x_viria.app.light.corelens.ui.overlay;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.x_viria.app.light.corelens.R;
import com.x_viria.app.light.corelens.utils.Unit;

import java.security.Key;
import java.util.Map;

public class PopupOptions<T> {

    private T t;
    private final Context CONTEXT;
    private String TITLE;

    public PopupOptions(Context context) {
        this.CONTEXT = context;
    }

    public void setTitle(String title) {
        this.TITLE = title;
    }

    public void show(View v, Map<String, T> map, PopupOptionsCallback callback) {
        LayoutInflater inflater = (LayoutInflater) CONTEXT.getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_options, null);

        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        ((TextView) popupView.findViewById(R.id.popup_title)).setText(TITLE);

        int padding = Unit.Pixel.dp2px(CONTEXT, 4);
        LinearLayout options_ll = popupView.findViewById(R.id.popup_options);
        for (Map.Entry<String, T> entry : map.entrySet()) {
            LinearLayout p = new LinearLayout(CONTEXT);
            p.setBackgroundColor(CONTEXT.getColor(R.color.black));
            ViewGroup.MarginLayoutParams mlp = new LinearLayout.LayoutParams(
                    Unit.Pixel.dp2px(CONTEXT, 88),
                    Unit.Pixel.dp2px(CONTEXT, 48)
            );
            mlp.setMargins(padding, padding, padding, padding);
            p.setLayoutParams(mlp);
            p.setGravity(Gravity.CENTER);
            p.setOnClickListener(v1 -> {
                callback.onClick(entry.getKey(), entry.getValue());
                popupWindow.dismiss();
            });
            p.setPadding(padding, padding, padding, padding);
            TextView t = new TextView(CONTEXT);
            t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            t.setText(entry.getKey());
            t.setTextColor(CONTEXT.getColor(R.color.CameraOptionTextColor));
            p.addView(t);
            options_ll.addView(p);
        }

        popupWindow.setAnimationStyle(R.style.PopupWindowTipsAnimation);
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
    }

    public static class PopupOptionsCallback<T> {

        public void onClick(String key, T id) {}

    }

}
