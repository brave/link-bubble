# DexGuard configuration.
# Copyright (c) 2012-2014 Saikoa / Itsana BVBA

-assumenosideeffects class java.**

-assumenoexternalsideeffects public class java.lang.Object {
    public java.lang.Object();
}

-assumenosideeffects public class java.lang.Object {
    public final java.lang.Class getClass();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
#    protected java.lang.Object clone();
#    public java.lang.String toString();
}

-assumenoexternalsideeffects public final class java.lang.Boolean {
    public java.lang.Boolean(boolean);
    public java.lang.Boolean(java.lang.String);
}

-assumenosideeffects public final class java.lang.Boolean {
    public static java.lang.Boolean valueOf(boolean);
    public static java.lang.Boolean valueOf(java.lang.String);
    public static java.lang.String toString(boolean);
    public static boolean getBoolean(java.lang.String);
    public static boolean parseBoolean(java.lang.String);
    public boolean booleanValue();
#    public java.lang.String toString();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Boolean);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Byte {
    public java.lang.Byte(byte);
    public java.lang.Byte(java.lang.String);
}

-assumenosideeffects public final class java.lang.Byte {
    public static java.lang.String toString(byte);
    public static java.lang.Byte valueOf(byte);
    public static byte parseByte(java.lang.String);
    public static byte parseByte(java.lang.String, int);
    public static java.lang.Byte valueOf(java.lang.String, int);
    public static java.lang.Byte valueOf(java.lang.String);
    public static java.lang.Byte decode(java.lang.String);
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public java.lang.String toString();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Byte);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Character {
    public java.lang.Character(char);
}

