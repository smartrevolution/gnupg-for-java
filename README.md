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


## Setup for Building

### Debian/Ubuntu/Mint/etc

    sudo apt-get install default-jdk make ant build-essential \
        libgpgme11-dev libgpg-error-dev


### Windows

* Install MinGW (for 32-bit): http://mingw.org/
* Install Gpg4win: http://gpg4win.org/download.html
** Signing Key Fingerprint: `61AC 3F5E E4BE 593C 13D6  8B1E 7CBD 620B EC70 B1B8`


### Mac OS X

You need to install GnuPG2 from one of a couple sources.  You can get
it from Homebrew, MacPorts, or Fink.  Or you can install
"GPGTools":https://gpgtools.org and then build gpgme from source.


## Building

To build the gnupg-for-java.jar in build/jar/, run this:

    ant clean jar

If you want the optional javadoc in build/docs/, run:

    ant javadoc

For the JNI headers, run:

    make -C jni/ headers


## Hacking Notes

Please conform to our code format standard. For C files use
./format-code.sh. For Java files, use Eclipse with the Android mode from the
Android project.  The default Eclipse formatting is usually close enough.

