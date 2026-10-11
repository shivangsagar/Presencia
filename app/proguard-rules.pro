# Keep generic type signatures required by Gson.
-keepattributes Signature

# Preserve Gson TypeToken and its generic type information.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Preserve SubjectData and its fields used by Gson.
-keep class com.shivangsagar.presencia.SubjectData { *; }