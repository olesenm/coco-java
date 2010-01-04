#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.63])
AC_INIT([Coco/R Java], [20090104], [Moessenboeck AT ssw.jku.at], [coco-java])
AC_CONFIG_AUX_DIR(autoconf)
AC_CONFIG_SRCDIR([src/Coco.java])

AC_CONFIG_SRCDIR([src/Coco.java])

# Checks for programs.
AC_PATH_PROG(JAVAC, javac, no)
AC_PATH_PROG(JAVA, java, no)
AC_PATH_PROG(JAR, jar, no)

if test "$JAVAC" = "no"; then
    AC_MSG_ERROR([Cannot find "javac" in your PATH])
fi
if test "$JAVA" = "no"; then
    AC_MSG_ERROR([Cannot find "java" in your PATH])
fi
if test "$JAR" = "no"; then
    AC_MSG_ERROR([Cannot find "jar" in your PATH])
fi


AC_PROG_INSTALL

AC_CONFIG_FILES([Makefile:autoconf/Makefile.in src/Makefile])

AC_OUTPUT

# -----------------------------------------------------------------------------
