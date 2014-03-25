
JNISRC := GnuPGContext.c GnuPGData.c GnuPGGenkeyResult.c GnuPGKey.c GnuPGSignature.c
JNI_CLASSES := $(patsubst %.c, com.freiheit.gnupg.%, $(JNISRC))
JNI_HEADERS := $(patsubst %.c, com_freiheit_gnupg_%.h, $(JNISRC))
SOURCES := $(JNISRC) gpgmeutils.c
OBJECTS := $(SOURCES:.c=.o)

JAVA_BUILD := ../build/classes

DEBUG := -g
ALL_CFLAGS := -Werror -Wall -Wno-deprecated-declarations -fPIC
ALL_CPPFLAGS = -D_REENTRANT -D_THREAD_SAFE -D_FILE_OFFSET_BITS=64 -DLARGEFILE_SOURCE=1 \
	-I"$(JAVA_HOME)/include"

UNAME := $(shell uname -s)
ifeq (MINGW,$(findstring MINGW,$(UNAME)))
  CC := mingw32-gcc
  ALL_CFLAGS +=  -mms-bitfields -Wl,--add-stdcall-alias
  ALL_CPPFLAGS += -I"$(JAVA_HOME)/include/win32" -I"$(PROGRAMFILES)/GNU/GnuPG/include"
  ALL_LDFLAGS :=  -s -shared -Wl,--enable-auto-import
  ALL_LIBS := -L"$(PROGRAMFILES)/GNU/GnuPG" -lgpgme-11
  JAVA_HOME := $(PROGRAMFILES)/Java/jdk1.7.0_51
  GNUPG_LIB := ../lib/gnupg-for-java.dll
else
  ifeq ($(UNAME),Darwin)
    CC := gcc
    ALL_CPPFLAGS += -I/usr/local/MacGPG2/include
    ALL_LDFLAGS := -dylib
    ALL_LIBS := -L/usr/local/MacGPG2/lib -framework MacGPGME
    JAVA_HOME := $(shell /usr/libexec/java_home \
	|| echo /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home)
    GNUPG_LIB := ../lib/libgnupg-for-java.dylib
  else
    CC := gcc
    ALL_CFLAGS +=  $(shell gpgme-config --cflags)
    ALL_CPPFLAGS += -I"$(JAVA_HOME)/include/linux"
    ALL_LDFLAGS := -shared
    ALL_LIBS := $(shell gpgme-config --thread=pthread --libs)
    GNUPG_LIB := ../lib/libgnupg-for-java.so
    # to override this JAVA_HOME, use the `-e' flag to make, or call it like:
    #   make JAVA_HOME=/opt/java
    JAVA_HOME := $(shell readlink -f /usr/bin/javac | sed "s:/bin/javac::")
  endif
endif

ALL_CFLAGS := $(ALL_CFLAGS) $(CFLAGS)
ALL_LDFLAGS := $(LDFLAGS) $(ALL_LDFLAGS)
ALL_LIBS := $(LIBS) $(ALL_LIBS)


JAVAP := $(JAVA_HOME)/bin/javap
JAVAH := $(JAVA_HOME)/bin/javah
JAVA  := $(JAVA_HOME)/bin/java

INSTALL := install -m 644


$(GNUPG_LIB): $(OBJECTS)
	$(CC) $(DEBUG) $(ALL_CFLAGS) $(ALL_LDFLAGS) $^ -o $@ $(ALL_LIBS)

$(OBJECTS): $(SOURCES) gpgmeutils.h

%.o: %.c
	$(CC) $(DEBUG) $(ALL_CFLAGS) $(ALL_CPPFLAGS) -c $<

lib: $(GNUPG_LIB)

headers: $(JNI_HEADERS)

$(JNI_HEADERS):
	$(JAVAH) -classpath $(JAVA_BUILD) -jni $(JNI_CLASSES)

mids:
	$(JAVAP) -classpath $(JAVA_BUILD) -s -p $(JNI_CLASSES)

clean:
	rm -f $(OBJECTS) *~
	rm -f $(GNUPG_LIB)
	rm -f $(JNI_HEADERS)

test:
	echo "JAVA_HOME: --$(JAVA_HOME)--"
	echo "JNISRC: $(JNISRC)"
	echo "JNI_CLASSES: $(JNI_CLASSES)"
	echo "JNI_HEADERS: $(JNI_HEADERS)"
	echo "SOURCES: $(SOURCES)"

TAGS:
	etags *.c *.h


.PHONY: lib header mids clean test
