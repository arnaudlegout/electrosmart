# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# remove all calls to Log.* when compiling in release mode.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Crashlytics specific rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception # Optional: Keep custom exceptions.

# Crashlytics specific rules for faster compilation
# -keep class com.crashlytics.** { *; }
# -dontwarn com.crashlytics.**

#-keep class com.google.firebase.crashlytics.** { *; }
#-dontwarn com.google.firebase.crashlytics.**
