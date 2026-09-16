# Keep Kotlin serialization metadata on the serializable model classes.
# The kotlinx-serialization runtime ships default rules, these are explicit
# insurance for the app's own @Serializable types.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.skooldev.shweep.**$$serializer { *; }

-keepclassmembers class com.skooldev.shweep.** {
    *** Companion;
}

-keepclasseswithmembers class com.skooldev.shweep.** {
    kotlinx.serialization.KSerializer serializer(...);
}