-assumenosideeffects public final class java.lang.Character {
    public static java.lang.String toString(char);
    public static boolean isValidCodePoint(int);
    public static boolean isSupplementaryCodePoint(int);
    public static boolean isHighSurrogate(char);
    public static boolean isLowSurrogate(char);
    public static boolean isSurrogatePair(char, char);
    public static int charCount(int);
    public static int toCodePoint(char, char);
#    public static int codePointAt(java.lang.CharSequence, int);
    public static int codePointAt(char[], int);
    public static int codePointAt(char[], int, int);
#    public static int codePointBefore(java.lang.CharSequence, int);
    public static int codePointBefore(char[], int);
    public static int codePointBefore(char[], int, int);
    public static int toChars(int, char[], int);
    public static char[] toChars(int);
#    public static int codePointCount(java.lang.CharSequence, int, int);
    public static int codePointCount(char[], int, int);
#    public static int offsetByCodePoints(java.lang.CharSequence, int, int);
    public static int offsetByCodePoints(char[], int, int, int, int);
    public static boolean isLowerCase(char);
    public static boolean isLowerCase(int);
    public static boolean isUpperCase(char);
    public static boolean isUpperCase(int);
    public static boolean isTitleCase(char);
    public static boolean isTitleCase(int);
    public static boolean isDigit(char);
    public static boolean isDigit(int);
    public static boolean isDefined(char);
    public static boolean isDefined(int);
    public static boolean isLetter(char);
    public static boolean isLetter(int);
    public static boolean isLetterOrDigit(char);
    public static boolean isLetterOrDigit(int);
    public static boolean isJavaLetter(char);
    public static boolean isJavaLetterOrDigit(char);
    public static boolean isJavaIdentifierStart(char);
    public static boolean isJavaIdentifierStart(int);
    public static boolean isJavaIdentifierPart(char);
    public static boolean isJavaIdentifierPart(int);
    public static boolean isUnicodeIdentifierStart(char);
    public static boolean isUnicodeIdentifierStart(int);
    public static boolean isUnicodeIdentifierPart(char);
    public static boolean isUnicodeIdentifierPart(int);
    public static boolean isIdentifierIgnorable(char);
    public static boolean isIdentifierIgnorable(int);
    public static char toLowerCase(char);
    public static int toLowerCase(int);
    public static char toUpperCase(char);
    public static int toUpperCase(int);
    public static char toTitleCase(char);
    public static int toTitleCase(int);
    public static int digit(char, int);
    public static int digit(int, int);
    public static int getNumericValue(char);
    public static int getNumericValue(int);
    public static boolean isSpace(char);
    public static boolean isSpaceChar(char);
    public static boolean isSpaceChar(int);
    public static boolean isWhitespace(char);
    public static boolean isWhitespace(int);
    public static boolean isISOControl(char);
    public static boolean isISOControl(int);
    public static int getType(char);
    public static int getType(int);
    public static char forDigit(int, int);
    public static byte getDirectionality(char);
    public static byte getDirectionality(int);
    public static boolean isMirrored(char);
    public static boolean isMirrored(int);
    public static java.lang.Character valueOf(char);
    public static char reverseBytes(char);
    public char charValue();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
#    public java.lang.String toString();
    public int compareTo(java.lang.Character);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Double {
    public java.lang.Double(double);
    public java.lang.Double(java.lang.String);
}

-assumenosideeffects public final class java.lang.Double {
    public static java.lang.String toString(double);
    public static java.lang.String toHexString(double);
    public static java.lang.Double valueOf(java.lang.String);
    public static java.lang.Double valueOf(double);
    public static double parseDouble(java.lang.String);
    public static boolean isNaN(double);
    public static boolean isInfinite(double);
    public static long doubleToLongBits(double);
    public static long doubleToRawLongBits(double);
    public static double longBitsToDouble(long);
    public static int compare(double, double);
    public boolean isNaN();
    public boolean isInfinite();
    public java.lang.String toString();
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Double);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Float {
    public java.lang.Float(float);
    public java.lang.Float(double);
    public java.lang.Float(java.lang.String);
}

-assumenosideeffects public final class java.lang.Float {
    public static java.lang.String toString(float);
    public static java.lang.String toHexString(float);
    public static java.lang.Float valueOf(java.lang.String);
    public static java.lang.Float valueOf(float);
    public static float parseFloat(java.lang.String);
    public static boolean isNaN(float);
    public static boolean isInfinite(float);
    public static int floatToIntBits(float);
    public static int floatToRawIntBits(float);
    public static float intBitsToFloat(int);
    public static int compare(float, float);
    public boolean isNaN();
    public boolean isInfinite();
#    public java.lang.String toString();
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Float);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Integer {
    public java.lang.Integer(int);
    public java.lang.Integer(java.lang.String);
}

-assumenosideeffects public final class java.lang.Integer {
    public static java.lang.String toString(int, int);
    public static java.lang.String toHexString(int);
    public static java.lang.String toOctalString(int);
    public static java.lang.String toBinaryString(int);
    public static java.lang.String toString(int);
    public static int parseInt(java.lang.String, int);
    public static int parseInt(java.lang.String);
    public static java.lang.Integer valueOf(java.lang.String, int);
    public static java.lang.Integer valueOf(java.lang.String);
    public static java.lang.Integer valueOf(int);
    public static java.lang.Integer getInteger(java.lang.String);
    public static java.lang.Integer getInteger(java.lang.String, int);
    public static java.lang.Integer getInteger(java.lang.String, java.lang.Integer);
    public static java.lang.Integer decode(java.lang.String);
    public static int highestOneBit(int);
    public static int lowestOneBit(int);
    public static int numberOfLeadingZeros(int);
    public static int numberOfTrailingZeros(int);
    public static int bitCount(int);
    public static int rotateLeft(int, int);
    public static int rotateRight(int, int);
    public static int reverse(int);
    public static int signum(int);
    public static int reverseBytes(int);
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public java.lang.String toString();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Integer);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public final class java.lang.Long {
    public java.lang.Long(long);
    public java.lang.Long(java.lang.String);
}

-assumenosideeffects public final class java.lang.Long {
    public static java.lang.String toString(long, int);
    public static java.lang.String toHexString(long);
    public static java.lang.String toOctalString(long);
    public static java.lang.String toBinaryString(long);
    public static java.lang.String toString(long);
    public static long parseLong(java.lang.String, int);
    public static long parseLong(java.lang.String);
    public static java.lang.Long valueOf(java.lang.String, int);
    public static java.lang.Long valueOf(java.lang.String);
    public static java.lang.Long valueOf(long);
    public static java.lang.Long decode(java.lang.String);
    public static java.lang.Long getLong(java.lang.String);
    public static java.lang.Long getLong(java.lang.String, long);
    public static java.lang.Long getLong(java.lang.String, java.lang.Long);
    public static long highestOneBit(long);
    public static long lowestOneBit(long);
    public static int numberOfLeadingZeros(long);
    public static int numberOfTrailingZeros(long);
    public static int bitCount(long);
    public static long rotateLeft(long, int);
    public static long rotateRight(long, int);
    public static long reverse(long);
    public static int signum(long);
    public static long reverseBytes(long);
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public java.lang.String toString();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Long);
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public abstract class java.lang.Number {
    public java.lang.Number();
}

#-assumenosideeffects public abstract class java.lang.Number {
#    public abstract int intValue();
#    public abstract long longValue();
#   public abstract float floatValue();
#   public abstract double doubleValue();
#    public byte byteValue();
#    public short shortValue();
#}

-assumenoexternalsideeffects public final class java.lang.Short {
    public java.lang.Short(short);
    public java.lang.Short(java.lang.String);
}

-assumenosideeffects public final class java.lang.Short {
    public static java.lang.String toString(short);
    public static short parseShort(java.lang.String);
    public static short parseShort(java.lang.String, int);
    public static java.lang.Short valueOf(java.lang.String, int);
    public static java.lang.Short valueOf(java.lang.String);
    public static java.lang.Short valueOf(short);
    public static java.lang.Short decode(java.lang.String);
    public static short reverseBytes(short);
    public byte byteValue();
    public short shortValue();
    public int intValue();
    public long longValue();
    public float floatValue();
    public double doubleValue();
#    public java.lang.String toString();
#    public int hashCode();
#    public boolean equals(java.lang.Object);
    public int compareTo(java.lang.Short);
#    public int compareTo(java.lang.Object);
}

#-assumenexternalosideeffects public final class java.lang.Void {
#}
#
#-assumenosideeffects public final class java.lang.Void {
#}

-assumenoexternalsideeffects public abstract class java.lang.Enum {
    protected java.lang.Enum(java.lang.String, int);
}

-assumenosideeffects public abstract class java.lang.Enum {
    public final java.lang.String name();
    public final int ordinal();
#    public java.lang.String toString();
#    public final boolean equals(java.lang.Object);
#    public final int hashCode();
#    protected final java.lang.Object clone();
    public final int compareTo(java.lang.Enum);
    public final java.lang.Class getDeclaringClass();
    public static java.lang.Enum valueOf(java.lang.Class, java.lang.String);
#    protected final void finalize();
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects public class java.lang.Throwable {
    public java.lang.Throwable();
    public java.lang.Throwable(java.lang.String);
    public java.lang.Throwable(java.lang.String, java.lang.Throwable);
    public java.lang.Throwable(java.lang.Throwable);
#    public synchronized java.lang.Throwable initCause(java.lang.Throwable);
#    public synchronized java.lang.Throwable fillInStackTrace();
#    public void setStackTrace(java.lang.StackTraceElement[]);
}

#-assumenosideeffects public class java.lang.Throwable {
#    public java.lang.String getMessage();
#    public java.lang.String getLocalizedMessage();
#    public java.lang.Throwable getCause();
#    public java.lang.String toString();
#    public java.lang.StackTraceElement[] getStackTrace();
#}

-assumenoexternalsideeffects public class java.lang.Error {
    public java.lang.Error();
    public java.lang.Error(java.lang.String);
    public java.lang.Error(java.lang.String, java.lang.Throwable);
    public java.lang.Error(java.lang.Throwable);
}

-assumenoexternalsideeffects public class java.lang.Exception {
    public java.lang.Exception();
    public java.lang.Exception(java.lang.String);
    public java.lang.Exception(java.lang.String, java.lang.Throwable);
    public java.lang.Exception(java.lang.Throwable);
}

-assumenosideeffects public final class java.lang.Math {
    public static double sin(double);
    public static double cos(double);
    public static double tan(double);
    public static double asin(double);
    public static double acos(double);
    public static double atan(double);
    public static double toRadians(double);
    public static double toDegrees(double);
    public static double exp(double);
    public static double log(double);
    public static double log10(double);
    public static double sqrt(double);
    public static double cbrt(double);
    public static double IEEEremainder(double, double);
    public static double ceil(double);
    public static double floor(double);
    public static double rint(double);
    public static double atan2(double, double);
    public static double pow(double, double);
    public static int round(float);
    public static long round(double);
    public static double random();
    public static int abs(int);
    public static long abs(long);
    public static float abs(float);
    public static double abs(double);
    public static int max(int, int);
    public static long max(long, long);
    public static float max(float, float);
    public static double max(double, double);
    public static int min(int, int);
    public static long min(long, long);
    public static float min(float, float);
    public static double min(double, double);
    public static double ulp(double);
    public static float ulp(float);
    public static double signum(double);
    public static float signum(float);
    public static double sinh(double);
    public static double cosh(double);
    public static double tanh(double);
    public static double hypot(double, double);
    public static double expm1(double);
    public static double log1p(double);
    public static double copySign(double, double);
    public static float copySign(float, float);
    public static int getExponent(float);
    public static int getExponent(double);
    public static double nextAfter(double, double);
    public static float nextAfter(float, double);
    public static double nextUp(double);
    public static float nextUp(float);
    public static double scalb(double, int);
    public static float scalb(float, int);
}

-assumenosideeffects public final class java.lang.StrictMath {
    public static double sin(double);
    public static double cos(double);
    public static double tan(double);
    public static double asin(double);
    public static double acos(double);
    public static double atan(double);
    public static strictfp double toRadians(double);
    public static strictfp double toDegrees(double);
    public static double exp(double);
    public static double log(double);
    public static double log10(double);
    public static double sqrt(double);
    public static double cbrt(double);
    public static double IEEEremainder(double, double);
    public static double ceil(double);
    public static double floor(double);
    public static double rint(double);
    public static double atan2(double, double);
    public static double pow(double, double);
    public static int round(float);
    public static long round(double);
    public static double random();
    public static int abs(int);
    public static long abs(long);
    public static float abs(float);
    public static double abs(double);
    public static int max(int, int);
    public static long max(long, long);
    public static float max(float, float);
    public static double max(double, double);
    public static int min(int, int);
    public static long min(long, long);
    public static float min(float, float);
    public static double min(double, double);
    public static double ulp(double);
    public static float ulp(float);
    public static double signum(double);
    public static float signum(float);
    public static double sinh(double);
    public static double cosh(double);
    public static double tanh(double);
    public static double hypot(double, double);
    public static double expm1(double);
    public static double log1p(double);
    public static double copySign(double, double);
    public static float copySign(float, float);
    public static int getExponent(float);
    public static int getExponent(double);
    public static double nextAfter(double, double);
    public static float nextAfter(float, double);
    public static double nextUp(double);
    public static float nextUp(float);
    public static double scalb(double, int);
    public static float scalb(float, int);
}

-assumenoexternalsideeffects public final class java.lang.Class {
}

-assumenosideeffects public final class java.lang.Class {
#    public java.lang.String toString();
#    public static java.lang.Class forName(java.lang.String);
#    public static java.lang.Class forName(java.lang.String, boolean, java.lang.ClassLoader);
#    public java.lang.Object newInstance();
    public boolean isInstance(java.lang.Object);
    public boolean isAssignableFrom(java.lang.Class);
    public boolean isInterface();
    public boolean isArray();
    public boolean isPrimitive();
    public boolean isAnnotation();
    public boolean isSynthetic();
    public java.lang.String getName();
    public java.lang.ClassLoader getClassLoader();
    public java.lang.reflect.TypeVariable[] getTypeParameters();
    public java.lang.Class getSuperclass();
    public java.lang.reflect.Type getGenericSuperclass();
    public java.lang.Package getPackage();
    public java.lang.Class[] getInterfaces();
    public java.lang.reflect.Type[] getGenericInterfaces();
    public java.lang.Class getComponentType();
    public int getModifiers();
    public java.lang.Object[] getSigners();
    public java.lang.reflect.Method getEnclosingMethod();
    public java.lang.reflect.Constructor getEnclosingConstructor();
    public java.lang.Class getDeclaringClass();
    public java.lang.Class getEnclosingClass();
    public java.lang.String getSimpleName();
    public java.lang.String getCanonicalName();
    public boolean isAnonymousClass();
    public boolean isLocalClass();
    public boolean isMemberClass();
    public java.lang.Class[] getClasses();
    public java.lang.reflect.Field[] getFields();
    public java.lang.reflect.Method[] getMethods();
    public java.lang.reflect.Constructor[] getConstructors();
#    public java.lang.reflect.Field getField(java.lang.String);
#    public java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[]);
#    public java.lang.reflect.Constructor getConstructor(java.lang.Class[]);
    public java.lang.Class[] getDeclaredClasses();
    public java.lang.reflect.Field[] getDeclaredFields();
    public java.lang.reflect.Method[] getDeclaredMethods();
    public java.lang.reflect.Constructor[] getDeclaredConstructors();
#    public java.lang.reflect.Field getDeclaredField(java.lang.String);
#    public java.lang.reflect.Method getDeclaredMethod(java.lang.String, java.lang.Class[]);
#    public java.lang.reflect.Constructor getDeclaredConstructor(java.lang.Class[]);
#    public java.io.InputStream getResourceAsStream(java.lang.String);
#    public java.net.URL getResource(java.lang.String);
    public java.security.ProtectionDomain getProtectionDomain();
    public boolean desiredAssertionStatus();
    public boolean isEnum();
    public java.lang.Object[] getEnumConstants();
    public java.lang.Object cast(java.lang.Object);
    public java.lang.Class asSubclass(java.lang.Class);
#    public java.lang.annotation.Annotation getAnnotation(java.lang.Class);
    public boolean isAnnotationPresent(java.lang.Class);
    public java.lang.annotation.Annotation[] getAnnotations();
    public java.lang.annotation.Annotation[] getDeclaredAnnotations();
}

-assumenoexternalsideeffects public abstract class java.lang.ClassLoader {
    protected java.lang.ClassLoader(java.lang.ClassLoader);
    protected java.lang.ClassLoader();
#    public synchronized void setDefaultAssertionStatus(boolean);
#    public synchronized void setPackageAssertionStatus(java.lang.String, boolean);
#    public synchronized void setClassAssertionStatus(java.lang.String, boolean);
#    public synchronized void clearAssertionStatus();
}

-assumenosideeffects public abstract class java.lang.ClassLoader {
    public static java.net.URL getSystemResource(java.lang.String);
    public static java.util.Enumeration getSystemResources(java.lang.String);
    public static java.io.InputStream getSystemResourceAsStream(java.lang.String);
    public static java.lang.ClassLoader getSystemClassLoader();
#    public java.lang.Class loadClass(java.lang.String);
#    protected synchronized java.lang.Class loadClass(java.lang.String, boolean);
#    protected java.lang.Class findClass(java.lang.String);
    protected final java.lang.Class defineClass(byte[], int, int);
    protected final java.lang.Class defineClass(java.lang.String, byte[], int, int);
    protected final java.lang.Class defineClass(java.lang.String, byte[], int, int, java.security.ProtectionDomain);
    protected final java.lang.Class defineClass(java.lang.String, java.nio.ByteBuffer, java.security.ProtectionDomain);
#    protected final void resolveClass(java.lang.Class);
    protected final java.lang.Class findSystemClass(java.lang.String);
    protected final java.lang.Class findLoadedClass(java.lang.String);
#    protected final void setSigners(java.lang.Class, java.lang.Object[]);
#    public java.net.URL getResource(java.lang.String);
#    public java.util.Enumeration getResources(java.lang.String);
#    protected java.net.URL findResource(java.lang.String);
#    protected java.util.Enumeration findResources(java.lang.String);
#    public java.io.InputStream getResourceAsStream(java.lang.String);
    public final java.lang.ClassLoader getParent();
#    protected java.lang.Package definePackage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.net.URL);
#    protected java.lang.Package getPackage(java.lang.String);
#    protected java.lang.Package[] getPackages();
#    protected java.lang.String findLibrary(java.lang.String);
}

-assumenoexternalsideeffects public class java.lang.InheritableThreadLocal {
    public java.lang.InheritableThreadLocal();
}

#-assumenosideeffects public class java.lang.InheritableThreadLocal {
#    protected java.lang.Object childValue(java.lang.Object);
#}

-assumenosideeffects public class java.lang.Package {
    public static java.lang.Package getPackage(java.lang.String);
    public static java.lang.Package[] getPackages();
#    public java.lang.String getName();
#    public java.lang.String getSpecificationTitle();
#    public java.lang.String getSpecificationVersion();
#    public java.lang.String getSpecificationVendor();
#    public java.lang.String getImplementationTitle();
#    public java.lang.String getImplementationVersion();
#    public java.lang.String getImplementationVendor();
#    public boolean isSealed();
#    public boolean isSealed(java.net.URL);
#    public boolean isCompatibleWith(java.lang.String);
#    public int hashCode();
#    public java.lang.String toString();
#    public java.lang.annotation.Annotation getAnnotation(java.lang.Class);
#    public boolean isAnnotationPresent(java.lang.Class);
#    public java.lang.annotation.Annotation[] getAnnotations();
#    public java.lang.annotation.Annotation[] getDeclaredAnnotations();
}

-assumenoexternalsideeffects public abstract class java.lang.Process {
    public java.lang.Process();
}

#-assumenosideeffects public abstract class java.lang.Process {
#    public abstract java.io.OutputStream getOutputStream();
#    public abstract java.io.InputStream getInputStream();
#    public abstract java.io.InputStream getErrorStream();
#    public abstract int exitValue();
#    public abstract void destroy();
#}

-assumenoexternalsideeffects public final class java.lang.ProcessBuilder {
#    public java.lang.ProcessBuilder(java.util.List);
    public java.lang.ProcessBuilder(java.lang.String[]);
}

-assumenosideeffects public final class java.lang.ProcessBuilder {
#    public java.lang.ProcessBuilder command(java.util.List);
    public java.lang.ProcessBuilder command(java.lang.String[]);
    public java.util.List command();
    public java.util.Map environment();
    public java.io.File directory();
    public java.lang.ProcessBuilder directory(java.io.File);
    public boolean redirectErrorStream();
    public java.lang.ProcessBuilder redirectErrorStream(boolean);
#    public java.lang.Process start();
}

#-assumenoexternalsideeffects public class java.lang.Runtime {
#    public static void runFinalizersOnExit(boolean);
#    public void exit(int);
#    public void addShutdownHook(java.lang.Thread);
#    public boolean removeShutdownHook(java.lang.Thread);
#    public void halt(int);
#    public java.lang.Process exec(java.lang.String);
#    public java.lang.Process exec(java.lang.String, java.lang.String[]);
#    public java.lang.Process exec(java.lang.String, java.lang.String[], java.io.File);
#    public java.lang.Process exec(java.lang.String[]);
#    public java.lang.Process exec(java.lang.String[], java.lang.String[]);
#    public java.lang.Process exec(java.lang.String[], java.lang.String[], java.io.File);
#    public void gc();
#    public void runFinalization();
#    public void traceInstructions(boolean);
#    public void traceMethodCalls(boolean);
#    public void load(java.lang.String);
#    public void loadLibrary(java.lang.String);
#}

-assumenosideeffects public class java.lang.Runtime {
    public static java.lang.Runtime getRuntime();
    public int availableProcessors();
    public long freeMemory();
    public long totalMemory();
    public long maxMemory();
    public java.io.InputStream getLocalizedInputStream(java.io.InputStream);
    public java.io.OutputStream getLocalizedOutputStream(java.io.OutputStream);
}

-assumenoexternalsideeffects public final class java.lang.RuntimePermission {
    public java.lang.RuntimePermission(java.lang.String);
    public java.lang.RuntimePermission(java.lang.String, java.lang.String);
}

-assumenoexternalsideeffects public class java.lang.SecurityManager {
    public java.lang.SecurityManager();
}

#-assumenosideeffects public class java.lang.SecurityManager {
#    protected java.lang.Class[] getClassContext();
#    protected java.lang.ClassLoader currentClassLoader();
#    protected java.lang.Class currentLoadedClass();
#    protected int classDepth(java.lang.String);
#    protected int classLoaderDepth();
#    protected boolean inClass(java.lang.String);
#    protected boolean inClassLoader();
#    public java.lang.Object getSecurityContext();
#    public void checkPermission(java.security.Permission);
#    public void checkPermission(java.security.Permission, java.lang.Object);
#    public void checkCreateClassLoader();
#    public void checkAccess(java.lang.Thread);
#    public void checkAccess(java.lang.ThreadGroup);
#    public void checkExit(int);
#    public void checkExec(java.lang.String);
#    public void checkLink(java.lang.String);
#    public void checkRead(java.io.FileDescriptor);
#    public void checkRead(java.lang.String);
#    public void checkRead(java.lang.String, java.lang.Object);
#    public void checkWrite(java.io.FileDescriptor);
#    public void checkWrite(java.lang.String);
#    public void checkDelete(java.lang.String);
#    public void checkConnect(java.lang.String, int);
#    public void checkConnect(java.lang.String, int, java.lang.Object);
#    public void checkListen(int);
#    public void checkAccept(java.lang.String, int);
#    public void checkMulticast(java.net.InetAddress);
#    public void checkMulticast(java.net.InetAddress, byte);
#    public void checkPropertiesAccess();
#    public void checkPropertyAccess(java.lang.String);
#    public boolean checkTopLevelWindow(java.lang.Object);
#    public void checkPrintJobAccess();
#    public void checkSystemClipboardAccess();
#    public void checkAwtEventQueueAccess();
#    public void checkPackageAccess(java.lang.String);
#    public void checkPackageDefinition(java.lang.String);
#    public void checkSetFactory();
#    public void checkMemberAccess(java.lang.Class, int);
#    public void checkSecurityAccess(java.lang.String);
#    public java.lang.ThreadGroup getThreadGroup();
#}

-assumenoexternalsideeffects public final class java.lang.StackTraceElement {
    public java.lang.StackTraceElement(java.lang.String, java.lang.String, java.lang.String, int);
}

-assumenosideeffects public final class java.lang.StackTraceElement {
    public java.lang.String getFileName();
    public int getLineNumber();
    public java.lang.String getClassName();
    public java.lang.String getMethodName();
    public boolean isNativeMethod();
#    public java.lang.String toString();
#    public boolean equals(java.lang.Object);
#    public int hashCode();
}

-assumenosideeffects public final class java.lang.System {
    public static java.io.Console console();
    public static java.nio.channels.Channel inheritedChannel();
    public static java.lang.SecurityManager getSecurityManager();
    public static long currentTimeMillis();
    public static long nanoTime();
    public static int identityHashCode(java.lang.Object);
#    public static java.util.Properties getProperties();
#    public static void setProperties(java.util.Properties);
#    public static java.lang.String getProperty(java.lang.String);
#    public static java.lang.String getProperty(java.lang.String, java.lang.String);
#    public static java.lang.String getenv(java.lang.String);
#    public static java.util.Map getenv();
    public static java.lang.String mapLibraryName(java.lang.String);
}

-assumenoexternalsideeffects public class java.lang.Thread {
    public java.lang.Thread();
    public java.lang.Thread(java.lang.Runnable);
    public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable);
    public java.lang.Thread(java.lang.String);
    public java.lang.Thread(java.lang.ThreadGroup, java.lang.String);
    public java.lang.Thread(java.lang.Runnable, java.lang.String);
    public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String);
    public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String, long);
    public final void setPriority(int);
    public final void setName(java.lang.String);
    public final void setDaemon(boolean);
