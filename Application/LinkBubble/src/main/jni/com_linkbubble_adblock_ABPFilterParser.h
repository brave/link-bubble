/* This file was originally machine generated, but is now manually maintained. */
#include <jni.h>
/* Header for class com_linkbubble_adblock_ABPFilterParser */

#ifndef _Included_com_linkbubble_adblock_ABPFilterParser
#define _Included_com_linkbubble_adblock_ABPFilterParser
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_linkbubble_adblock_ABPFilterParser
 * Method:    init
 * Signature: ()Ljava/lang/void;
 */
JNIEXPORT void JNICALL Java_com_linkbubble_adblock_ABPFilterParser_init
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     com_linkbubble_adblock_ABPFilterParser
 * Method:    stringFromJNI
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jboolean JNICALL Java_com_linkbubble_adblock_ABPFilterParser_shouldBlock
        (JNIEnv *, jobject, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif
