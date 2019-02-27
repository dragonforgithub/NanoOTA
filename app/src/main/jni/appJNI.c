/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>

#include <android/log.h>
#include <jb_nanoHAL.h>


#define TAG "NanoAppJni"
#define OS_PRINTF(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define OS_ERROR(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__)
#define true  (1)
#define false (0)
/*
 Java
 boolean jboolean
 byte	 jbyte
 char	 jchar
 short	 jshort
 int    jint
 long	 jlong
 float	 jfloat
 double     jdouble
 Object     jobject
 Class	 jclass
 String     jstring
 Object[]	 jobjectArray
 boolean[]	 jbooleanArray
 byte[]  jbyteArray
 char[]  jcharArray
 short[] jshortArray
 int[]	 jintArray
 long[]  jlongArray
 float[] jfloatArray
 double[]	 jdoubleArray
 */
static JavaVM * gJavaVM=NULL;
static jobject  gJavaObj;
static JNIEnv * gJavaEnv=NULL;
static char init = 0;
static volatile int gIsThreadStart = false;


static int OnWriteCommand_CallBack(char* pBuf,int size)
{
	JNIEnv *env;
	jclass jniCallbackClass;
	jmethodID WriteCmdCb=NULL;
	int    status;
	int    ret;

	status=(*gJavaVM)->GetEnv(gJavaVM, (void **)&env, 0);
	if(status != JNI_OK){
        //AttachCurrentThread��������һ�����ڵ�ǰ�����ӵ�JVM���̵߳�JNIEnvָ��
		if((*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL) != JNI_OK)
		{
			OS_ERROR("%s: AttachCurrentThread() failed\n", __FUNCTION__);
			return -1;
		}
	}

	jniCallbackClass = (*env)->GetObjectClass(env,gJavaObj);
	if(jniCallbackClass == NULL){
		OS_ERROR("FindClass() Error.....\n");
		return -1;
	}

	WriteCmdCb = (*env)->GetMethodID(env, jniCallbackClass, "NanoWriteCommand","([BI)I");
	if(WriteCmdCb == NULL){
		OS_ERROR("Find OnDataReceived() Error.....\n");
		return -1;
	}

	jbyteArray data = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, data, 0, size, (jbyte*)pBuf);
	ret = (*env)->CallIntMethod(env,gJavaObj,WriteCmdCb,data,size);
	(*env)->DeleteLocalRef(env,data);

	if(status != JNI_OK){
        /*����DetachCurrentThread�������JVM������*/
		if((*gJavaVM)->DetachCurrentThread(gJavaVM) != JNI_OK)
		{
			OS_ERROR("%s: DetachCurrentThread() failed\n", __FUNCTION__);
		}
	}

	return ret;		
}

static int OnWriteRequest_CallBack(char* pBuf,int size)
{
	JNIEnv *env;
	jclass jniCallbackClass;
 	jmethodID WriteReqCb=NULL;
	int    status;
	int    ret =-1;
	
	status=(*gJavaVM)->GetEnv(gJavaVM, (void**)&env, 0);
	if(status != JNI_OK){
		//AttachCurrentThread��������һ�����ڵ�ǰ�����ӵ�JVM���̵߳�JNIEnvָ��
		if((*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL) != JNI_OK)
		{
			OS_ERROR("%s: AttachCurrentThread() failed\n", __FUNCTION__);
			return -1;
		}
	}

	jniCallbackClass = (*env)->GetObjectClass(env,gJavaObj);
	if(jniCallbackClass == NULL){
		OS_ERROR("FindClass() Error.....\n");
		return -1;
	}
	
	WriteReqCb = (*env)->GetMethodID(env, jniCallbackClass, "NanoWriteRequest","([BI)I");
	if(WriteReqCb == NULL){
		OS_ERROR("Find OnDataReceived() Error.....\n");
		return -1;
	}


	jbyteArray data = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, data, 0, size, (jbyte *)pBuf);
	ret = (*env)->CallIntMethod(env,gJavaObj,WriteReqCb,data,size);
	(*env)->DeleteLocalRef(env,data);

	if(status != JNI_OK){
        /*����DetachCurrentThread�������JVM������*/
		if((*gJavaVM)->DetachCurrentThread(gJavaVM) != JNI_OK)
		{
			OS_ERROR("%s: DetachCurrentThread() failed\n", __FUNCTION__);
		}
	}

	return ret;
}

static void OnDataReceived_CallBack(char* pBuf,int size)
{
	JNIEnv *env;
	jclass jniCallbackClass;
 	jmethodID OnDataReceivedCb=NULL;
	int    status;
	
	status=(*gJavaVM)->GetEnv(gJavaVM, (void**)&env, 0);
	if(status != JNI_OK)
	{
        /*����AttachCurrentThread����ǰ�߳���JVM���й���*/
		if((*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL) != JNI_OK)
		{
			OS_ERROR("%s: AttachCurrentThread() failed\n", __FUNCTION__);
			return;
		}
	}

	jniCallbackClass = (*env)->GetObjectClass(env,gJavaObj);
	if(jniCallbackClass == NULL)
	{
		OS_ERROR("FindClass() Error.....\n");
		return;
	}
	
	OnDataReceivedCb = (*env)->GetMethodID(env, jniCallbackClass, "OnDataReceived","([BI)V");
	if(OnDataReceivedCb == NULL){
		OS_ERROR("Find OnDataReceived() Error.....\n");
		return;
	}

	jbyteArray data = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, data, 0, size, (jbyte *)pBuf);
	(*env)->CallVoidMethod(env,gJavaObj,OnDataReceivedCb,data,size);
	(*env)->DeleteLocalRef(env,data);

	if(status != JNI_OK)
	{
        /*����DetachCurrentThread�������JVM������*/
		if((*gJavaVM)->DetachCurrentThread(gJavaVM) != JNI_OK)
		{
			OS_ERROR("%s: DetachCurrentThread() failed\n", __FUNCTION__);
		}
	}
}

