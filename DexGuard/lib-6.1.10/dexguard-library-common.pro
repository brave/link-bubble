# Common DexGuard configuration for debug versions and release versions.
# Copyright (c) 2012-2015 GuardSquare NV

# Keep some attributes that the compiler needs.
-keepattributes Exceptions,Deprecated,EnclosingMethod

# Keep all public API.
-keep public class * {
    public protected *;
}

-include dexguard-common.pro
