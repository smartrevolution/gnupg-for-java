GnuPG for Java
==============

GnuPG for Java is a Java wrapper for the gpgme (GnuPG Made Easy) library for
working with the GnuPG encryption suite. It is a native binding to the gpgme
using JNI.  It requires gpgme 1.4.3 or newer, and expects to work with GnuPG
2.x as the "engine" in gpgme.  With GnuPG 2.x, gpg-agent will handle prompting
the user for the passphrase, as well as passphrase caching.

gpgpme is the standard library for developing third-party apps on top of GnuPG.

Stefan Richter originally wrote it for 32-Bit Intel GNU/Linux platforms. Some
of his colleagues added 64-Bit Intel GNU/Linux support. It should build and
run on other UNIX platforms too, but that has not been tested.  The Guardian
Project then ported it to GnuPG 2.x and Android as the basis of Gnu Privacy
Guard for Android, and added lots of features and fixed lots of bugs in the
process.

It should be easy to add Windows support by compiling a DLL, adding this to
the jar and extend the loading mechanism to load a DLL on a Windows platform
instead of loading a .so lib.


## Hacking Notes

Please conform to our code format standard. For C files use format-code.sh.

