# Common DexGuard configuration for debug versions and release versions.
# Copyright (c) 2012-2014 Saikoa / Itsana BVBA

-ignorewarnings
-dontwarn sun.**
-dontwarn javax.**
-dontwarn org.apache.**
-android

-zipalign 4
-dontcompress resources.arsc,**.jpg,**.jpeg,**.png,**.gif,**.wav,**.mp2,**.mp3,**.ogg,**.aac,**.mpg,**.mpeg,**.mid,**.midi,**.smf,**.jet,**.rtttl,**.imy,**.xmf,**.mp4,**.m4a,**.m4v,**.3gp,**.3gpp,**.3g2,**.3gpp2,**.amr,**.awb,**.wma,**.wmv,**.webm
-dontcompress RESOURCES.ARSC,**.JPG,**.JPEG,**.PNG,**.GIF,**.WAV,**.MP2,**.MP3,**.OGG,**.AAC,**.MPG,**.MPEG,**.MID,**.MIDI,**.SMF,**.JET,**.RTTTL,**.IMY,**.XMF,**.MP4,**.M4A,**.M4V,**.3GP,**.3GPP,**.3G2,**.3GPP2,**.AMR,**.AWB,**.WMA,**.WMV,**.WEBM

-keepattributes *Annotation*,Signature,InnerClasses,SourceFile,LineNumberTable
-renamesourcefileattribute ''
-keepresourcexmlattributenames manifest/installLocation,manifest/versionCode,manifest/application/*/intent-filter/*/name

# com.example.android.apis.animation.ShapeHolder,...
-keepclassmembers class **Holder {
    public *** get*();
    public void set*(***);
}

# The name may be stored and then used after an update.
-dontnote android.app.backup.BackupAgent
-keep,allowshrinking public !abstract class * extends android.app.backup.BackupAgent

-keepclassmembers !abstract class !com.google.ads.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclassmembers !abstract class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.content.Context {
   public void *(android.view.View);
}

-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontnote android.webkit.JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Ignore an abstract class in a descriptor in a support class.
-dontnote android.support.v4.view.ActionProvider

# Ignore a dynamic reference from a support class to a runtime class.
-dontnote libcore.icu.ICU

# Ignore references to removed R classes.
-dontwarn android.support.v7.appcompat.R
-dontwarn android.support.v7.appcompat.R$*

# Avoid merging and inlining compatibility classes.
-keep,allowshrinking,allowobfuscation class android.support.**Compat* { *; }

# Play Services.
-dontnote com.google.android.gms.location.ActivityRecognitionResult
-keep class com.google.android.gms.location.ActivityRecognitionResult

-dontnote com.google.android.gms.common.internal.safeparcel.SafeParcelable
-keepclassmembers class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final java.lang.String NULL;
}

-dontnote com.google.android.gms.common.annotation.KeepName
-keep,allowobfuscation @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Play market License Verification Library.
-dontnote com.android.vending.licensing.ILicensingService
-keep,allowobfuscation public class com.android.vending.licensing.ILicensingService

# Play market expansion downloader.
-keepclassmembers public class com.google.android.vending.expansion.downloader.impl.DownloadsDB$* {
    public static final java.lang.String[][] SCHEMA;
    public static final java.lang.String     TABLE_NAME;
}

# AdMob.
-dontnote com.google.ads.mediation.MediationServerParameters$Parameter
-keepclassmembers,allowobfuscation class * {
    @com.google.ads.mediation.MediationServerParameters$Parameter java.lang.String *;
}

-dontnote com.google.ads.mediation.MediationAdapter
-keep !abstract !interface * implements com.google.ads.mediation.MediationAdapter

-dontnote com.google.ads.mediation.customevent.CustomEvent
-keep !abstract !interface * implements com.google.ads.mediation.customevent.CustomEvent

# Guava.
-dontnote com.google.common.primitives.UnsignedBytes$LexicographicalComparatorHolder$UnsafeComparator
-keepclassmembers class com.google.common.primitives.UnsignedBytes$LexicographicalComparatorHolder$UnsafeComparator {
    sun.misc.Unsafe theUnsafe;
}

# Injection in Guice/RoboGuice/Dagger/ActionBarSherlock.
-dontnote com.google.inject.Provider
-keep,allowobfuscation class * implements com.google.inject.Provider

