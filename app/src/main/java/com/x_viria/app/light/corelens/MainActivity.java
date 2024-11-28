package com.x_viria.app.light.corelens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.x_viria.app.light.corelens.core.Camera;
import com.x_viria.app.light.corelens.core.Media;
import com.x_viria.app.light.corelens.core.callback.ImageReaderCallback;
import com.x_viria.app.light.corelens.ui.overlay.PopupOptions;
import com.x_viria.app.light.corelens.utils.Pixels;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;

    private Camera CAMERA;

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 100;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 120;


    private void refreshUI() throws CameraAccessException {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Test", Toast.LENGTH_SHORT).show();
            requestAllPermissions();
        }

        TextView lens_tv = findViewById(R.id.MainActivity__Header_Lens_TV);
        lens_tv.setText(String.format("%.0fmm", Math.ceil(CAMERA.getFocalLength(CAMERA.getId()))));
        TextView ss_tv = findViewById(R.id.MainActivity__Header_SS_TV);
        Map<String, Long> map_ss = CAMERA.validSSList(CAMERA.getId());
        String key_ss = "Auto";
        TextView iso_tv = findViewById(R.id.MainActivity__Header_ISO_TV);
        Map<String, Integer> map_iso = CAMERA.validISOList(CAMERA.getId());
        String key_iso = "Auto";
        if (!CAMERA.getAE()) {
            for (Map.Entry<String, Long> entry : map_ss.entrySet()) {
                if (entry.getValue() == CAMERA.getSS()) {
                    key_ss = entry.getKey() + "s";
                    break;
                }
            }
            for (Map.Entry<String, Integer> entry : map_iso.entrySet()) {
                if (entry.getValue() == CAMERA.getISO()) {
                    key_iso = entry.getKey();
                    break;
                }
            }
        }
        ss_tv.setText(key_ss);
        iso_tv.setText(key_iso);

        SeekBar Zoom_SB = findViewById(R.id.MainActivity__Zoom_SB);
        try {
            Log.d("", CAMERA.getId() + " - " + String.valueOf(CAMERA.getMaxZoom()));
            Zoom_SB.setMax((int) (CAMERA.getMaxZoom() * 10.0f));
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

        ImageView LastTaken_IV = findViewById(R.id.MainActivity__LastTaken_IV);
        LastTaken_IV.setImageURI(new Media(this).getLastTakenImageUri());

        Map<String, Pixels.Info> aspectList = CAMERA.getAspectList();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setContentView(R.layout.activity_main);

        TextureView textureView = findViewById(R.id.MainActivity__TextureView);

        CAMERA = new Camera(MainActivity.this);
        CAMERA.setTextureView(textureView);

        textureView.setSurfaceTextureListener(textureListener);

        SeekBar Zoom_SB = findViewById(R.id.MainActivity__Zoom_SB);
        TextView Zoom_TV = findViewById(R.id.MainActivity__Zoom_TV);

        LinearLayout Lens_LL = findViewById(R.id.MainActivity__Header_Lens_LL);
        Lens_LL.setOnClickListener(v -> {
            int facing = CAMERA.getFacing();
            try {
                Map<String, String> map = new LinkedHashMap<>();
                List<String> ids = CAMERA.getIds(facing);
                for (String id : ids) {
                    double focalLength = Math.ceil(CAMERA.getFocalLength(id));
                    String fmt = String.format("%.0fmm", focalLength);
                    map.put(fmt, id);
                }
                PopupOptions<String> popupOptions = new PopupOptions<>(MainActivity.this);
                popupOptions.setTitle("Lens");
                popupOptions.show(v, map, new PopupOptions.PopupOptionsCallback<String>() {
                    @SuppressLint("MissingPermission")
                    public void onClick(String key, String id) {
                        CAMERA.open(id);
                        Zoom_SB.setProgress(10);
                        Zoom_TV.setText("x1.0");
                        TextView lens_tv = findViewById(R.id.MainActivity__Header_Lens_TV);
                        lens_tv.setText(key);
                        try {
                            refreshUI();
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        });

        LinearLayout SS_LL = findViewById(R.id.MainActivity__Header_SS_LL);
        SS_LL.setOnClickListener(v -> {
            try {
                Map<String, Long> map = CAMERA.validSSList(CAMERA.getId());
                PopupOptions<Long> popupOptions = new PopupOptions<>(MainActivity.this);
                popupOptions.setTitle("SS");
                popupOptions.show(v, map, new PopupOptions.PopupOptionsCallback<Long>() {
                    @SuppressLint("MissingPermission")
                    public void onClick(String key, Long ns) {
                        try {
                            CAMERA.setSS(ns);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        TextView ss_tv = findViewById(R.id.MainActivity__Header_SS_TV);
                        if (ns == -1L) {
                            ss_tv.setText(key);
                        } else {
                            ss_tv.setText(String.format("%ss", key));
                        }
                        try {
                            refreshUI();
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        });

        LinearLayout ISO_LL = findViewById(R.id.MainActivity__Header_ISO_LL);
        ISO_LL.setOnClickListener(v -> {
            try {
                Map<String, Integer> map = CAMERA.validISOList(CAMERA.getId());
                PopupOptions<Integer> popupOptions = new PopupOptions<>(MainActivity.this);
                popupOptions.setTitle("ISO");
                popupOptions.show(v, map, new PopupOptions.PopupOptionsCallback<Integer>() {
                    @SuppressLint("MissingPermission")
                    public void onClick(String key, Integer iso) {
                        try {
                            CAMERA.setISO(iso);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        TextView iso_tv = findViewById(R.id.MainActivity__Header_ISO_TV);
                        if (iso == -1) {
                            iso_tv.setText(key);
                        } else {
                            iso_tv.setText(String.format("%s", key));
                        }
                        try {
                            refreshUI();
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        });

        Zoom_SB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = (float) progress / 10.0f;
                Zoom_TV.setText(String.format("x%.1f", scale));
                try {
                    CAMERA.setZoom(scale);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        new Handler().postDelayed(() -> {
            try {
                Zoom_SB.setMax((int) (CAMERA.getMaxZoom() * 10.0f));
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }, 1000);

        ImageView Settings_IV = findViewById(R.id.MainActivity__Settings_IV);
        Settings_IV.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        CardView Image_CV = findViewById(R.id.MainActivity__Image_CV);
        Image_CV.setOnClickListener(v -> {
        });

        CardView Grab_CV = findViewById(R.id.MainActivity__Grab);
        Grab_CV.setOnClickListener(v -> {
            LinearLayout Wait_LL = findViewById(R.id.MainActivity__Wait_LL);
            Wait_LL.setVisibility(View.VISIBLE);
            CAMERA.takePicture(new ImageReaderCallback() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    super.onImageAvailable(reader);
                }

                @Override
                public void saveToMediaStore(byte[] bytes) {
                    new Handler().postDelayed(() -> {
                        try {
                            Wait_LL.setVisibility(View.INVISIBLE);
                            refreshUI();
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }, 1000);
                }
            });
        });

        ImageView Flip_IV = findViewById(R.id.MainActivity__Flip);
        Flip_IV.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestAllPermissions();
                return;
            }
            try {
                CAMERA.flip();
                refreshUI();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            refreshUI();
        } catch (RuntimeException | CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void requestAllPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        }
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestAllPermissions();
                    return;
                }
                CAMERA.open();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        CAMERA.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestAllPermissions();
            return;
        }
        try {
            CAMERA.open();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    CAMERA.open();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    refreshUI();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}