#    public void setContextClassLoader(java.lang.ClassLoader);
#    public void setUncaughtExceptionHandler(java.lang.Thread$UncaughtExceptionHandler);
}

-assumenosideeffects public class java.lang.Thread {
    public static java.lang.Thread currentThread();
    public static boolean interrupted();
#    public static int activeCount();
    public static boolean holdsLock(java.lang.Object);
    public static java.util.Map getAllStackTraces();
    public static java.lang.Thread$UncaughtExceptionHandler getDefaultUncaughtExceptionHandler();
#    protected java.lang.Object clone();
#    public boolean isInterrupted();
    public final boolean isAlive();
    public final int getPriority();
    public final java.lang.String getName();
    public final java.lang.ThreadGroup getThreadGroup();
#    public int countStackFrames();
    public final boolean isDaemon();
#    public java.lang.String toString();
#    public java.lang.ClassLoader getContextClassLoader();
#    public java.lang.StackTraceElement[] getStackTrace();
#    public long getId();
#    public java.lang.Thread$State getState();
#    public java.lang.Thread$UncaughtExceptionHandler getUncaughtExceptionHandler();
}

-assumenoexternalsideeffects public class java.lang.ThreadDeath {
    public java.lang.ThreadDeath();
}

-assumenoexternalsideeffects public class java.lang.ThreadGroup {
    public java.lang.ThreadGroup(java.lang.String);
    public java.lang.ThreadGroup(java.lang.ThreadGroup, java.lang.String);
    public final void setDaemon(boolean);
    public final void setMaxPriority(int);
#    public void uncaughtException(java.lang.Thread, java.lang.Throwable);
#    public boolean allowThreadSuspension(boolean);
}

