/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.chr.screenrecorder.ui.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;

import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.chr.screenrecorder.IScreenRecorderAidlInterface;
import net.chr.screenrecorder.R;
import net.chr.screenrecorder.core.RESAudioClient;
import net.chr.screenrecorder.core.RESCoreParameters;
import net.chr.screenrecorder.media.MediaAudioEncoder;
import net.chr.screenrecorder.media.MediaMuxerWrapper;
import net.chr.screenrecorder.media.MediaScreenEncoder;

import net.chr.screenrecorder.media.MediaEncoder;

import net.chr.screenrecorder.model.DanmakuBean;
import net.chr.screenrecorder.rtmp.RESFlvData;
import net.chr.screenrecorder.rtmp.RESFlvDataCollecter;
import net.chr.screenrecorder.service.ScreenRecordListenerService;
import net.chr.screenrecorder.task.RtmpStreamingSender;
import net.chr.screenrecorder.task.ScreenRecorder;
import net.chr.screenrecorder.tools.LogTools;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class ScreenRecordActivity extends FragmentActivity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private Button mButton;
    private Button mButtonReset;
    private EditText mRtmpAddET;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecorder mVideoRecorder;
    private RESAudioClient audioClient;
    private RtmpStreamingSender streamingSender;
    private ExecutorService executorService;
    private List<DanmakuBean> danmakuBeanList = new ArrayList<>();
    private String rtmpAddr;
    private boolean isRecording;
    private RESCoreParameters coreParameters;
    private int mScreenDensity;

    private  ScreenCaptureFragment fragment;

    private String  intentValue,intentValue2;
    WindowManager wm;
    private Button btn_floatView;
    Handler han;
    private static MediaMuxerWrapper sMuxer;
    private Handler mHandler;


    SurfaceView mSurfaceView ;
    Surface mSurface ;
    TextureView mTextureView;

    public static void launchActivity(Context ctx) {
        Intent it = new Intent(ctx, ScreenRecordActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(it);
    }

    private IScreenRecorderAidlInterface recorderAidlInterface;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recorderAidlInterface = IScreenRecorderAidlInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            recorderAidlInterface = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (savedInstanceState == null) {
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            fragment = new ScreenCaptureFragment();
//            transaction.replace(R.id.sample_content_fragment, fragment);
//            transaction.commit();
//
//        }
//lxk
        mTextureView = (TextureView) findViewById(R.id.textureView);
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mButton = findViewById(R.id.button);
        mRtmpAddET = findViewById(R.id.et_rtmp_address);

        mButton.setOnClickListener(this);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurface = mSurfaceView.getHolder().getSurface();


        Intent intent = getIntent();
        intentValue = intent.getStringExtra("arge1");

        int port = 1936;
        if (intentValue != null) {
            mRtmpAddET.setText("rtmp://" + intentValue + ":" + port + "/live2/video");
            mRtmpAddET.setVisibility(View.INVISIBLE);
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("net.chr.screenrecorder");
        registerReceiver(receiver, intentFilter);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.e("ScreenRecordActivity", "handleMessage00: received:" + msg);

                switch (msg.what) {
                    case 1:
                        onClick(null);
                        break;
                    default:
                        break;
                }
            }

        };
        sendMessage(null);

    }

    public void sendMessage(String message)
    {
        Log.e("ScreenRecordActivity", "sendMessage: "+message);
        Message msg = mHandler.obtainMessage();// new Message();
        msg.what = 1;
        msg.obj = message;

        mHandler.sendMessage(msg);
    }
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        //setIntent(intent);//must store the new intent unless getIntent() will return the old one
        Log.e("ScreenRecordActivity", "onNewIntent: *********");

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("ScreenRecordActivity", "onActivityResult: "+"onActivityResult" );
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }

        rtmpAddr = mRtmpAddET.getText().toString().trim();
        if (TextUtils.isEmpty(rtmpAddr)) {
            Toast.makeText(this, "rtmp address cannot be null", Toast.LENGTH_SHORT).show();
            return;
        }
        streamingSender = new RtmpStreamingSender();
        streamingSender.sendStart(rtmpAddr);
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                streamingSender.sendFood(flvData, type);
            }
        };
 //lxk       coreParameters = new RESCoreParameters();

