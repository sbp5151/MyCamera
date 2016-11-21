package com.jld.camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Camera录像
 * <p>
 * 以上是拍照过程，当如果切换成视频录制模式时，将遵循以下步骤：
 * <p>
 * 获取一个初始化Camera,且开始预览
 * 调用unlock方法允许media进程去访问camera
 * 通过camera去setCamera(Camera)，了解更多，可看MediaRecorder
 * 当完成录制时，调用reconnect方法重新获取且重新lock camera
 * 调用stopPreview()方法且release方法时，作为以上描述
 * 这个类是一个非线程安全类，意味着在被使用任何工作线程中，大多数长运行操作（preview,focus,photo capture 等）发生，
 * 异步调用回调是必要的。回调被调用在事件线程，Camera方法决不能一次被多个线程调用
 * 不同的安卓设备可能有不同的硬件规格，如像素的评级和自动对焦功能。
 */
public class VcrFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private TextView mTimeFen;
    private TextView mTimeMiao;
    private ImageButton mBtnClick;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private boolean mIsSufaceCreated;
    public static final String TAG = "VcrFragment";
    private SurfaceHolder mHolder;
    private MediaRecorder mRecorder;
    private boolean mIsRecording;
    private AnimatorSet mSet;
    private StateInterface mStateInterface;
    Handler mHandler = new Handler();
    private Activity mActivity;
    private RelativeLayout mPauseRelativeLayout;
    private LinearLayout mTimeLinerLayout;
    private ImageButton mPauseImageButton;
    private boolean mIsPause;

    public VcrFragment() {
    }
    public void setInterface(StateInterface stateInterface){
        mStateInterface = stateInterface;
    }
    public interface StateInterface {
        void vcrState();
        void vcrStop();
    }

    public static VcrFragment newInstance(String param1, String param2) {
        VcrFragment fragment = new VcrFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vcr, container, false);
        initView(view);
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(mBtnClick, "scaleX", 1.0f, 0.8f, 1.0f);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(mBtnClick, "scaleY", 1.0f, 0.8f, 1.0f);
        animatorX.setRepeatCount(-1);
        animatorY.setRepeatCount(-1);
        mSet = new AnimatorSet();
        mSet.playTogether(animatorX, animatorY);
        mSet.setDuration(1000);
        return view;
    }

    private void initView(View view) {
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        mTimeFen = (TextView) view.findViewById(R.id.tv_time_fen);
        mTimeMiao = (TextView) view.findViewById(R.id.tv_time_miao);
        mBtnClick = (ImageButton) view.findViewById(R.id.imageButton);
        mPauseRelativeLayout = (RelativeLayout) view.findViewById(R.id.pauseRelativeLayout);
        mTimeLinerLayout = (LinearLayout) view.findViewById(R.id.timeLinearLayout);
        mPauseImageButton = (ImageButton) view.findViewById(R.id.pauseImageButton);
        mTimeLinerLayout.setVisibility(View.GONE);
        initParam();
    }

    private void initParam() {
        mBtnClick.setOnClickListener(this);
        mPauseImageButton.setOnClickListener(this);
        mPauseRelativeLayout.setVisibility(View.GONE);
        mHolder = mSurfaceView.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mIsSufaceCreated = true;
                if (mCamera == null)
                    mCamera = Camera.open();
                try {
                    mCamera.setPreviewDisplay(mHolder);
                    initCamera();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        if (b) {
                            initCamera();
                            mCamera.cancelAutoFocus();
                        }
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mIsSufaceCreated = false;
                stopPreview();
            }
        });
    }

    //启动预览
    private void initCamera() {
        Camera.Parameters parameters = mCamera.getParameters();
        List focusAreas = parameters.getSupportedFlashModes();
        if (focusAreas.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
        } else if (focusAreas.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);//自动对焦
        }
        parameters.setPreviewFrameRate(20);
        //设置相机预览方向
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.pauseImageButton://暂停或者继续按钮
                if (mIsPause) {//继续
                    if (mRecorder != null) {
                        mRecorder.resume();//重启录制
                        mPauseImageButton.setImageResource(R.mipmap.vcr_pause);
                        mSet.resume();//重启动画
                        mHandler.postDelayed(timeRun, 1000);//启动计时
                        mIsPause = false;
                    }
                } else {//暂停
                    if (mRecorder != null) {
                        mRecorder.pause();
                        mPauseImageButton.setImageResource(R.mipmap.vcr_play);
                        if (mSet.isRunning())
                            mSet.pause();
                        mHandler.removeCallbacks(timeRun);
                        mIsPause = true;
                    }
                }
                break;
            case R.id.imageButton://开始或者停止录制按钮
                if (mIsRecording) {
                    stopRecording();
                } else {
                    initMediaRecorder();
                    startRecording();
                }
                break;
        }
    }

    /**
     * 初始化MediaRecorder
     */
    private void initMediaRecorder() {
        //实例化
        mRecorder = new MediaRecorder();
        mCamera.unlock();
        //给Recorder设置Camera对象，保证录像跟预览的方向保持一致
        mRecorder.setCamera(mCamera);
        mRecorder.setOrientationHint(90);  //改变保存后的视频文件播放时是否横屏(不加这句，视频文件播放的时候角度是反的)
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 设置从麦克风采集声音
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 设置从摄像头采集图像
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);  // 设置视频的输出格式 为MP4
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); // 设置音频的编码格式
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP); // 设置视频的编码格式
        mRecorder.setVideoSize(1280, 720);  // 设置视频大小

        mRecorder.setVideoEncodingBitRate(600 * 600);
        mRecorder.setVideoFrameRate(30);// 设置帧率
        mHolder.setKeepScreenOn(true);//保证屏幕不自动熄灭
