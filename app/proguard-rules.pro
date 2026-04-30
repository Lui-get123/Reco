# --- Reglas de ProGuard para RepartoCobro ---

# 1. Preservar modelos usados en JSON
# Es crítico mantener los nombres de los campos para que la deserialización de Supabase/JSON funcione.
-keepclassmembers class com.example.repartocobro.model.** { *; }
-keep class com.example.repartocobro.model.** { *; }

# 2. Preservar anotaciones @Keep
# Cualquier clase o método marcado con @Keep no será ofuscado.
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

# 3. Reglas para OkHttp y Retrofit (si se usara)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**
-dontwarn okio.**

# 4. Proteger la lógica de Supabase y Membresía
# Evitamos que se eliminen o renombren métodos críticos de validación.
-keep class com.example.repartocobro.data.SupabaseLicenseRepository {
    public *** redeemCode(...);
    public *** getLicenseStatus(...);
}

# 5. Reglas generales de Android
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# --- DOCUMENTACIÓN PARA NUEVAS LIBRERÍAS ---
# Si agregas una librería nueva y la app falla en modo Release:
# 1. Busca "ProGuard rules for [NombreLibrería]" en internet.
# 2. Copia las reglas aquí.
# 3. Comprueba si la librería usa Reflexión o JSON, en cuyo caso usa -keep para sus modelos.
