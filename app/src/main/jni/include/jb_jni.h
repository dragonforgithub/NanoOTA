#ifndef __JB_JNI_H__
#define __JB_JNI_H__

#ifdef __cplusplus
extern "C" { 
#endif

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_getRandom
Input		:
Output		:
Return		:	返回给Java层获取到的随机数的6字节数组
Describe		:	获取随机数，需要Java层传个数组大小len，len值不能小于6
-------------------------------------------------------------------------------*/
jbyteArray Java_com_lxl_nanosic_app_ble_Encryption_getRandom(JNIEnv * env,jobject thiz,jint len);

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_devInfoVerify
Input		:
Output		:
Return		:	返回一个布尔值，TRUE表示验证通过，FALSE表示验证不通过。
Describe		:	验证是否是我们的遥控。reportID不需要传进来
-------------------------------------------------------------------------------*/
jboolean Java_com_lxl_nanosic_app_ble_Encryption_devInfoVerify(JNIEnv * env,jobject thiz,jbyteArray version_packet);

/*-----------------------------------------------------------------------------
Function Name:	Java_com_lxl_nanosic_app_ble_encryptData
Input		:
Output		:
Return		:	返回加密后的数组，第一个字节为包序号
Describe		:	传入下发的数据包的数据，另外如果是16字节的文件头，第一个字节为0，第二字节为包序号，如果是数据或者需要重传的64字节文件头，第一字节为1，第二字节为包序号，后续字节为数据，
-------------------------------------------------------------------------------*/
jbyteArray Java_com_lxl_nanosic_app_ble_Encryption_encryptData(JNIEnv * env,jobject thiz,jbyteArray data);


#ifdef __cplusplus
}
#endif
#endif

