# Keep youtubedl-android and JNI bindings
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

# Keep models for reflection/serialization
-keep class com.eurtlabs.stash.data.model.** { *; }