//        mRecorder.setMaxDuration(10000); //设置最大录像时间为10s
        mRecorder.setPreviewDisplay(mHolder.getSurface());

        //设置视频存储路径
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "GlassVideo");
        if (!file.exists()) {
            //多级文件夹的创建
            file.mkdirs();
        }
        mRecorder.setOutputFile(file.getPath() + File.separator + "glass_" + System.currentTimeMillis() + ".mp4");
    }

    /**
     * 开始录制
     */
    private void startRecording() {
        if (mRecorder != null) {
            try {
                mRecorder.prepare();
                mRecorder.start();
                mIsRecording = true;
                timeNum = 0;
                mHandler.postDelayed(timeRun, 1000);
                mSet.start();
                mStateInterface.vcrState();//向activity提供停止录制回调监听
                mPauseRelativeLayout.setVisibility(View.VISIBLE);//显示暂停按钮
                mTimeLinerLayout.setVisibility(View.VISIBLE);//显示录制时间
            } catch (Exception e) {
                mIsRecording = false;
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * 停止录制
     */
    private void stopRecording() {
        if (mCamera != null) {
            mCamera.lock();
        }
        if (mRecorder != null) {
            Log.d(TAG, "mIsPause:" + mIsPause);
            if (mIsPause) {
                mIsPause = false;
                mRecorder.resume();
                mPauseImageButton.setImageResource(R.mipmap.vcr_pause);
            }
            try {
                mRecorder.stop();
            } catch (Exception e) {
                Log.d(TAG, "Exception:" + e.toString());

            }
            mRecorder.release();
            mRecorder = null;
        }
        if (mSet.isStarted())
            mSet.end();
        mStateInterface.vcrStop();//向activity提供停止录制回调监听
        mPauseRelativeLayout.setVisibility(View.GONE);//隐藏暂停按钮
        mTimeLinerLayout.setVisibility(View.GONE);//隐藏录制时间按钮
        mHandler.removeCallbacks(timeRun);
        mTimeFen.setText("00");
        mTimeMiao.setText("00");
        mIsRecording = false;
        //重启预览
    }

    /**
     * 释放预览
     */
    private void stopPreview() {
        //释放Camera对象
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private int timeNum = 0;
    private int fen = 0;
    private int miao = 0;
    private int shi = 0;

    Runnable timeRun = new Runnable() {
        @Override
        public void run() {
            updateTimestamp();
            mHandler.postDelayed(this, 1000);
        }
    };

    private void updateTimestamp() {
        timeNum++;
        Log.d(TAG, "timeNum: " + timeNum);
        if (timeNum < 10) {
            mTimeMiao.setText("0" + String.valueOf(timeNum));
        } else if (timeNum < 60) {
            mTimeMiao.setText(String.valueOf(timeNum));
        } else if (timeNum >= 60 && timeNum < 60 * 10) {
            fen = timeNum / 60;
            miao = timeNum % 60;
            mTimeMiao.setText(String.valueOf(miao));
            mTimeFen.setText("0" + String.valueOf(fen));
        } else if (timeNum >= 60 && timeNum < 60 * 60) {
            fen = timeNum / 60;
            miao = timeNum % 60;
            mTimeMiao.setText(String.valueOf(miao));
            mTimeFen.setText(String.valueOf(fen));
        } else if (timeNum >= 60 * 60 && timeNum < 60 * 60 * 3) {//如果时间超过一个小时，那么秒转为分，分转为时
            shi = timeNum / (60 * 60);
            fen = timeNum % (60 * 60);
            mTimeMiao.setText(String.valueOf(fen));
            mTimeFen.setText(String.valueOf(shi));
        } else {
            stopRecording();
            Toast.makeText(mActivity, getString(R.string.TimeOutHint), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        mSet.cancel();
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
            }
        }
        mHandler.removeCallbacks(timeRun);
        stopPreview();
        super.onDestroy();
    }

}
