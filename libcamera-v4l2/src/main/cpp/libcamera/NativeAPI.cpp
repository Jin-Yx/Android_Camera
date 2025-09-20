//
// Created by Hsj on 2021/4/26.
//

#include "Common.h"
#include "NativeAPI.h"
#include "CameraAPI.h"
#include <list>
#include <string>

#define TAG "NativeAPI"
#define OBJECT_ID "nativeObj"
#define CLASS_NAME "com/hsj/camera/CameraAPI"

typedef jlong CAMERA_ID;

static void setFieldLong(JNIEnv *env, jobject obj, const char *fieldName, jlong value) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID field = env->GetFieldID(clazz, fieldName, "J");
    if (LIKELY(field)) {
        env->SetLongField(obj, field, value);
    } else {
        LOGE(TAG, "setFieldLong: failed '%s' not found", fieldName);
    }
    env->DeleteLocalRef(clazz);
}

static CAMERA_ID nativeInit(JNIEnv *env, jobject thiz) {
    auto *camera = new CameraAPI();
    auto cameraId = reinterpret_cast<CAMERA_ID>(camera);
    setFieldLong(env, thiz, OBJECT_ID, cameraId);
    return cameraId;
}

static ActionInfo nativeCreate(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, int productId, jint vendorId) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_RELEASE;
    if (LIKELY(camera)) {
        status = camera->connect(productId, vendorId);
    }
    LOGD(TAG, "camera->open(): %d", status);
    return status;
}

static ActionInfo nativeCreate2(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, jstring jdeviceName) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_RELEASE;
    if (LIKELY(camera)) {
        const char *videoNode = env->GetStringUTFChars(jdeviceName, JNI_FALSE);
        LOGD(TAG, "open camera with video node is %s\n", videoNode);
        status = camera->connect(videoNode);
    }
    LOGD(TAG, "camera->open(): %d", status);
    return status;
}

static ActionInfo nativeAutoExposure(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, jboolean isAuto) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        status = camera->autoExposure(isAuto);
    }
    LOGD(TAG, "camera->autoExposure(): %d", status);
    return status;
}

static ActionInfo nativeSetExposure(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, int level) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        if (level > 0) {
            status = camera->updateExposure(level);
        } else {
            status = ACTION_ERROR_SET_EXPOSURE;
            LOGE(TAG, "camera->updateExposure() failed: level must more than 0");
        }
    }
    LOGD(TAG, "camera->updateExposure(): %d", status);
    return status;
}

static jobject nativePreviewSize(JNIEnv *env, jobject thiz, CAMERA_ID cameraId) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    if (LIKELY(camera)) {
        jclass cls_list = env->FindClass("java/util/ArrayList");
        jmethodID construct_list = env->GetMethodID(cls_list , "<init>","()V");
        jmethodID method_list_add = env->GetMethodID(cls_list, "add", "(Ljava/lang/Object;)Z");

        jclass cls_previewSize = env->FindClass("com/hsj/camera/bean/PreviewSize");
        jmethodID construct_previewSize = env->GetMethodID(cls_previewSize, "<init>","(IIILjava/util/ArrayList;)V");

        jclass cls_frameRate = env->FindClass("com/hsj/camera/bean/FrameRate");
        jmethodID construct_frameRate = env->GetMethodID(cls_frameRate, "<init>","(II)V");

        jobject obj_previewSize_list = env->NewObject(cls_list , construct_list);

        std::list<PreviewSize> previewSizes = camera->getPreviewSize();
        LOGI(TAG, "previewSize size: %d\n", previewSizes.size());
        std::list<PreviewSize>::iterator previewSize;
        for (previewSize = previewSizes.begin(); previewSize != previewSizes.end(); ++previewSize) {
            int pixelFormat;
            switch (previewSize->pixelFormat) {
                case V4L2_PIX_FMT_MJPEG:
                    pixelFormat = FRAME_FORMAT_MJPEG;
                    break;
                case V4L2_PIX_FMT_YUYV:
                    pixelFormat = FRAME_FORMAT_YUYV;
                    break;
//                case V4L2_PIX_FMT_H264:
//                    pixelFormat = FRAME_FORMAT_DEPTH;
//                    break;
                default:
                    pixelFormat = -1;
                    break;
            }

            jobject obj_frameRate_list = env->NewObject(cls_list , construct_list);

            std::list<FrameRate> frameRates = previewSize->frameRates;
            std::list<FrameRate>::iterator frameRate;
            for (frameRate = frameRates.begin(); frameRate != frameRates.end(); ++frameRate) {
                jobject obj_frameRate = env->NewObject(cls_frameRate, construct_frameRate, frameRate->numerator, frameRate->denominator);
                env->CallBooleanMethod(obj_frameRate_list, method_list_add, obj_frameRate);
            }

            jobject obj_previewSize = env->NewObject(cls_previewSize, construct_previewSize, pixelFormat, previewSize->width, previewSize->height, obj_frameRate_list);
            env->CallBooleanMethod(obj_previewSize_list, method_list_add, obj_previewSize);
        }
        return obj_previewSize_list;
    }
    return 0;
}

