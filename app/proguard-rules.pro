# kotlinx.serialization keeps its generated serializers via companion objects that R8
# cannot see are reachable from the reflective lookup in `serializer()`.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.arturomanzoli.rastreado.**$$serializer { *; }
-keepclasseswithmembers class io.github.arturomanzoli.rastreado.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class io.github.arturomanzoli.rastreado.**
-keep, allowobfuscation, allowshrinking class <1>

# OkHttp ships optional Conscrypt/BouncyCastle integrations that are absent at runtime.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
