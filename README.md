About GnuPG for Java
=======================

This is a lib I wrote a couple of years ago. It is a native binding to the
gpgme library from Werner Koch. It uses JNI. 

gpgpme is the standard library for developing third-party apps on top of GnuPG.

I wrote it for 32-Bit Intel GNU/Linux platforms. Some of my colleagues added
64-Bit Intel GNU/Linux support. It should run on other Unix-platforms, too. But
this is not tested. Also I am not aware if it runs with the current version
of gpgme. It should be easy to add Windows support by compiling a DLL, adding
this to the jar and extend the loading mechanism to load a DLL on a Windows platform
instead of loading a .so lib.


