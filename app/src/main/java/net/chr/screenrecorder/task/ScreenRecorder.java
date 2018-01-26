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

package net.chr.screenrecorder.task;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.TextView;

import net.chr.screenrecorder.core.Packager;
import net.chr.screenrecorder.rtmp.RESFlvData;
import net.chr.screenrecorder.rtmp.RESFlvDataCollecter;
import net.chr.screenrecorder.tools.LogTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


import static net.chr.screenrecorder.rtmp.RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
import static net.chr.screenrecorder.rtmp.RESFlvData.NALU_TYPE_IDR;

/**
 * @author Yrom
 * Modified by raomengyang 2017-03-12
 */
public class ScreenRecorder extends Thread {
    private static final String TAG = "ScreenRecorder";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 10; // 2 seconds between I-frames
    private static final int TIMEOUT_US = 10000;

    private MediaCodec mEncoder;
    private Surface mSurface;
    private Surface mSurface2;
    private SurfaceView mSurfaceView;
    private long startTime = 0;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;
    private RESFlvDataCollecter mDataCollecter;

    private static final String mVideoPath = Environment.getExternalStorageDirectory().getPath() + "/";
    File outputFile;
    FileOutputStream outputStream;

    SurfaceTexture mSurfaceTexture;
    TextureView mTextureView;

    public ScreenRecorder(RESFlvDataCollecter dataCollecter, int width, int height, int bitrate, int dpi, MediaProjection mp,
                          SurfaceView sfv, TextureView tv) {
        super(TAG);
        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        mDpi = dpi;
        mMediaProjection = mp;
        startTime = 0;
        mDataCollecter = dataCollecter;

        mTextureView = tv;

        //SurfaceTexture st = mTextureView.getSurfaceTexture();
      //  mSurface = new Surface(st);

         //mSurfaceTexture = new SurfaceTexture(10) ;

//        this.screenWidth=screenWidth;
//        this.screenHeight=screenHeight;
//        gRect=new Rect(0,0,screenWidth,screenHeight);


       // mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);	// これを入れないと映像が取れない
       // mSurface = new Surface(mSurfaceTexture);
       // mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);



        //mSurfaceView = sfv;
       // mSurface2  =  mSurfaceView.getHolder().getSurface();
       // mSurfaceView = new SurfaceView(null);

//        android.util.Log.e(TAG, "onCreate mVideoPath="+mVideoPath );
//        outputFile = new File(mVideoPath,"1.h264");
//        try {
//            outputStream = new FileOutputStream(outputFile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }


    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @Override
    public void run() {
        try {
            try {
                prepareEncoder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,//VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,//VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mSurface, null, null);


            Log.e(TAG, "created virtual display: " + mVirtualDisplay);



            recordVirtualDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    public void resetEncoder()
    {
        Rect r= new Rect(100,100,800,600);
        //r.set(0,0,100,100);
        try{

            Canvas c = mSurface.lockCanvas(null);
            mSurface.unlockCanvasAndPost(c);

           // mVirtualDisplay.resize(mWidth-1, mHeight-1, mDpi);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void prepareEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);

        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);

        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);


        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1);

        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, FRAME_RATE);

        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.e(TAG, "created video format: 00 " + format);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        Log.e(TAG, "created video format: 12 ");
        Log.e(TAG, "created input surface: " + mSurface2);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.e(TAG, "created video format: 34 ");

       mSurface = mEncoder.createInputSurface();

         mEncoder.start();
        Log.e(TAG, "created video format: 56 ");
    }

    private void recordVirtualDisplay() {

        long timeStamp=0;
        long presentationTimeUsLast=0;
        ByteBuffer realData_last = ByteBuffer.allocate(512*1024);
        ByteBuffer realData_lastlast = ByteBuffer.allocate(512*1024);
        Log.e(TAG, "recordVirtualDisplay: realData_last.remaining()="+realData_last.remaining());
        while (!mQuit.get()) {

            int eobIndex = mEncoder.dequeueOutputBuffer(mBufferInfo,0/*TIMEOUT_US*/);

            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    LogTools.d("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                   // Log.e(TAG, "recordVirtualDisplay: eobInex=" +eobIndex );
                    if(System.currentTimeMillis() - timeStamp >= 10 && timeStamp != 0)
                    {
                       // Log.e(TAG, "recordVirtualDisplay: >100 eobInex=" +eobIndex );
//                        Bundle params = new Bundle();
//                        params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//                        mEncoder.setParameters(params);

//lxklxk                        if(realData_lastlast.remaining() != 0)
//                        {
//                            sendRealData((presentationTimeUsLast / 1000+(System.currentTimeMillis() - timeStamp)) - startTime, realData_lastlast);
//
//                        }
                    }
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                            mEncoder.getOutputFormat().toString());
                    sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());

                    break;
                case MediaCodec.BUFFER_FLAG_CODEC_CONFIG:
                    LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                            mEncoder.getOutputFormat().toString());
                    sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());
                    break;
                default:
                    LogTools.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                    if (startTime == 0) {
                        startTime = mBufferInfo.presentationTimeUs / 1000;
                        timeStamp = System.currentTimeMillis();
                    }

