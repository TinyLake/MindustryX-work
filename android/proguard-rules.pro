-dontobfuscate

-keep class mindustry.** { *; }
-keep class arc.** { *; }
-keep class net.jpountz.** { *; }
-keep class rhino.** { *; }
-keep class com.android.dex.** { *; }
-keep class mindustryX.** { *; }
-keep class kotlin.** { *; }
-keep class org.jetbrains.annotations.** { *; }
-keep class org.intellij.lang.annotations.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-dontwarn javax.naming.**

#-printusage out.txt