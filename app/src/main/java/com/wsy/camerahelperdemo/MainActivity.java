package com.wsy.camerahelperdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.wsy.camerahelperdemo.camera.CameraHelper;
import com.wsy.camerahelperdemo.camera.CameraListener;

import java.io.ByteArrayOutputStream;


public class MainActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "MainActivity";
    private static final String[] NEEDED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private CameraHelper cameraHelper;
    private View previewView;
    private ImageView ivPreview;
    private Integer cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static final int ACTION_REQUEST_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.texture_view);
        ivPreview = findViewById(R.id.iv_image);
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }

    @Override
    protected void onPause() {
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
        }
        super.onDestroy();
    }

    private void initCamera() {
        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Log.i(TAG, "onCameraOpened: ");
            }

            @Override
            public void onPreview(byte[] data, Camera camera) {
                Log.i(TAG, "onPreview: ");
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                Log.i(TAG, "onCameraConfigurationChanged: ");
            }

            @Override
            public void onTakePicture(int width, int height, CameraHelper.TakePictureType takePictureType, final int displayOrientation, final byte[] data) {
                if (takePictureType == CameraHelper.TakePictureType.JPG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            bitmap = getRotateBitmap(bitmap, 360 - displayOrientation);
                            ivPreview.setImageBitmap(bitmap);
                        }
                    });
                }else {
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,width,height,null);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0,0,width,height),100,baos);
                    byte[] nv21Data = baos.toByteArray();
                     try {
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = BitmapFactory.decodeByteArray(nv21Data,0,nv21Data.length);
                    bitmap = getRotateBitmap(bitmap, 360 - displayOrientation);
                    final Bitmap finalBitmap = bitmap;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivPreview.setImageBitmap(finalBitmap);
                        }
                    });
                }
            }
        };
        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .additionalRotation(0)
                .specificCameraId(cameraID != null ? cameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .takePictureType(CameraHelper.TakePictureType.NV21)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
        Log.i(TAG, "initCamera: ");
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onGlobalLayout() {

        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (checkPermissions(NEEDED_PERMISSIONS)) {
            initCamera();
        } else {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ACTION_REQUEST_PERMISSIONS:
                boolean isAllGranted = true;
                for (int grantResult : grantResults) {
                    isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
                }
                if (isAllGranted) {
                    initCamera();
                    if (cameraHelper != null) {
                        cameraHelper.start();
                    }
                } else {
                    Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }


    public void switchCamera(View view) {
        if (cameraHelper != null) {
            cameraHelper.switchCamera();
        }
    }

    boolean isFlashOn = false;

    public void switchFlash(View view) {
        if (cameraHelper != null) {
            if (isFlashOn) {
                isFlashOn = !cameraHelper.setFlashMode(CameraHelper.FlashMode.OFF);
                if (!isFlashOn) {
                    ((ImageView) view).setImageResource(R.drawable.ic_flash_on);
                }
            } else {
                isFlashOn = cameraHelper.setFlashMode(CameraHelper.FlashMode.TORCH);
                if (isFlashOn) {
                    ((ImageView) view).setImageResource(R.drawable.ic_flash_off);
                }
            }
        }
    }

    public void takePicture(View view) {
        if (cameraHelper != null) {
            cameraHelper.takePicture();
        }
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree) {
        if (b == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
    }
}