-assumenosideeffects public class java.lang.ThreadGroup {
    public final java.lang.String getName();
    public final java.lang.ThreadGroup getParent();
    public final int getMaxPriority();
    public final boolean isDaemon();
#    public synchronized boolean isDestroyed();
    public final boolean parentOf(java.lang.ThreadGroup);
#    public int activeCount();
#    public int activeGroupCount();
#    public java.lang.String toString();
}

-assumenoexternalsideeffects public class java.lang.ThreadLocal {
    public java.lang.ThreadLocal();
}

#-assumenosideeffects public class java.lang.ThreadLocal {
#    protected java.lang.Object initialValue();
#    public java.lang.Object get();
#    public void remove();
#}

#-assumenoexternalsideeffects public class java.lang.ThreadLocal {
#    public void set(java.lang.Object);
#}

-assumenoexternalsideeffects public final class java.lang.String {
    public java.lang.String();
    public java.lang.String(java.lang.String);
    public java.lang.String(char[]);
    public java.lang.String(char[], int, int);
    public java.lang.String(int[], int, int);
    public java.lang.String(byte[], int, int, int);
    public java.lang.String(byte[], int);
    public java.lang.String(byte[], int, int, java.lang.String);
#    public java.lang.String(byte[], int, int, java.nio.charset.Charset);
    public java.lang.String(byte[], java.lang.String);
#    public java.lang.String(byte[], java.nio.charset.Charset);
    public java.lang.String(byte[], int, int);
    public java.lang.String(byte[]);
    public java.lang.String(java.lang.StringBuffer);
    public java.lang.String(java.lang.StringBuilder);
}