                    /**
                     * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                     * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                     */
                    //lxk for test
                   // sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());

                   // Log.e(TAG, "recordVirtualDisplay: in default loop" );
                    if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                        ByteBuffer realData = mEncoder.getOutputBuffer(eobIndex);//mEncoder.getOutputBuffers()[eobIndex];
                        Log.e(TAG, "recordVirtualDisplay: ==="+realData.remaining());
                        byte[] finalBuff = new byte[10];
                        realData.get(finalBuff, mBufferInfo.offset, 10);

                        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;

                        int referenceBit = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x60;
                        referenceBit = referenceBit >> 5;
                        Log.e(TAG, "IDR: "+ Integer.toHexString(frameType) );
                        Log.e(TAG, "referenceBit: "+ referenceBit );

                       // if(frameType ==  NALU_TYPE_IDR || referenceBit >=2 )
                        if(frameType != 9)
                        {
                            if(frameType != 0)
                            {
                                try
                                {
                                    realData_last.put(realData);
                                }catch (BufferOverflowException e)
                                {
                                    realData_last.clear();
                                    e.printStackTrace();
                                }

                                presentationTimeUsLast = mBufferInfo.presentationTimeUs;
                                timeStamp = System.currentTimeMillis();
                            }

                        }
                        else
                        {
                            realData_lastlast.clear();
                            realData_lastlast.put(realData_last);
                            realData_last.clear();
                        }
                        realData.position(mBufferInfo.offset + 4);
                        realData.limit(mBufferInfo.offset + mBufferInfo.size);
                        sendRealData((mBufferInfo.presentationTimeUs / 1000) - startTime, realData);
                    }
                    else
                    {
                        Log.e(TAG, "recordVirtualDisplay: mBufferInfo.flags="+mBufferInfo.flags );
                    }
                    mEncoder.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
    }


    private void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }


    public final boolean getStatus() {
        return !mQuit.get();
    }


    private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                 true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);

        Log.e(TAG, "sendAVCDecoderConfigurationRecord: "+ bytesToHexString(finalBuff,20) );

        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
        mDataCollecter.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
    }

    /**
     * byte[]数组转换为16进制的字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的结果
     */
    private static String bytesToHexString(byte[] bytes,int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void sendRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH +
                realDataLength;

        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                        Packager.FLVPackager.NALU_HEADER_LENGTH,
                realDataLength);
        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;

        int pos = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH + Packager.FLVPackager.NALU_HEADER_LENGTH;


        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                 false,
                frameType == 5,
                realDataLength);
        //Log.e(TAG, "sendRealData: "+ bytesToHexString(finalBuff,20) );


        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = frameType;
        mDataCollecter.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
    }
}
