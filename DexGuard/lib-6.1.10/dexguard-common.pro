# Common DexGuard configuration for debug versions and release versions.
# Copyright (c) 2012-2015 GuardSquare NV

-ignorewarnings
-dontwarn sun.**
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn org.apache.**
-android

-zipalign 4
-dontcompress resources.arsc,**.jpg,**.jpeg,**.png,**.gif,**.wav,**.mp2,**.mp3,**.ogg,**.aac,**.mpg,**.mpeg,**.mid,**.midi,**.smf,**.jet,**.rtttl,**.imy,**.xmf,**.mp4,**.m4a,**.m4v,**.3gp,**.3gpp,**.3g2,**.3gpp2,**.amr,**.awb,**.wma,**.wmv,**.webm,**.zip,**.jar
-dontcompress RESOURCES.ARSC,**.JPG,**.JPEG,**.PNG,**.GIF,**.WAV,**.MP2,**.MP3,**.OGG,**.AAC,**.MPG,**.MPEG,**.MID,**.MIDI,**.SMF,**.JET,**.RTTTL,**.IMY,**.XMF,**.MP4,**.M4A,**.M4V,**.3GP,**.3GPP,**.3G2,**.3GPP2,**.AMR,**.AWB,**.WMA,**.WMV,**.WEBM,**.ZIP,**.JAR

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

# Renderscript support library.
-dontwarn android.os.SystemProperties
-dontwarn android.renderscript.RenderScript

# Ignore references to removed R classes.
-dontwarn android.support.**.R
-dontwarn android.support.**.R$*

# Ignore dynamic references and descriptor classes in support classes.
-dontnote android.support.**

# Ignore references from compatibility support classes to missing classes.
-dontwarn
    android.support.**Compat*,
    android.support.**Honeycomb*,
    android.support.**Jellybean*,
    android.support.**JB*,
    android.support.**Kitkat*,
    android.support.**19,
    android.support.**21,
    android.support.v7.internal.**,
    android.support.v7.widget.Toolbar,
    android.app.Notification$Builder

# Avoid merging and inlining compatibility classes.
-keep,allowshrinking,allowobfuscation class
    android.support.**Compat*,
    android.support.**Honeycomb*,
    android.support.**Jellybean*,
    android.support.**JB*,
    android.support.**Kitkat*,
    android.support.**19,
    android.support.**21
        { *; }

# Fields accessed before initialization.
-keepclassmembers,allowshrinking,allowobfuscation class android.support.v7.widget.GridLayout {
    ** *;
}

# Google Play Services.
-dontwarn com.google.android.gms.**
-dontnote com.google.android.gms.**
-keep class com.google.android.gms.location.ActivityRecognitionResult

-keepclassmembers class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final java.lang.String NULL;
}

-keep,allowobfuscation @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Google Play market License Verification Library.
-dontnote com.android.vending.licensing.ILicensingService
-keep,allowobfuscation public class com.android.vending.licensing.ILicensingService

# Google Play market expansion downloader.
-keepclassmembers public class com.google.android.vending.expansion.downloader.impl.DownloadsDB$* {
    public static final java.lang.String[][] SCHEMA;
    public static final java.lang.String     TABLE_NAME;
}

# Google Cloud Messaging.
-keep,allowshrinking class **.GCMIntentService

# AdMob (based on Google Play Services).
-keepnames class com.google.android.gms.ads.AdActivity

# AdMob (old library).
-dontnote com.google.ads.mediation.MediationServerParameters$Parameter
-keepclassmembers,allowobfuscation class * {
    @com.google.ads.mediation.MediationServerParameters$Parameter java.lang.String *;
}

-dontnote com.google.ads.mediation.MediationAdapter
-keep !abstract !interface * implements com.google.ads.mediation.MediationAdapter

-dontnote com.google.ads.mediation.customevent.CustomEvent
-keep !abstract !interface * implements com.google.ads.mediation.customevent.CustomEvent

# Amazon Ads.
-dontnote com.amazon.device.ads.AdActivity
-keepnames class com.amazon.device.ads.AdActivity

# Guava.
-dontnote sun.misc.Unsafe
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

-dontnote com.google.inject.internal.util.$FinalizableReference
-keepnames class com.google.inject.internal.util.$FinalizableReference

-keepclassmembers class * {
    void finalizeReferent();
}

# RoboGuice
-dontwarn roboguice.activity.RoboMapActivity
-dontwarn roboguice.test.**

-dontnote android.support.v4.app.Fragment
-keepclassmembers class android.support.v4.app.Fragment {
    public android.view.View getView();
}

-dontnote android.support.v4.app.FragmentManager
-keepclassmembers class android.support.v4.app.FragmentManager {
    public android.support.v4.app.Fragment findFragmentById(int);
    public android.support.v4.app.Fragment findFragmentByTag(java.lang.String);
}

