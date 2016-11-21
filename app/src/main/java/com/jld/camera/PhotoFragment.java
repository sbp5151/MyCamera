package com.jld.camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.jld.camera.R.id.surfaceView;

/**
 * Camera拍照
 * <p>
 * 用Camera去拍照，将遵循以下几个步骤：
 * <p>
 * 获得一个Camera的实例，通过open方法
 * 如果必要的话，可以修改一些默认参数
 * 通过初始化SurfaceHolder去setPreviewDisplay(SurfaceHolder),没有Surface, camera不能开始预览
 * 调用startPreview方法开始更新预览的surface，在拍照前，预览（preview）必须被开启。
 * 当你想开始拍照时，使用takePicture(Camera.ShutterCallback,
 * Camera.PictureCallback, Camera.PictureCallback, Camera.PictureCallback), 等待回调提供真实的图像数据
 * 当拍完一张照片时，预览（preview）将会停止，当你想要拍更多的照片时，须要再一次调用startPreview方法
 * 当调用stopPreview方法时，将停止更新预览的surface
 * 当调用release方法时，将马上释放camera
 */
public class PhotoFragment extends Fragment implements Camera.PictureCallback {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    public static final String TAG = "PhotoFragment";
    private Camera.Parameters parameters;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ImageButton mImageButton;
    private Activity mActivity;

    public PhotoFragment() {
    }

    public static PhotoFragment newInstance(String param1, String param2) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mImageButton = (ImageButton) view.findViewById(R.id.imageButton);
        mSurfaceView = (SurfaceView) view.findViewById(surfaceView);
        mHolder = mSurfaceView.getHolder();
        // 设置Surface不需要维护自己的缓冲区
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated");
                if (null == mCamera)
                    mCamera = Camera.open();
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                    initCamera();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged");
                //实现自动对焦
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();//实现相机的参数初始化
                            camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                        }
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed");
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
            }
        });
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImageButton.setClickable(false);
                btnAnimator(mImageButton);
                mCamera.takePicture(null, null, PhotoFragment.this);
            }
        });
    }

    /**
     * 拍照按钮抖动动画
     *
     * @param view
     */
    private void btnAnimator(View view) {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.7f, 1.0f);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.7f, 1.0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animatorX, animatorY);
        set.setDuration(200);
        set.start();
    }

    //相机参数的初始化设置
    private void initCamera() {
        parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        setDisplay(parameters, mCamera);
        mCamera.setParameters(parameters);
        mCamera.startPreview();//开启预览
        mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    //控制图像的正确显示方向
    private void setDisplay(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }

    /**
     * 相片存储
     *
     * @param bytes
     * @param camera
     */
    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        Log.d(TAG, "onPictureTaken" + bytes.length);
        File file = getOutputMediaFile(bytes);
        if (file != null) {
            Log.d(TAG, "File:" + file.getAbsolutePath());
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(bytes);
                Toast.makeText(mActivity, getString(R.string.win), Toast.LENGTH_SHORT).show();
                outputStream.close();
                outputStream = null;
            } catch (FileNotFoundException e) {
                Log.d(TAG, "FileNotFoundException:" + e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "IOException:" + e.toString());
                e.printStackTrace();
            }
        }
        mImageButton.setClickable(true);
        mCamera.startPreview();//重新开启预览
    }

    private File getOutputMediaFile(byte[] bytes) {
        //get the mobile Pictures directory
//        File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);//获取相册目录
        //get the current time
        //存储卡是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (getAvailableExternalMemorySize() >= bytes.length) {//存储空间是否足够
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
                String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "GlassPhoto";
                File file = new File(Path);
                File file2 = null;
                Log.d(TAG, "file:" + file);
                if (!file.exists()) {
                    file.mkdirs();
                }
                file2 = new File(file.getAbsolutePath() + File.separator + timeStamp + "_glass.jpg");
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        Log.d(TAG, "createNewFile:" + e.toString());
                        Toast.makeText(mActivity, getString(R.string.code_exception), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        return null;
                    }
                }
                return file2;
            } else {
                Toast.makeText(mActivity, getString(R.string.code_size_outof), Toast.LENGTH_LONG).show();
                return null;
            }
        } else {
            Toast.makeText(mActivity, getString(R.string.code_exception), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    /**
     * 获取SDCARD剩余存储空间
     *
     * @return
     */
    public static long getAvailableExternalMemorySize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }
}
