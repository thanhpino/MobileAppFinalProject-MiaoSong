# MiaoSong release ProGuard / R8 rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile, LineNumberTable

# Core domain models & Room entities
-keep class vn.miaosong.core.model.** { *; }
-keep class vn.miaosong.core.data.local.** { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-dontwarn kotlinx.serialization.json.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class vn.miaosong.**$$serializer { *; }
-keepclassmembers class vn.miaosong.** {
    *** Companion;
}
-keepclasseswithmembers class vn.miaosong.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OSMDroid OpenStreetMap (avoid stripping tile/overlay classes in release AAB)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-dontwarn org.slf4j.**

# Firebase / Firestore / Hilt
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