-dontnote com.google.inject.Module
-keepclassmembers class * implements com.google.inject.Module {
    <init>(android.content.Context);
    <init>();
}

# Dagger.
-keep class **$$ModuleAdapter
-keep class **$$InjectAdapter
-keep class **$$StaticInjection

-dontnote dagger.Lazy
-keepnames class dagger.Lazy

# Butter Knife.
-dontnote butterknife.InjectView
-keep,allowobfuscation @interface butterknife.InjectView
-keep,allowobfuscation @interface butterknife.On*

-keep class **$$ViewInjector {
    public static void inject(...);
    public static void reset(...);
}

-keepclasseswithmembernames class * {
    @butterknife.InjectView <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.InjectView <methods>;
}

-keepclasseswithmembers class * {
    @butterknife.On* <methods>;
}

# Retrofit.
-dontnote retrofit.http.RestMethod
-keep,allowobfuscation @retrofit.http.RestMethod @interface *

-keepclassmembers @retrofit.http.RestMethod @interface * {
    <methods>;
}

-keepclassmembers,allowobfuscation interface * {
    @retrofit.http.** <methods>;
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

# Facebook Conceal.
-dontnote com.facebook.proguard.annotations.DoNotStrip
-keep,allowobfuscation @interface com.facebook.proguard.annotations.DoNotStrip

-keep @com.facebook.proguard.annotations.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.proguard.annotations.DoNotStrip *;
}

# Paypal card.io.
-dontnote io.card.**
-keep class io.card.payment.CardScanner   { *; }
-keep class io.card.payment.DetectionInfo { *; }
-keep class io.card.payment.CreditCard    { *; }

# OrmLite.
-dontnote com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
-keepclassmembers class * extends com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper {
    <init>(android.content.Context);
}

-dontnote com.j256.ormlite.field.DatabaseFieldConfig
-keepclassmembers class com.j256.ormlite.field.DatabaseFieldConfig {
    <fields>;
}

-dontnote com.j256.ormlite.dao.Dao
-keepclassmembers class * implements com.j256.ormlite.dao.Dao {
    <init>(**);
    <init>(**, java.lang.Class);
}

-dontnote com.j256.ormlite.android.AndroidLog
-keep class com.j256.ormlite.android.AndroidLog {
    <init>(java.lang.String);
}

-dontnote com.j256.ormlite.table.DatabaseTable
-keep @com.j256.ormlite.table.DatabaseTable class * {
    void set*(***);
    *** get*();
}

-dontnote com.j256.ormlite.field.DatabaseField
-keepclassmembers @interface com.j256.ormlite.field.DatabaseField {
    <methods>;
}

-dontnote com.j256.ormlite.field.ForeignCollectionField
-keepclassmembers @interface com.j256.ormlite.field.ForeignCollectionField {
    <methods>;
}

-keepclasseswithmembers class * {
    @com.j256.ormlite.field.DatabaseField <fields>;
}

-keepclasseswithmembers class * {
    @com.j256.ormlite.field.ForeignCollectionField <fields>;
}

-dontnote org.joda.time.DateTime
-keep,allowobfuscation class org.joda.time.DateTime
-keepclassmembers class org.joda.time.DateTime {
    <init>(long);
    long getMillis();
}

# Couchbase Lite.
-adaptresourcefilecontents META-INF/services/com.couchbase.lite.*

-dontnote com.couchbase.**
-keep class com.couchbase.lite.storage.SQLiteStorageEngine
-keep,allowobfuscation class * implements com.couchbase.lite.storage.SQLiteStorageEngine

-keep,allowobfuscation class * implements com.couchbase.lite.util.Logger

-keep class com.couchbase.touchdb.TDCollateJSON {
    int compareStringsUnicode(java.lang.String, java.lang.String);
}

-keep class com.couchbase.lite.router.Router {
    com.couchbase.lite.Status *(com.couchbase.lite.Database, java.lang.String, java.lang.String);
}

# Ektorp.
-keep class org.ektorp.** implements java.io.Serializable {
    <fields>;
    <init>(...);
    void set*(***);
    *** get*();
    boolean is*();
}

-dontnote org.ektorp.support.CouchDbDocument
-keep class * extends org.ektorp.support.CouchDbDocument {
    <fields>;
    <init>(...);
    void set*(***);
    *** get*();
    boolean is*();
}

# Google API.
-dontnote com.google.api.client.util.Key
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# GSON.
-keep,allowobfuscation class com.google.gson.annotations.*

-dontnote com.google.gson.annotations.Expose
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

-dontnote com.google.gson.annotations.SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Jackson.
-dontwarn org.codehaus.jackson.map.ext.**
-dontnote org.codehaus.jackson.**

-keep,allowobfuscation @interface org.codehaus.jackson.annotate.*
-keep,allowobfuscation @interface org.codehaus.jackson.map.annotate.*

