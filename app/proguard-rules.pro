# Shrink and optimise, but keep class and method names: the size win comes from dropping unused
# AndroidX code, not from renaming, and readable stack traces matter more in a sideloaded app
# where crash reports arrive as screenshots.
-dontobfuscate
# Preserve line numbers so stack traces in crash screenshots are readable.
-keepattributes SourceFile,LineNumberTable
# Suppress R8's "missing class" notes for optional SDK features (e.g. Yandex TV permission).
-dontnote yandex.tv.**