static ActionInfo nativeFrameSize(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, jint width, jint height, jint frameFormat, jint denominator) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        if (width > 0 && height > 0) {
            status = camera->setFrameSize(width, height, frameFormat, denominator);
        } else {
            status = ACTION_ERROR_SET_W_H;
            LOGE(TAG, "camera->setFrameSize() failed: width and height must more than 0");
        }
    }
    LOGD(TAG, "camera->setFrameSize(): %d", status);
    return status;
}

static ActionInfo nativeFrameCallback(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, jobject frame_callback) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        jobject _frame_callback = env->NewGlobalRef(frame_callback);
        status = camera->setFrameCallback(env, _frame_callback);
    }
    LOGD(TAG, "camera->setFrameCallback(): %d", status);
    return status;
}

static ActionInfo nativePreview(JNIEnv *env, jobject thiz, CAMERA_ID cameraId, jobject surface) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        status = camera->setPreview(surface ? ANativeWindow_fromSurface(env, surface) : NULL);
    }
    LOGD(TAG, "camera->setPreview(): %d", status);
    return status;
}

static ActionInfo nativeStart(JNIEnv *env, jobject thiz, CAMERA_ID cameraId) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        status = camera->start();
    }
    LOGD(TAG, "camera->start(): %d", status);
    return status;
}

static ActionInfo nativeStop(JNIEnv *env, jobject thiz, CAMERA_ID cameraId) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    ActionInfo status = ACTION_ERROR_DESTROY;
    if (LIKELY(camera)) {
        status = camera->stop();
    }
    LOGD(TAG, "camera->stop(): %d", status);
    return status;
}

static ActionInfo nativeDestroy(JNIEnv *env, jobject thiz, CAMERA_ID cameraId) {
    auto *camera = reinterpret_cast<CameraAPI *>(cameraId);
    setFieldLong(env, thiz, OBJECT_ID, 0);
    ActionInfo status = ACTION_ERROR_RELEASE;
    if (LIKELY(camera)) {
        status = camera->close();
        LOGD(TAG, "camera->close(): %d", status);
        status = camera->destroy();
        SAFE_DELETE(camera)
    }
    LOGD(TAG, "camera->destroy(): %d", status);
    return status;
}

static const JNINativeMethod METHODS[] = {
    {"nativeInit",          "()J",                                          (void *) nativeInit},
    {"nativeCreate",        "(JII)I",                                       (void *) nativeCreate},
    {"nativeCreate2",       "(JLjava/lang/String;)I",                       (void *) nativeCreate2},
    {"nativeAutoExposure",  "(JZ)I",                                        (void *) nativeAutoExposure},
    {"nativeSetExposure",   "(JI)I",                                        (void *) nativeSetExposure},
    {"nativeFrameCallback", "(JLcom/hsj/camera/callback/IFrameCallback;)I", (void *) nativeFrameCallback},
    {"nativePreviewSize",   "(J)Ljava/util/ArrayList;",                     (void *) nativePreviewSize},
    {"nativeFrameSize",     "(JIIII)I",                                      (void *) nativeFrameSize},
    {"nativePreview",       "(JLandroid/view/Surface;)I",                   (void *) nativePreview},
    {"nativeStart",         "(J)I",                                         (void *) nativeStart},
    {"nativeStop",          "(J)I",                                         (void *) nativeStop},
    {"nativeDestroy",       "(J)I",                                         (void *) nativeDestroy},
};

jint registerAPI(JNIEnv *env){
    jclass clazz = env->FindClass(CLASS_NAME);
    if (clazz == nullptr) return JNI_ERR;
    jint ret = env->RegisterNatives(clazz, METHODS, sizeof(METHODS) / sizeof(JNINativeMethod));
    return ret == JNI_OK ? JNI_VERSION_1_6 : ret;
}