-dontnote org.codehaus.jackson.annotate.JsonAutoDetect
-keepclassmembers @org.codehaus.jackson.annotate.JsonAutoDetect class * {
    void set*(***);
    *** get*();
    boolean is*();
}

-keepclassmembers class * {
    @org.codehaus.jackson.annotate.* <methods>;
}

# Apache logging.
-adaptclassstrings org.apache.commons.logging.LogFactory

# LogBack logging.
-dontnote ch.qos.logback.**
-keep,allowobfuscation class * implements ch.qos.logback.classic.spi.LoggerContextListener
-keep,allowobfuscation class * implements ch.qos.logback.classic.net.ReceiverBase
-keep,allowobfuscation class * implements ch.qos.logback.core.status.StatusListener
-keep,allowobfuscation class * implements ch.qos.logback.core.pattern.DynamicConverter
-keep,allowobfuscation class * implements ch.qos.logback.core.pattern.CompositeConverter
-keep,allowobfuscation class * implements ch.qos.logback.core.joran.action.Action
-keep,allowobfuscation class * implements ch.qos.logback.core.Appender
-keep,allowobfuscation class * implements ch.qos.logback.core.boolex.EventEvaluator
-keep,allowobfuscation class * implements ch.qos.logback.core.status.StatusListener
-keep,allowobfuscation class * implements ch.qos.logback.core.spi.PropertyDefiner

-keepclassmembers class ch.qos.logback.** {
    public static ** valueOf(java.lang.String);
}

-keep,allowobfuscation,includedescriptorclasses class * implements ch.qos.logback.classic.selector.ContextSelector {
    public <init>(ch.qos.logback.classic.LoggerContext);
}

-adaptresourcefilecontents assets/logback*.xml

# Facebook API.
-dontnote com.facebook.model.GraphObject
-keepclassmembers interface com.facebook.model.GraphObject {
    <methods>;
}

# Tapjoy.
-dontnote com.tapjoy.**
-keep class com.tapjoy.TJCOffersWebView
-keep class com.tapjoy.TapjoyFullScreenAdWebView
-keep class com.tapjoy.TapjoyVideoView
-keep class com.tapjoy.TJAdUnitView
-keep class com.tapjoy.mraid.view.ActionHandler
-keep class com.tapjoy.mraid.view.Browser

-dontnote org.json.JSONObject
-keep class com.tapjoy.TJAdUnitJSBridge {
  public void *(org.json.JSONObject, java.lang.String);
  public void closeRequested();
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

# Java mail.
-dontnote javax.mail.*
-keep,allowobfuscation class javax.mail.Session
-keep,allowobfuscation class javax.mail.URLName
-keep,allowobfuscation public class * extends javax.mail.Store {
    public <init>(javax.mail.Session, javax.mail.URLName);
}

-adaptresourcefilecontents javamail.default.providers, javamail.*.provider

# Cordova.
-dontnote org.apache.cordova.NativeToJsMessageQueue$PrivateApiBridgeMode
-dontnote com.squareup.okhttp.internal.Platform

-dontnote org.apache.cordova.App
-keep class org.apache.cordova.App

-dontnote org.apache.cordova.api.CordovaPlugin
-keep public class * extends org.apache.cordova.api.CordovaPlugin

-dontnote org.apache.cordova.CordovaPlugin
-keep public class * extends org.apache.cordova.CordovaPlugin

-dontnote com.phonegap.api.Plugin
-keep public class * extends com.phonegap.api.Plugin

# libGDX.
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*

-dontnote com.badlogic.gdx.Application
-dontnote com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
    <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}

-dontnote com.badlogic.gdx.physics.box2d.World
-keepclassmembers class com.badlogic.gdx.physics.box2d.World {
    boolean contactFilter(long, long);
    void    beginContact(long);
    void    endContact(long);
    void    preSolve(long, long);
    void    postSolve(long, long);
    boolean reportFixture(long);
    float   reportRayFixture(long, float, float, float, float, float);
}

# Nuance Vocalizer.
-dontnote com.nuance.android.vocalizer.**
-keep class com.nuance.android.vocalizer.VocalizerEngine {
  <fields>;
  *** speechMarksReceived(...);
  *** audioSamplesReceived(...);
  *** openAssetFile(...);
  *** closeAssetFile(...);
  *** stop(...);
}

-keep class com.nuance.android.vocalizer.VocalizerSpeechMark { <fields>; }
-keep class com.nuance.android.vocalizer.VocalizerVersion    { <fields>; }
-keep class com.nuance.android.vocalizer.VocalizerVoice      { <fields>; }
-keep class com.nuance.android.vocalizer.internal.VocalizerFileInfo     { <fields>; }
-keep class com.nuance.android.vocalizer.internal.VocalizerResourceInfo { <fields>; }
-keep class com.nuance.android.vocalizer.internal.VocalizerStatusInfo   { <fields>; }

# Enumerations.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods.
-keepclasseswithmembernames,includedescriptorclasses class * {
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
