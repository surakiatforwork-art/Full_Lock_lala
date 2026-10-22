# Android entry points are preserved by AGP's generated manifest rules.
# No application-wide keep rule: internal filters, models, and helpers can be renamed/optimized.
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-repackageclasses 'com.fulllock.internal'
