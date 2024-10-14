package com.x_viria.app.light.corelens.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.x_viria.app.light.corelens.core.callback.ImageReaderCallback;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Camera {

    private final Activity ACTIVITY;
    private final CameraManager CAMERA_MANAGER;

    private CameraCaptureSession CAMERA_CAPTURE_SESSION;
    private CameraDevice CAMERA_DEVICE;
    private String CAMERA_ID;
    private CaptureRequest.Builder CAPTURE_REQUEST_BUILDER;
    private TextureView TEXTURE_VIEW;

    private int LENS_FACING = CameraCharacteristics.LENS_FACING_BACK;

    public Camera(Activity activity) {
        this.ACTIVITY = activity;
        this.CAMERA_MANAGER = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean getAE() {
        Integer ae_mode = CAPTURE_REQUEST_BUILDER.get(CaptureRequest.CONTROL_AE_MODE);
        if (ae_mode == null) return true;
        return ae_mode == CaptureRequest.CONTROL_AE_MODE_ON;
    }

    public int getFacing() {
        return LENS_FACING;
    }

    public double getFocalLength(String id) throws CameraAccessException {
        CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(id);
        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        if (focalLengths != null && sensorSize != null) {
            double sensorDiagonal = Math.sqrt(Math.pow(sensorSize.getWidth(), 2.0) + Math.pow(sensorSize.getHeight(), 2.0));
            for (float focalLength : focalLengths) {
                return (focalLength * 43.2666) / sensorDiagonal;
            }
        }
        return -1;
    }

    public String getId() {
        return CAMERA_ID;
    }

    public List<String> getIds(int facing) throws CameraAccessException {
        List<String> list = new ArrayList<>();
        for (String id : CAMERA_MANAGER.getCameraIdList()) {
            CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(id);
            int[] caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            boolean logical = false;
            assert caps != null;
            for (int cap : caps) {
                if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                    logical = true;
                    break;
                }
            }
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (
                    lensFacing != null
                    && lensFacing == facing
                    && !logical
            ) {
                list.add(id);
            }
        }
        return list;
    }

    public void getAspectList() throws CameraAccessException {
        CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(CAMERA_ID);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map != null) {
            Size[] outputSizes = map.getOutputSizes(ImageFormat.JPEG);
            for (Size size : outputSizes) {
                int width = size.getWidth();
                int height = size.getHeight();
                float aspectRatio = (float) width / height;
                Log.d("CameraAspectRatio", "Supported size: " + width + "x" + height + ", Aspect ratio: " + aspectRatio);
            }
        }
    }

    public int getISO() {
        Integer iso = CAPTURE_REQUEST_BUILDER.get(CaptureRequest.SENSOR_SENSITIVITY);
        if (iso == null) iso = -1;
        return iso;
    }

    public Range<Integer> getISORange(String id) throws CameraAccessException {
        CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(id);
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
    }

    public long getSS() {
        Long ss = CAPTURE_REQUEST_BUILDER.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
        if (ss == null) ss = -1L;
        return ss;
    }

    public Range<Long> getSSRange(String id) throws CameraAccessException {
        CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(id);
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
    }

    public void close() {
        if (CAMERA_DEVICE != null) {
            CAMERA_DEVICE.close();
            CAMERA_DEVICE = null;
        }
    }

    @RequiresPermission("android.permission.CAMERA")
    public void flip() throws CameraAccessException {
        if (LENS_FACING == CameraCharacteristics.LENS_FACING_BACK) {
            LENS_FACING = CameraCharacteristics.LENS_FACING_FRONT;
            open();
        } else {
            LENS_FACING = CameraCharacteristics.LENS_FACING_BACK;
            open();
        }
    }

    @RequiresPermission("android.permission.CAMERA")
    public void open() throws CameraAccessException {
        String[] cameraIdList = CAMERA_MANAGER.getCameraIdList();
        for (String id : cameraIdList) {
            CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(id);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing != null && lensFacing == LENS_FACING) {
                open(id);
                break;
            }
        }
    }

    @RequiresPermission("android.permission.CAMERA")
    public void open(String id) {
        try {
            close();
            CAMERA_ID = id;
            CAMERA_MANAGER.openCamera(id, STATE_CALLBACK, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setISO(int iso) {
        if (iso == -1) {
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        } else {
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        }
        updatePreview();
    }

    public void setSS(long ns) {
        if (ns == -1L) {
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        } else {
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            CAPTURE_REQUEST_BUILDER.set(CaptureRequest.SENSOR_EXPOSURE_TIME, ns);
        }
        updatePreview();
    }

    public void setTextureView(TextureView textureView) {
        this.TEXTURE_VIEW = textureView;
    }

    public void takePicture(ImageReaderCallback ir_callback) {
        if (CAMERA_DEVICE == null) return;
        try {
            CameraCharacteristics characteristics = CAMERA_MANAGER.getCameraCharacteristics(CAMERA_DEVICE.getId());
            Size[] jpegSizes = Objects.requireNonNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                    .getOutputSizes(ImageFormat.JPEG);

            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(TEXTURE_VIEW.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = CAMERA_DEVICE.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = ACTIVITY.getWindowManager().getDefaultDisplay().getRotation();
            int jpegOrientation = getJpegOrientation(characteristics, rotation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        saveToMediaStore(bytes);
                    }
                }

                private void saveToMediaStore(byte[] bytes) {
                    new Media(ACTIVITY).savePicture(bytes);
                    ir_callback.saveToMediaStore(bytes);
                }
            };
            reader.setOnImageAvailableListener(readerListener, null);

            CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startCameraPreview(CAPTURE_REQUEST_BUILDER);
                }
            };

            CAMERA_DEVICE.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> validISOList(String id) throws CameraAccessException {
        Map<String, Integer> map = new LinkedHashMap<>();
        Map<String, Integer> list = new LinkedHashMap<>();
        list.put("Auto", -1);
        list.put("64", 64);
        list.put("80", 80);
        list.put("100", 100);
        list.put("125", 125);
        list.put("160", 160);
        list.put("200", 200);
        list.put("250", 250);
        list.put("320", 320);
        list.put("400", 400);
        list.put("500", 500);
        list.put("640", 640);
        list.put("800", 800);
        list.put("1000", 1000);
        list.put("1250", 1250);
        list.put("1600", 1600);
        list.put("2000", 2000);
        list.put("2500", 2500);
        list.put("3200", 3200);
        list.put("4000", 4000);
        list.put("5000", 5000);
        list.put("6400", 6400);
        for(Map.Entry<String, Integer> entry : list.entrySet()) {
            Range<Integer> range = getISORange(id);
            if (entry.getValue() == -1 || (range.getLower() <= entry.getValue() && entry.getValue() <= range.getUpper())) map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public Map<String, Long> validSSList(String id) throws CameraAccessException {
        Map<String, Long> map = new LinkedHashMap<>();
        Map<String, Long> list = new LinkedHashMap<>();
        list.put("Auto", -1L);
        list.put("1/10000", 1000000000L / 10000L);
        list.put("1/8000", 1000000000L / 8000L);
        list.put("1/6400", 1000000000L / 6400L);
        list.put("1/5000", 1000000000L / 5000L);
        list.put("1/4000", 1000000000L / 4000L);
        list.put("1/3200", 1000000000L / 3200L);
        list.put("1/2500", 1000000000L / 2500L);
        list.put("1/2000", 1000000000L / 2000L);
        list.put("1/1600", 1000000000L / 1600L);
        list.put("1/1250", 1000000000L / 1250L);
        list.put("1/1000", 1000000000L / 1000L);
        list.put("1/800", 1000000000L / 800L);
        list.put("1/640", 1000000000L / 640L);
        list.put("1/500", 1000000000L / 500L);
        list.put("1/400", 1000000000L / 400L);
        list.put("1/320", 1000000000L / 320L);
        list.put("1/250", 1000000000L / 250L);
        list.put("1/200", 1000000000L / 200L);
        list.put("1/160", 1000000000L / 160L);
        list.put("1/125", 1000000000L / 125L);
        list.put("1/100", 1000000000L / 100L);
        list.put("1/80", 1000000000L / 80L);
        list.put("1/60", 1000000000L / 60L);
        list.put("1/50", 1000000000L / 50L);
        list.put("1/40", 1000000000L / 40L);
        list.put("1/30", 1000000000L / 30L);
        list.put("1/25", 1000000000L / 25L);
        list.put("1/20", 1000000000L / 20L);
        list.put("1/15", 1000000000L / 15L);
        list.put("1/10", 1000000000L / 10L);
        list.put("1/8", 1000000000L / 8L);
        list.put("1/5", 1000000000L / 5L);
        list.put("1/4", 1000000000L / 4L);
        list.put("1/2", 1000000000L / 2L);
        list.put("1", 1000000000L);
        list.put("2", (long) (1000000000L / (1.0 / 2.0)));
        for(Map.Entry<String, Long> entry : list.entrySet()) {
            Range<Long> range = getSSRange(id);
            if (entry.getValue() == -1L || (range.getLower() <= entry.getValue() && entry.getValue() <= range.getUpper())) map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private final CameraDevice.StateCallback STATE_CALLBACK = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            CAMERA_DEVICE = camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            CAMERA_DEVICE.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (CAMERA_DEVICE != null) CAMERA_DEVICE.close();
        }
    };

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void startCameraPreview(CaptureRequest.Builder builder) {
        try {
            SurfaceTexture texture = TEXTURE_VIEW.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(TEXTURE_VIEW.getWidth(), TEXTURE_VIEW.getHeight());
            Surface surface = new Surface(texture);

            CAPTURE_REQUEST_BUILDER = builder;

            CAMERA_DEVICE.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (CAMERA_DEVICE == null) {
                        return;
                    }
                    CAMERA_CAPTURE_SESSION = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(ACTIVITY, "Failed: " + session, Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCameraPreview() {
        try {
            SurfaceTexture texture = TEXTURE_VIEW.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(TEXTURE_VIEW.getWidth(), TEXTURE_VIEW.getHeight());
            Surface surface = new Surface(texture);

            CAPTURE_REQUEST_BUILDER = CAMERA_DEVICE.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            CAPTURE_REQUEST_BUILDER.addTarget(surface);

            CAMERA_DEVICE.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (CAMERA_DEVICE == null) {
                        return;
                    }
                    CAMERA_CAPTURE_SESSION = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(ACTIVITY, "Failed: " + session, Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (CAMERA_DEVICE == null) {
            return;
        }
        CAPTURE_REQUEST_BUILDER.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            CAMERA_CAPTURE_SESSION.setRepeatingRequest(CAPTURE_REQUEST_BUILDER.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
