# Legado Engine Core - keep all public API
-keep class io.legado.engine.** { *; }
-keep interface io.legado.engine.** { *; }

# Keep Jsoup
-keeppackagenames org.jsoup.nodes

# Keep JsonPath
-keep class com.jayway.jsonpath.** { *; }

# Keep Rhino
-keep class org.mozilla.javascript.** { *; }
