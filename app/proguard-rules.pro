# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-android
-dontpreverify
-optimizations !code/simplification/arithmetic
-keepattributes SourceFile, LineNumberTable, Exception, *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontobfuscate

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keepclassmembers class * {
    @com.google.api.client.util.Key *;
}

-keepclassmembers class de.binarynoise.appdate.** { # <-- change package name to your app's
    *** Companion;
}
-keep class de.binarynoise.appdate.classes.json.***

-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.jvm.internal.impl.load.java.*

-keep class * {
	@com.fasterxml.jackson.annotation.* *;
}

# AboutLibrary
-keep class .R
-keep class **.R$* {
    <fields>;
}
