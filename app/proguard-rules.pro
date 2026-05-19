# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class vn.kba2018.attendance.**$$serializer { *; }
-keepclassmembers class vn.kba2018.attendance.** { *** Companion; }
-keepclasseswithmembers class vn.kba2018.attendance.** { kotlinx.serialization.KSerializer serializer(...); }
