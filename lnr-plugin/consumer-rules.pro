# Keep plugin entry point
-keep class io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin { *; }
-keep class io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin$* { *; }

# Keep engine classes
-keep class io.legado.engine.** { *; }

# Keep Gson serialization
-keepclassmembers class io.github.dmzz_yyhyy.lnrplugin.source.** { *; }