-assumenosideeffects public final class java.lang.String {
    public static java.lang.String format(java.lang.String, java.lang.Object[]);
    public static java.lang.String format(java.util.Locale, java.lang.String, java.lang.Object[]);
    public static java.lang.String valueOf(java.lang.Object);
    public static java.lang.String valueOf(char[]);
    public static java.lang.String valueOf(char[], int, int);
    public static java.lang.String copyValueOf(char[], int, int);
    public static java.lang.String copyValueOf(char[]);
    public static java.lang.String valueOf(boolean);
    public static java.lang.String valueOf(char);
    public static java.lang.String valueOf(int);
    public static java.lang.String valueOf(long);
    public static java.lang.String valueOf(float);
    public static java.lang.String valueOf(double);
    public int length();
    public boolean isEmpty();
    public char charAt(int);
    public int codePointAt(int);
    public int codePointBefore(int);
    public int codePointCount(int, int);
    public int offsetByCodePoints(int, int);
#    public void getChars(int, int, char[], int);
#    public void getBytes(int, int, byte[], int);
    public byte[] getBytes(java.lang.String);
    public byte[] getBytes(java.nio.charset.Charset);
    public byte[] getBytes();
#    public boolean equals(java.lang.Object);
    public boolean contentEquals(java.lang.StringBuffer);
#    public boolean contentEquals(java.lang.CharSequence);
    public boolean equalsIgnoreCase(java.lang.String);
    public int compareTo(java.lang.String);
    public int compareToIgnoreCase(java.lang.String);
    public boolean regionMatches(int, java.lang.String, int, int);
    public boolean regionMatches(boolean, int, java.lang.String, int, int);
    public boolean startsWith(java.lang.String, int);
    public boolean startsWith(java.lang.String);
    public boolean endsWith(java.lang.String);
#    public int hashCode();
    public int indexOf(int);
    public int indexOf(int, int);
    public int lastIndexOf(int);
    public int lastIndexOf(int, int);
    public int indexOf(java.lang.String);
    public int indexOf(java.lang.String, int);
    public int lastIndexOf(java.lang.String);
    public int lastIndexOf(java.lang.String, int);
    public java.lang.String substring(int);
    public java.lang.String substring(int, int);
    public java.lang.CharSequence subSequence(int, int);
    public java.lang.String concat(java.lang.String);
    public java.lang.String replace(char, char);
    public boolean matches(java.lang.String);
#    public boolean contains(java.lang.CharSequence);
    public java.lang.String replaceFirst(java.lang.String, java.lang.String);
    public java.lang.String replaceAll(java.lang.String, java.lang.String);
#    public java.lang.String replace(java.lang.CharSequence, java.lang.CharSequence);
    public java.lang.String[] split(java.lang.String, int);
    public java.lang.String[] split(java.lang.String);
    public java.lang.String toLowerCase(java.util.Locale);
    public java.lang.String toLowerCase();
    public java.lang.String toUpperCase(java.util.Locale);
    public java.lang.String toUpperCase();
    public java.lang.String trim();
#    public java.lang.String toString();
    public char[] toCharArray();
#    public int compareTo(java.lang.Object);
}

