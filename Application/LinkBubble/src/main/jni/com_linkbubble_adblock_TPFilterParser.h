/* This file was originally machine generated, but is now manually maintained. */
#include <jni.h>
/* Header for class com_linkbubble_adblock_TPFilterParser */

#ifndef _Included_com_linkbubble_adblock_TPFilterParser
#define _Included_com_linkbubble_adblock_TPFilterParser
#ifdef __cplusplus
extern "C" {
#endif


/*
 * Class:     com_linkbubble_adblock_TPFilterParser
 * Method:    init
 * Signature: ()Ljava/lang/void;
 */
JNIEXPORT void JNICALL Java_com_linkbubble_adblock_TPFilterParser_init
    (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     com_linkbubble_adblock_TPFilterParser
 * Method:    init
 * Signature: ()Ljava/lang/void;
 */
JNIEXPORT jboolean JNICALL Java_com_linkbubble_adblock_TPFilterParser_matchesTracker
    (JNIEnv *, jobject, jstring);

/*
 * Class:     com_linkbubble_adblock_TPFilterParser
 * Method:    init
 * Signature: ()Ljava/lang/void;
 */
JNIEXPORT jstring JNICALL Java_com_linkbubble_adblock_TPFilterParser_findFirstPartyHosts
        (JNIEnv *, jobject, jstring);


#ifdef __cplusplus
}
#endif
#endif