// lxk       audioClient = new RESAudioClient(coreParameters);
//
//        if (!audioClient.prepare()) {
//            LogTools.d("!!!!!audioClient.prepare()failed");
//            return;
//        }
    if(false) {
        mVideoRecorder = new ScreenRecorder(collecter, RESFlvData.VIDEO_WIDTH, RESFlvData.VIDEO_HEIGHT, RESFlvData.VIDEO_BITRATE, mScreenDensity, mediaProjection, mSurfaceView
                , mTextureView);
        mVideoRecorder.start();
        //lxk audioClient.start(collecter);

    }else
    {
        try
        {
            sMuxer = new MediaMuxerWrapper(this, ".mp4");
            MediaScreenEncoder mse = new MediaScreenEncoder(sMuxer,collecter, mMediaEncoderListener,
                    mediaProjection, RESFlvData.VIDEO_WIDTH, RESFlvData.VIDEO_HEIGHT, RESFlvData.VIDEO_HEIGHT, 800 * 1024, 15);
           // new MediaAudioEncoder(sMuxer,collecter, mMediaEncoderListener);
            sMuxer.prepare();
            sMuxer.startRecording();



        }catch (Exception e)
        {
          e.printStackTrace();
        }

    }
        executorService = Executors.newCachedThreadPool();
        executorService.execute(streamingSender);


        mButton.setText("Stop Recorder");
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }


    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            Log.v("ScreenRecordActivity", "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            Log.v("ScreenRecordActivity", "onStopped:encoder=" + encoder);
        }
    };

    private void createFloatView()
    {
        //Button btn_floatView = new Button();
        btn_floatView = new Button(getApplicationContext());
        btn_floatView.setText("开始时间：");

        wm = (WindowManager) getApplicationContext().getSystemService(
                Context.WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        // 设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        /*
         * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
         * 即拉下通知栏不可见
         */

        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        /*
         * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
         * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
         * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
         */

        // 设置悬浮窗的长得宽
        params.width = 400;
        params.height = 300;


        han = new Handler() {
            int ii=0;
            public void handleMessage(android.os.Message msg) {
                btn_floatView.setText("开始时间："+ii++);
            };
        };

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    while(true)
                    {
                        han.sendEmptyMessage(0x01);
                        Thread.sleep(1000);
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        // 设置悬浮窗的Touch监听
        btn_floatView.setOnTouchListener(new OnTouchListener()
        {
            int lastX, lastY;
            int paramX, paramY;

            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = params.x;
                        paramY = params.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        params.x = paramX + dx;
                        params.y = paramY + dy;
                        // 更新悬浮窗位置
                        wm.updateViewLayout(btn_floatView, params);
                        break;
                    case MotionEvent.ACTION_BUTTON_PRESS:

                        break;
                }
                return true;
            }
        });

        wm.addView(btn_floatView, params);
      //  isAdded = true;
    }



    @Override
    public void onClick(View v) {
        Log.e("mVideoRecorder", "onClick: "+mVideoRecorder );
      //  if (mVideoRecorder != null)
        if (sMuxer != null )
        {
            stopScreenRecord();
            sMuxer.stopRecording();
            sMuxer = null;
            mButton.setText("Start");
        } else {
            createScreenCapture();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        wm.removeViewImmediate(btn_floatView);
        if (mVideoRecorder != null) {
            stopScreenRecord();
        }
        if (sMuxer != null )
        {
            Log.e("mVideoRecorder", "onDestroy: Stopping screen recording" );
            stopScreenRecord();
            sMuxer.stopRecording();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRecording) stopScreenRecordService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecording) startScreenRecordService();
    }

    private void startScreenRecordService() {
        if (mVideoRecorder != null && mVideoRecorder.getStatus()) {
            Intent runningServiceIT = new Intent(this, ScreenRecordListenerService.class);
            bindService(runningServiceIT, connection, BIND_AUTO_CREATE);
            startService(runningServiceIT);
            startAutoSendDanmaku();
        }
    }

    private void startAutoSendDanmaku() {
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                int index = 0;
                while (true) {
                    DanmakuBean danmakuBean = new DanmakuBean();
                    danmakuBean.setMessage(String.valueOf(index++));
                    danmakuBean.setName("little girl");
                    danmakuBeanList.add(danmakuBean);
                    try {
                        if (recorderAidlInterface != null) {
                            recorderAidlInterface.sendDanmaku(danmakuBeanList);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void stopScreenRecordService() {
        Intent runningServiceIT = new Intent(this, ScreenRecordListenerService.class);
        stopService(runningServiceIT);
        if (mVideoRecorder != null && mVideoRecorder.getStatus()) {
            Toast.makeText(this, "现在正在进行录屏直播哦", Toast.LENGTH_SHORT).show();
        }
    }

    private void createScreenCapture() {
        isRecording = true;
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    private void stopScreenRecord() {
        if(mVideoRecorder != null)
        {
            mVideoRecorder.quit();
            mVideoRecorder = null;
        }
        if (streamingSender != null) {
            streamingSender.sendStop();
            streamingSender.quit();
            streamingSender = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        mButton.setText("Restart recorder");
    }

    public static class RESAudioBuff {
        public boolean isReadyToFill;
        public int audioFormat = -1;
        public byte[] buff;

        public RESAudioBuff(int audioFormat, int size) {
            isReadyToFill = true;
            this.audioFormat = audioFormat;
            buff = new byte[size];
        }
    }

}
