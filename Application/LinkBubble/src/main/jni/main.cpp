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
    // Subset of easylist for top 500 sites, later we should expand this to include top 1M or 10M
    // But in general most adds even outside of top 500 will likely hit the same rules.
    parser.parse("||googleads.g.doubleclick.net^$popup,third-party\n"
        "||postrelease.com^$third-party\n"
        "/pubads_\n"
        "/securepubads.\n"
        "||googlesyndication.com/safeframe/$third-party\n"
        ".com/ads/$image,object,subdocument\n"
        "/adclick.\n"
        "||pagead2.googlesyndication.com^$~object-subrequest\n"
        ".ca/ads/\n"
        "||adlegend.com^$third-party\n"
        "||googlesyndication.com/pagead/$third-party\n"
        "-728x90_\n"
        ".net/ads/\n"
        "_300x250_\n"
        "||doubleverify.com^$third-party\n"
        "_300x250.\n"
        ".adserver.\n"
        "/td-ads-\n"
        "/pubads.\n"
        "||doubleclick.net^$third-party,domain=3news.co.nz|92q.com|abc-7.com|addictinggames.com|allbusiness.com|allthingsd.com|bizjournals.com|bloomberg.com|bnn.ca|boom92houston.com|boom945.com|boomphilly.com|break.com|cbc.ca|cbs19.tv|cbs3springfield.com|cbsatlanta.com|cbslocal.com|complex.com|dailymail.co.uk|darkhorizons.com|doubleviking.com|euronews.com|extratv.com|fandango.com|fox19.com|fox5vegas.com|gorillanation.com|hawaiinewsnow.com|hellobeautiful.com|hiphopnc.com|hot1041stl.com|hothiphopdetroit.com|hotspotatl.com|hulu.com|imdb.com|indiatimes.com|indyhiphop.com|ipowerrichmond.com|joblo.com|kcra.com|kctv5.com|ketv.com|koat.com|koco.com|kolotv.com|kpho.com|kptv.com|ksat.com|ksbw.com|ksfy.com|ksl.com|kypost.com|kysdc.com|live5news.com|livestation.com|livestream.com|metro.us|metronews.ca|miamiherald.com|my9nj.com|myboom1029.com|mycolumbusmagic.com|mycolumbuspower.com|myfoxdetroit.com|myfoxorlando.com|myfoxphilly.com|myfoxphoenix.com|myfoxtampabay.com|nbcrightnow.com|neatorama.com|necn.com|neopets.com|news.com.au|news4jax.com|newsone.com|nintendoeverything.com|oldschoolcincy.com|own3d.tv|pagesuite-professional.co.uk|pandora.com|player.theplatform.com|ps3news.com|radio.com|radionowindy.com|rottentomatoes.com|sbsun.com|shacknews.com|sk-gaming.com|ted.com|thebeatdfw.com|theboxhouston.com|theglobeandmail.com|timesnow.tv|tv2.no|twitch.tv|universalsports.com|ustream.tv|wapt.com|washingtonpost.com|wate.com|wbaltv.com|wcvb.com|wdrb.com|wdsu.com|wflx.com|wfmz.com|wfsb.com|wgal.com|whdh.com|wired.com|wisn.com|wiznation.com|wlky.com|wlns.com|wlwt.com|wmur.com|wnem.com|wowt.com|wral.com|wsj.com|wsmv.com|wsvn.com|wtae.com|wthr.com|wxii12.com|wyff4.com|yahoo.com|youtube.com|zhiphopcleveland.com\n"
        "/show_ads.js\n"
        "/show_ads_\n"
        "&ad_type=\n"
        "||amazon-adsystem.com^$third-party\n"
        "||revsci.net^$third-party\n"
        "||adtech.de^$third-party\n"
        "://ads.\n"
        "||adnxs.com^$third-party\n"
        "||criteo.com^$third-party\n"
        "||rfihub.net^$third-party\n"
        "||ad.doubleclick.net^$~object-subrequest,third-party\n"
        "||adadvisor.net^$third-party\n"
        "||2mdn.net^$~object-subrequest,third-party\n"
        "||casalemedia.com^$third-party\n"
        "728x90.html|\n"
        "||advertising.com^$third-party\n"
        ".ace.advertising.\n"
        "||moatads.com^$third-party\n"
        "||cdn.optmd.com^$popup,third-party\n"
        "||adtechus.com^$third-party\n"
        "||effectivemeasure.net^$third-party\n"
        "||fresh8.co^$third-party\n"
        "/bannerview.*?\n"
        "@@||eplayerhtml5.performgroup.com/js/tsEplayerHtml5/js/Eplayer/js/modules/bannerview/bannerview.main.js?\n"
        "||liverail.com^$~object-subrequest,third-party\n"
        "||pubmatic.com^$third-party\n"
        "_ad_change.\n"
        "/get_ad_\n"
        "?adunitid=\n"
        "/adbox.\n"
        ".com/ad/$domain=~channel4.com|~watchever.de\n"
        "/taboola-\n"
        "@@||trc.taboola.com*http%$script,third-party\n"
        // Not part of easylist.txt default list but good addition
        "||cdn.taboola.com\n");
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
