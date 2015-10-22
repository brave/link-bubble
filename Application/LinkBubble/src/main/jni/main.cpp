#include <string.h>
#include <jni.h>
#include "com_linkbubble_adblock_ABPFilterParser.h"
#include "ABPFilterParser.h"

ABPFilterParser parser;

/*
 * Class:     com_linkbubble_adblock_ABPFilterParser
 * Method:    init
 * Signature: ()Ljava/lang/void;
 */
JNIEXPORT void JNICALL Java_com_linkbubble_adblock_ABPFilterParser_init(JNIEnv *env, jobject obj) {
    parser.parse("||pagead2.googlesyndication.com^$~object-subrequest\n"
                 "||googlesyndication.com/pagead/$third-party\n"
                 "||googlesyndication.com/safeframe/$third-party\n"
                 "||googlesyndication.com/simgad/$third-party\n"
                 "||googlesyndication.com^*/click_to_buy/$object-subrequest,third-party\n"
                 "||googlesyndication.com^*/domainpark.cgi?\n"
                 "||adsafeprotected.com^$third-party\n"
                 ".com/ad/$domain=~channel4.com|~watchever.de\n"
                 ".com/ad2/\n"
                 ".com/ad6/\n"
                 ".com/ad?\n"
                 ".com/adclk?\n"
                 "||googlesyndication.com^*/googlevideoadslibraryas3.swf$object-subrequest,third-party");
}

/*
 * Class:     com_linkbubble_adblock_ABPFilterParser
 * Method:    stringFromJNI
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jboolean JNICALL Java_com_linkbubble_adblock_ABPFilterParser_shouldBlock(JNIEnv *env, jobject obj, jstring input) {
    const char *nativeInput = env->GetStringUTFChars(input, 0);

    bool shouldBlock = parser.matches(nativeInput);

    env->ReleaseStringUTFChars(input, nativeInput);

    return shouldBlock ? JNI_TRUE : JNI_FALSE;
    //env->NewStringUTF("Hello from JNI");
}
