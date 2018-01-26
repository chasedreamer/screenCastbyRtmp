LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

##MY_CPP_LIST := $(wildcard $(LOCAL_PATH)/librtmp/*.c)
MY_CPP_LIST := jni/rtmp/$(wildcard librtmp/*.c)
MY_CPP_LIST = librtmp/amf.c
MY_CPP_LIST += librtmp/hashswf.c
MY_CPP_LIST += librtmp/parseurl.c
MY_CPP_LIST += librtmp/rtmp.c
MY_CPP_LIST += librtmp/log.c

MY_CPP_LIST += screenrecorderrtmp.c

LOCAL_SRC_FILES := $(MY_CPP_LIST)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/librtmp
LOCAL_C_INCLUDES := ./librtmp/
LOCAL_C_INCLUDES += jni/rtmp/librtmp/
LOCAL_C_INCLUDES += screenrecorderrtmp.h
LOCAL_C_INCLUDES += log.h

LOCAL_CFLAGS += -DNO_CRYPTO

LOCAL_MODULE := libscreenrecorderrtmp


LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)