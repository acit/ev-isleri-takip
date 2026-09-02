# ============================================
# AILE TAKIP - ProGuard Rules
# ============================================

# ---- Room Database ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ---- Data Models (Room entities) ----
-keep class com.aile.takip.data.model.** { *; }
-keep class com.aile.takip.data.model.Task { *; }
-keep class com.aile.takip.data.model.InventoryItem { *; }
-keep class com.aile.takip.data.model.Budget { *; }
-keep class com.aile.takip.data.model.Expense { *; }
-keep class com.aile.takip.data.model.Invoice { *; }
-keep class com.aile.takip.data.model.Message { *; }
-keep class com.aile.takip.data.model.ShoppingItem { *; }
-keep class com.aile.takip.data.model.FamilyMember { *; }
-keep class com.aile.takip.data.model.MealPlan { *; }
-keep class com.aile.takip.data.model.SportsClub { *; }
-keep class com.aile.takip.data.model.WorkoutLog { *; }
-keep class com.aile.takip.data.model.CalorieLog { *; }
-keep class com.aile.takip.data.model.MenstrualCycle { *; }
-keep class com.aile.takip.data.model.UserAuth { *; }
-keep class com.aile.takip.data.model.SyncEvent { *; }
-keep class com.aile.takip.data.model.Note { *; }
-keep class com.aile.takip.data.model.Reminder { *; }
-keep class com.aile.takip.data.model.WaterLog { *; }
-keep class com.aile.takip.data.model.SleepLog { *; }
-keep class com.aile.takip.data.model.PriceRecord { *; }
-keep class com.aile.takip.data.model.Attachment { *; }

# ---- Gson ----
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.database.** { *; }
-keep class com.google.firebase.auth.** { *; }

# ---- Kotlin Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---- Compose ----
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---- ML Kit Barcode ----
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ---- CameraX ----
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Coil ----
-keep class coil3.** { *; }
-dontwarn coil3.**

# ---- Navigation ----
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.AndroidViewModel
-keepclassmembers class * {
    <init>(android.app.Application);
}

# ---- SyncCoordinator ----
-keep class com.aile.takip.sync.** { *; }

# ---- BarcodeLookupHelper ----
-keep class com.aile.takip.utils.BarcodeLookupHelper { *; }
-keep class com.aile.takip.utils.BarcodeLookupHelper$ProductInfo { *; }

# ---- WorkManager ----
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---- Enum types ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Parcelable ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---- R8 full mode ----
-allowaccessmodification
-repackageclasses ''