static void OnVoiceKeyAction_CallBack(int action)
{
	JNIEnv *env;
	jclass jniCallbackClass;
 	jmethodID OnVoiceKeyActionCb=NULL;
	int    status;
	
	status=(*gJavaVM)->GetEnv(gJavaVM, (void **)&env, 0);
	if(status != JNI_OK)
	{
	    /*����AttachCurrentThread����ǰ�߳���JVM���й���*/
		if((*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL) != JNI_OK)
		{
			OS_ERROR("%s: AttachCurrentThread() failed\n", __FUNCTION__);
			return;
		}
	}

	jniCallbackClass = (*env)->GetObjectClass(env,gJavaObj);
	if(jniCallbackClass == NULL)
	{
		OS_ERROR("FindClass() Error.....\n");
		return;
	}
	
	OnVoiceKeyActionCb = (*env)->GetMethodID(env, jniCallbackClass, "OnVoiceKeyAction","(I)V");
	if(OnVoiceKeyActionCb == NULL){
		OS_ERROR("Find OnVoiceKeyActionCb() Error.....\n");
		return;
	}

	(*env)->CallVoidMethod(env, gJavaObj, OnVoiceKeyActionCb, action);

	if(status != JNI_OK)
	{
	    /*����DetachCurrentThread�������JVM������*/
		if((*gJavaVM)->DetachCurrentThread(gJavaVM) != JNI_OK)
		{
			OS_ERROR("%s: DetachCurrentThread() failed\n", __FUNCTION__);
		}
	}
}

/*
 * Class:     com_lxl_login_demo_JniApiCall
 * Method:    NanoOpen
 * Signature: ()I
 */
JNIEXPORT jint
Java_com_lxl_login_demo_JniApiCall_NanoOpen( JNIEnv* env,jobject obj)
{
	jclass jniCallbackClass;

	if(!init)
	{
		init = 1;
		/* ͨ��JNIEnv���ǿ��Ի�ȡһ��Java��������� */
		(*env)->GetJavaVM(env,&gJavaVM);
		gJavaObj = (*env)->NewGlobalRef(env, obj);
		OS_PRINTF("JNI Version Build In Time %s-%s\n",__DATE__,__TIME__);
	    nano_open(NULL);
		/*gIsThreadStart = true;
		OS_TaskCreate(NULL,Nano_PthreadLoop,NULL);*/
		nano_RegisterWriteCmdCb((WriteCb)OnWriteCommand_CallBack);
		nano_RegisterWriteReqCb((WriteCb)OnWriteRequest_CallBack);
#if 1
		nano_RegisterDataReceivedCb(OnDataReceived_CallBack);
		nano_RegisterVoiceKeyCb(OnVoiceKeyAction_CallBack);
		OS_PRINTF("NanoOpen Success\n");
#endif
	}

	return 0;
}

/*
 * Class:     com_lxl_login_demo_JniApiCall
 * Method:    NanoLogin
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_lxl_login_demo_JniApiCall_NanoLogin
        (JNIEnv *env, jobject obj, jbyteArray appbuf, jint size)
{
    jboolean is_copy;

    unsigned char* buf = (unsigned char*)(*env)->GetByteArrayElements(env,appbuf, &is_copy);
    if(!buf){
        OS_ERROR("invalid buff\n");
        return 0;
    }

    if ((*env)->ExceptionCheck(env)) return -1;

    OS_PRINTF("nano_appLogin data size = %d\n",size);
    nano_appLogin(buf,size);

    if(buf)
        (*env)->ReleaseByteArrayElements(env,appbuf,buf,0);

    return 0;
}

/*
 * Class:     com_lxl_login_demo_JniApiCall
 * Method:    NanoRegister
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_lxl_login_demo_JniApiCall_NanoRegister
		(JNIEnv *env, jobject obj, jbyteArray appbuf, jint size)
{
	jboolean is_copy;

	unsigned char* buf = (unsigned char*)(*env)->GetByteArrayElements(env, appbuf, &is_copy);
	if(!buf){
		OS_ERROR("invalid buff\n");
		return 0;
	}

	if ((*env)->ExceptionCheck(env)) return -1;

	OS_PRINTF("nano_appProcData size = %d\n",size);
	nano_appRegister(buf,size);

	if(buf)
		(*env)->ReleaseByteArrayElements(env,appbuf,buf,0);

	return 0;
}

/*
 * Class:     com_lxl_login_demo_JniApiCall
 * Method:    NanoProcData
 * Signature: ([BI)V
 */
JNIEXPORT void
Java_com_lxl_login_demo_JniApiCall_NanoProcData(JNIEnv *env, jobject obj,jbyteArray appbuf,jint size)
{
    jboolean is_copy;
		
	unsigned char* buf = (unsigned char*)(*env)->GetByteArrayElements(env,appbuf, &is_copy);
	if(!buf){
		OS_ERROR("invalid buff\n");
		return;
	}
	
	if ((*env)->ExceptionCheck(env)) return;

    OS_PRINTF("nano_appProcData size = %d\n",size);
	nano_appProcData(buf,size);
	
	if(buf)
		(*env)->ReleaseByteArrayElements(env,appbuf,buf,0);
}

