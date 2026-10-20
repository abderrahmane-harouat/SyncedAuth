# SyncedAuth keeps everything via default Android/Compose rules.
# javax.crypto is used at runtime for HMAC-SHA1.
-keep class javax.crypto.** { *; }
-keep class org.json.** { *; }
