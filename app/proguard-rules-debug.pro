# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# Crashlytics specific rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception # Optional: Keep custom exceptions.

# Crashlytics specific rules for faster compilation
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**