-assumenoexternalsideeffects abstract class java.lang.AbstractStringBuilder {
    public void ensureCapacity(int);
    public void trimToSize();
    public void setLength(int);
    public void setCharAt(int, char);
    public java.lang.AbstractStringBuilder append(java.lang.Object);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
    public java.lang.AbstractStringBuilder append(char[]);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, char[]);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
    public java.lang.AbstractStringBuilder insert(int, boolean);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder reverse();
#    public java.lang.Appendable append(char);
#    public java.lang.Appendable append(java.lang.CharSequence, int, int);
#    public java.lang.Appendable append(java.lang.CharSequence);
}

-assumenoexternalreturnvalues abstract class java.lang.AbstractStringBuilder {
    public java.lang.AbstractStringBuilder append(java.lang.Object);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
    public java.lang.AbstractStringBuilder append(char[]);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, char[]);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
    public java.lang.AbstractStringBuilder insert(int, boolean);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder reverse();
}

-assumenosideeffects abstract class java.lang.AbstractStringBuilder {
    public int length();
    public int capacity();
    public char charAt(int);
    public int codePointAt(int);
    public int codePointBefore(int);
    public int codePointCount(int, int);
    public int offsetByCodePoints(int, int);
#    public void getChars(int, int, char[], int);
    public java.lang.String substring(int);
    public java.lang.CharSequence subSequence(int, int);
    public java.lang.String substring(int, int);
    public int indexOf(java.lang.String);
    public int indexOf(java.lang.String, int);
    public int lastIndexOf(java.lang.String);
    public int lastIndexOf(java.lang.String, int);
#    public abstract java.lang.String toString();
}

