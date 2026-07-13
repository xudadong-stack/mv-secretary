# MV-Secretary - Private File Vault
-keep class com.secretary.room.** { *; }
-keepattributes *Annotation*

# 优量汇（腾讯广告）SDK ProGuard 规则
-keep class com.qq.e.** { *; }
-keep class com.qq.e.comm.** { *; }
-keep class com.qq.e.ads.** { *; }
-dontwarn com.qq.e.**
