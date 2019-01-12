#ifndef __JB_JNI_H__
#define __JB_JNI_H__

#ifdef __cplusplus
extern "C" { 
#endif

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_getRandom
Input		:
Output		:
Return		:	���ظ�Java���ȡ�����������6�ֽ�����
Describe		:	��ȡ���������ҪJava�㴫�������Сlen��lenֵ����С��6
-------------------------------------------------------------------------------*/
jbyteArray Java_com_lxl_nanosic_app_ble_Encryption_getRandom(JNIEnv * env,jobject thiz,jint len);

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_devInfoVerify
Input		:
Output		:
Return		:	����һ������ֵ��TRUE��ʾ��֤ͨ����FALSE��ʾ��֤��ͨ����
Describe		:	��֤�Ƿ������ǵ�ң�ء�reportID����Ҫ������
-------------------------------------------------------------------------------*/
jboolean Java_com_lxl_nanosic_app_ble_Encryption_devInfoVerify(JNIEnv * env,jobject thiz,jbyteArray version_packet);

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_encryptData
Input		:
Output		:
Return		:	���ؼ��ܺ�����飬��һ���ֽ�Ϊ�����
Describe		:	�����·������ݰ������ݣ����������16�ֽڵ��ļ�ͷ����һ���ֽ�Ϊ0���ڶ��ֽ�Ϊ����ţ���������ݻ�����Ҫ�ش���64�ֽ��ļ�ͷ����һ�ֽ�Ϊ1���ڶ��ֽ�Ϊ����ţ������ֽ�Ϊ���ݣ�
-------------------------------------------------------------------------------*/
jbyteArray Java_com_lxl_nanosic_app_ble_Encryption_encryptData(JNIEnv * env,jobject thiz,jbyteArray data);


#ifdef __cplusplus
}
#endif
#endif