-assumenoexternalsideeffects public final class java.lang.StringBuffer {
    public java.lang.StringBuffer();
    public java.lang.StringBuffer(int);
    public java.lang.StringBuffer(java.lang.String);
#    public java.lang.StringBuffer(java.lang.CharSequence);
    public void ensureCapacity(int);
    public void trimToSize();
    public void setLength(int);
    public void setCharAt(int, char);
    public java.lang.StringBuffer append(java.lang.Object);
    public java.lang.StringBuffer append(java.lang.String);
    public java.lang.StringBuffer append(java.lang.StringBuffer);
#    public java.lang.StringBuffer append(java.lang.CharSequence);
#    public java.lang.StringBuffer append(java.lang.CharSequence, int, int);
    public java.lang.StringBuffer append(char[]);
    public java.lang.StringBuffer append(char[], int, int);
    public java.lang.StringBuffer append(boolean);
    public java.lang.StringBuffer append(char);
    public java.lang.StringBuffer append(int);
    public java.lang.StringBuffer appendCodePoint(int);
    public java.lang.StringBuffer append(long);
    public java.lang.StringBuffer append(float);
    public java.lang.StringBuffer append(double);
    public java.lang.StringBuffer delete(int, int);
    public java.lang.StringBuffer deleteCharAt(int);
    public java.lang.StringBuffer replace(int, int, java.lang.String);
    public java.lang.StringBuffer insert(int, char[], int, int);
    public java.lang.StringBuffer insert(int, java.lang.Object);
    public java.lang.StringBuffer insert(int, java.lang.String);
    public java.lang.StringBuffer insert(int, char[]);
#    public java.lang.StringBuffer insert(int, java.lang.CharSequence);
#    public java.lang.StringBuffer insert(int, java.lang.CharSequence, int, int);
    public java.lang.StringBuffer insert(int, boolean);
    public java.lang.StringBuffer insert(int, char);
    public java.lang.StringBuffer insert(int, int);
    public java.lang.StringBuffer insert(int, long);
    public java.lang.StringBuffer insert(int, float);
    public java.lang.StringBuffer insert(int, double);
    public java.lang.StringBuffer reverse();
    public java.lang.AbstractStringBuilder reverse();
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, boolean);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
    public java.lang.AbstractStringBuilder insert(int, char[]);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(char[]);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.Object);
#    public java.lang.Appendable append(char);
#    public java.lang.Appendable append(java.lang.CharSequence, int, int);
#    public java.lang.Appendable append(java.lang.CharSequence);
}

-assumenoexternalreturnvalues public final class java.lang.StringBuffer {
    public java.lang.StringBuffer append(java.lang.Object);
    public java.lang.StringBuffer append(java.lang.String);
    public java.lang.StringBuffer append(java.lang.StringBuffer);
#    public java.lang.StringBuffer append(java.lang.CharSequence);
#    public java.lang.StringBuffer append(java.lang.CharSequence, int, int);
    public java.lang.StringBuffer append(char[]);
    public java.lang.StringBuffer append(char[], int, int);
    public java.lang.StringBuffer append(boolean);
    public java.lang.StringBuffer append(char);
    public java.lang.StringBuffer append(int);
    public java.lang.StringBuffer appendCodePoint(int);
    public java.lang.StringBuffer append(long);
    public java.lang.StringBuffer append(float);
    public java.lang.StringBuffer append(double);
    public java.lang.StringBuffer delete(int, int);
    public java.lang.StringBuffer deleteCharAt(int);
    public java.lang.StringBuffer replace(int, int, java.lang.String);
    public java.lang.StringBuffer insert(int, char[], int, int);
    public java.lang.StringBuffer insert(int, java.lang.Object);
    public java.lang.StringBuffer insert(int, java.lang.String);
    public java.lang.StringBuffer insert(int, char[]);
#    public java.lang.StringBuffer insert(int, java.lang.CharSequence);
#    public java.lang.StringBuffer insert(int, java.lang.CharSequence, int, int);
    public java.lang.StringBuffer insert(int, boolean);
    public java.lang.StringBuffer insert(int, char);
    public java.lang.StringBuffer insert(int, int);
    public java.lang.StringBuffer insert(int, long);
    public java.lang.StringBuffer insert(int, float);
    public java.lang.StringBuffer insert(int, double);
    public java.lang.StringBuffer reverse();
    public java.lang.AbstractStringBuilder reverse();
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, boolean);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
    public java.lang.AbstractStringBuilder insert(int, char[]);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(char[]);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.Object);
#    public java.lang.Appendable append(char);
#    public java.lang.Appendable append(java.lang.CharSequence, int, int);
#    public java.lang.Appendable append(java.lang.CharSequence);
}


-assumenosideeffects public final class java.lang.StringBuffer {
    public int length();
    public int capacity();
    public char charAt(int);
    public int codePointAt(int);
    public int codePointBefore(int);
    public int codePointCount(int, int);
    public int offsetByCodePoints(int, int);
#    public void getChars(int, int, char[], int);
    public java.lang.String substring(int);
    public java.lang.CharSequence subSequence(int, int);
    public java.lang.String substring(int, int);
    public int indexOf(java.lang.String);
    public int indexOf(java.lang.String, int);
    public int lastIndexOf(java.lang.String);
    public int lastIndexOf(java.lang.String, int);
#    public java.lang.String toString();
}

