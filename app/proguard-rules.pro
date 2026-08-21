# What R8 must not remove, and why.
#
# The list is short on purpose. Room, Hilt and Compose ship their own consumer rules, so
# nothing here repeats them — a keep rule copied from a blog post is a rule nobody can
# delete later, because nobody knows what it was protecting.
#
# Anything added here needs a sentence saying which crash it prevents, and a release build
# that shows the crash without it (`docs/ANDROID_STANDARDS.md` §4).

# Line numbers in a crash report, and the mapping to read them with. Without this a stack
# trace from the Play console names obfuscated methods with no lines, which is a stack trace
# that cannot be acted on. The source file name itself is hidden, which is what
# SourceFile/LineNumberTable plus `-renamesourcefileattribute` buys back.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