-keep,allowobfuscation @interface javax.inject.**          { *; }
-keep,allowobfuscation @interface com.google.inject.**     { *; }
-keep,allowobfuscation @interface roboguice.**             { *; }
-keep,allowobfuscation @interface com.actionbarsherlock.** { *; }

-dontnote com.google.inject.Inject
-dontnote roboguice.event.Observes
-keepclassmembers,allowobfuscation class * {
    @javax.inject.**          <fields>;
    @com.google.inject.**     <fields>;
    @roboguice.**             <fields>;
    @roboguice.event.Observes <methods>;
    @com.actionbarsherlock.** <fields>;
    @dagger.**                *;
    !private <init>();
    @com.google.inject.Inject <init>(***);
}

-dontnote roboguice.activity.event.OnCreateEvent
-keepclass,allowobfuscation class roboguice.activity.event.OnCreateEvent

-dontnote roboguice.inject.SharedPreferencesProvider$PreferencesNameHolder
-keepclass,allowobfuscation class roboguice.inject.SharedPreferencesProvider$PreferencesNameHolder

-dontnote com.google.inject.internal.util.$Finalizer
-keepclassmembers class com.google.inject.internal.util.$Finalizer {
    public static java.lang.ref.ReferenceQueue startFinalizer(java.lang.Class,java.lang.Object);
}

-keepclassmembers class * {
    void finalizeReferent();
}

# Dagger.
-keep class **$$ModuleAdapter
-keep class **$$InjectAdapter
-keep class **$$StaticInjection

-dontnote dagger.Lazy
-keepnames class dagger.Lazy

# Butter Knife.
-dontnote butterknife.InjectView
-dontnote butterknife.OnClick
-keep,allowobfuscation @interface butterknife.InjectView
-keep,allowobfuscation @interface butterknife.OnClick

-keep class **$$ViewInjector {
    public static void inject(...);
    public static void reset(...);
}

-keepclasseswithmembers class * {
    @butterknife.InjectView <fields>;
}

-keepclasseswithmembers class * {
    @butterknife.InjectView <methods>;
}

-keepclasseswithmembers class * {
    @butterknife.OnClick <methods>;
}

# ActionBarSherlock.
-dontnote com.actionbarsherlock.internal.nineoldandroids.animation.*
-dontnote com.actionbarsherlock.ActionBarSherlock
-keepclassmembers !abstract class * extends com.actionbarsherlock.ActionBarSherlock {
    <init>(android.app.Activity, int);
}

-dontnote com.actionbarsherlock.view.ActionProvider
-keep !abstract class * extends com.actionbarsherlock.view.ActionProvider {
    <init>(android.content.Context);
}

# Apache logging.
-adaptclassstrings org.apache.commons.logging.LogFactory

# Facebook API.
-dontnote com.facebook.model.GraphObject
-keepclassmembers interface com.facebook.model.GraphObject {
    <methods>;
}

# SQLCipher.
-dontnote net.sqlcipher.**
-keep class net.sqlcipher.CursorWindow {
    int nWindow;
}
-keep class net.sqlcipher.database.SQLiteDatabase {
    int mNativeHandle;
}
-keep class net.sqlcipher.database.SQLiteProgram {
    int nHandle;
    int nStatement;
}
-keep class net.sqlcipher.database.SQLiteQuery
-keep class net.sqlcipher.database.SQLiteStatement
-keep class net.sqlcipher.database.* extends java.lang.Exception

-keepclassmembers class net.sqlcipher.database.SQLiteCompiledSql {
    int nHandle;
    int nStatement;
}
-keepclassmembers class net.sqlcipher.database.SQLiteDebug$PagerStats {
    int memoryUsed;
    int largestMemAlloc;
    int pageCacheOverflo;
}
-keepclassmembers class net.sqlcipher.** {
    native <methods>;
}

# Cordova.
-dontnote org.apache.cordova.NativeToJsMessageQueue$PrivateApiBridgeMode

-dontnote org.apache.cordova.api.CordovaPlugin
-keep public class * extends org.apache.cordova.api.CordovaPlugin

-dontnote com.phonegap.api.Plugin
-keep public class * extends com.phonegap.api.Plugin

# Enumerations.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Serializable classes.
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