-assumenoexternalsideeffects public final class java.lang.StringBuilder {
    public java.lang.StringBuilder();
    public java.lang.StringBuilder(int);
    public java.lang.StringBuilder(java.lang.String);
#    public java.lang.StringBuilder(java.lang.CharSequence);
    public void setCharAt(int, char);
    public void setLength(int);
    public void trimToSize();
    public void ensureCapacity(int);
#    public java.lang.StringBuilder append(java.lang.Object);
    public java.lang.StringBuilder append(java.lang.String);
    public java.lang.StringBuilder append(java.lang.StringBuffer);
#    public java.lang.StringBuilder append(java.lang.CharSequence);
#    public java.lang.StringBuilder append(java.lang.CharSequence, int, int);
    public java.lang.StringBuilder append(char[]);
    public java.lang.StringBuilder append(char[], int, int);
    public java.lang.StringBuilder append(boolean);
    public java.lang.StringBuilder append(char);
    public java.lang.StringBuilder append(int);
    public java.lang.StringBuilder append(long);
    public java.lang.StringBuilder append(float);
    public java.lang.StringBuilder append(double);
    public java.lang.StringBuilder appendCodePoint(int);
    public java.lang.StringBuilder delete(int, int);
    public java.lang.StringBuilder deleteCharAt(int);
    public java.lang.StringBuilder replace(int, int, java.lang.String);
    public java.lang.StringBuilder insert(int, char[], int, int);
    public java.lang.StringBuilder insert(int, java.lang.Object);
    public java.lang.StringBuilder insert(int, java.lang.String);
    public java.lang.StringBuilder insert(int, char[]);
#    public java.lang.StringBuilder insert(int, java.lang.CharSequence);
#    public java.lang.StringBuilder insert(int, java.lang.CharSequence, int, int);
    public java.lang.StringBuilder insert(int, boolean);
    public java.lang.StringBuilder insert(int, char);
    public java.lang.StringBuilder insert(int, int);
    public java.lang.StringBuilder insert(int, long);
    public java.lang.StringBuilder insert(int, float);
    public java.lang.StringBuilder insert(int, double);
    public java.lang.StringBuilder reverse();
    public java.lang.AbstractStringBuilder reverse();
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, boolean);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
    public java.lang.AbstractStringBuilder insert(int, char[]);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(char[]);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.Object);
#    public java.lang.Appendable append(char);
#    public java.lang.Appendable append(java.lang.CharSequence, int, int);
#    public java.lang.Appendable append(java.lang.CharSequence);
}

-assumenoexternalreturnvalues public final class java.lang.StringBuilder {
    public java.lang.StringBuilder append(java.lang.Object);
    public java.lang.StringBuilder append(java.lang.String);
    public java.lang.StringBuilder append(java.lang.StringBuffer);
#    public java.lang.StringBuilder append(java.lang.CharSequence);
#    public java.lang.StringBuilder append(java.lang.CharSequence, int, int);
    public java.lang.StringBuilder append(char[]);
    public java.lang.StringBuilder append(char[], int, int);
    public java.lang.StringBuilder append(boolean);
    public java.lang.StringBuilder append(char);
    public java.lang.StringBuilder append(int);
    public java.lang.StringBuilder append(long);
    public java.lang.StringBuilder append(float);
    public java.lang.StringBuilder append(double);
    public java.lang.StringBuilder appendCodePoint(int);
    public java.lang.StringBuilder delete(int, int);
    public java.lang.StringBuilder deleteCharAt(int);
    public java.lang.StringBuilder replace(int, int, java.lang.String);
    public java.lang.StringBuilder insert(int, char[], int, int);
    public java.lang.StringBuilder insert(int, java.lang.Object);
    public java.lang.StringBuilder insert(int, java.lang.String);
    public java.lang.StringBuilder insert(int, char[]);
#    public java.lang.StringBuilder insert(int, java.lang.CharSequence);
#    public java.lang.StringBuilder insert(int, java.lang.CharSequence, int, int);
    public java.lang.StringBuilder insert(int, boolean);
    public java.lang.StringBuilder insert(int, char);
    public java.lang.StringBuilder insert(int, int);
    public java.lang.StringBuilder insert(int, long);
    public java.lang.StringBuilder insert(int, float);
    public java.lang.StringBuilder insert(int, double);
    public java.lang.StringBuilder reverse();
    public java.lang.AbstractStringBuilder reverse();
    public java.lang.AbstractStringBuilder insert(int, double);
    public java.lang.AbstractStringBuilder insert(int, float);
    public java.lang.AbstractStringBuilder insert(int, long);
    public java.lang.AbstractStringBuilder insert(int, int);
    public java.lang.AbstractStringBuilder insert(int, char);
    public java.lang.AbstractStringBuilder insert(int, boolean);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder insert(int, java.lang.CharSequence);
    public java.lang.AbstractStringBuilder insert(int, char[]);
    public java.lang.AbstractStringBuilder insert(int, java.lang.String);
    public java.lang.AbstractStringBuilder insert(int, java.lang.Object);
    public java.lang.AbstractStringBuilder insert(int, char[], int, int);
    public java.lang.AbstractStringBuilder replace(int, int, java.lang.String);
    public java.lang.AbstractStringBuilder deleteCharAt(int);
    public java.lang.AbstractStringBuilder appendCodePoint(int);
    public java.lang.AbstractStringBuilder delete(int, int);
    public java.lang.AbstractStringBuilder append(double);
    public java.lang.AbstractStringBuilder append(float);
    public java.lang.AbstractStringBuilder append(long);
    public java.lang.AbstractStringBuilder append(int);
    public java.lang.AbstractStringBuilder append(char);
    public java.lang.AbstractStringBuilder append(boolean);
    public java.lang.AbstractStringBuilder append(char[], int, int);
    public java.lang.AbstractStringBuilder append(char[]);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence, int, int);
#    public java.lang.AbstractStringBuilder append(java.lang.CharSequence);
    public java.lang.AbstractStringBuilder append(java.lang.StringBuffer);
    public java.lang.AbstractStringBuilder append(java.lang.String);
    public java.lang.AbstractStringBuilder append(java.lang.Object);
#    public java.lang.Appendable append(char);
#    public java.lang.Appendable append(java.lang.CharSequence, int, int);
#    public java.lang.Appendable append(java.lang.CharSequence);
}

-assumenosideeffects public final class java.lang.StringBuilder {
    public int indexOf(java.lang.String);
    public int indexOf(java.lang.String, int);
    public int lastIndexOf(java.lang.String);
    public int lastIndexOf(java.lang.String, int);
#    public java.lang.String toString();
    public java.lang.String substring(int, int);
    public java.lang.CharSequence subSequence(int, int);
    public java.lang.String substring(int);
#    public void getChars(int, int, char[], int);
    public int offsetByCodePoints(int, int);
    public int codePointCount(int, int);
    public int codePointBefore(int);
    public int codePointAt(int);
    public char charAt(int);
    public int capacity();
    public int length();
}
