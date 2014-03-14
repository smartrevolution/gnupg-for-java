# to override this JAVA_HOME, use the `-e' flag to make, or call it like:
#   make JAVA_HOME=/opt/java
JAVA_HOME := $(shell readlink -f /usr/bin/javac | sed "s:/bin/javac::")

GNUPG_LIB := ../lib/libgnupg-for-java.so
JNISRC := GnuPGContext.c GnuPGData.c GnuPGGenkeyResult.c GnuPGKey.c GnuPGSignature.c
JNI_CLASSES := $(patsubst %.c, com.freiheit.gnupg.%, $(JNISRC))
JNI_HEADERS := $(patsubst %.c, com_freiheit_gnupg_%.h, $(JNISRC))
SOURCES := $(JNISRC) gpgmeutils.c
OBJECTS := $(SOURCES:.c=.o)

JAVAP := $(JAVA_HOME)/bin/javap
JAVAH := $(JAVA_HOME)/bin/javah
JAVA  := $(JAVA_HOME)/bin/java

CC := gcc
INSTALL := install -m 644

DEBUG := -g
CFLAGS := -Werror -Wall -Wno-deprecated-declarations -Wno-unused-but-set-variable \
	-fPIC $(shell gpgme-config --cflags)
CPPFLAGS = -D_REENTRANT -D_THREAD_SAFE -D_FILE_OFFSET_BITS=64 -DLARGEFILE_SOURCE=1 \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
	-I$(HOME)/code/gnupg/gpgme/src # this is needed for context.h

JAVA_BUILD := ../build/classes
LDFLAGS := $(shell gpgme-config --thread=pthread --libs)

$(GNUPG_LIB): $(OBJECTS)
	$(CC) $(DEBUG) -shared $(CFLAGS) $^ -o $@ $(LDFLAGS)

$(OBJECTS): $(SOURCES) $(JNI_HEADERS) gpgmeutils.h

%.o: %.c
	$(CC) $(DEBUG) $(CFLAGS) $(CPPFLAGS) -c $<

lib: $(GNUPG_LIB)

header: $(JNI_HEADERS)

$(JNI_HEADERS):
	$(JAVAH) -classpath $(JAVA_BUILD) -jni $(JNI_CLASSES)

mids:
	$(JAVAP) -classpath $(JAVA_BUILD) -s -p $(JNI_CLASSES)

clean:
	rm -f $(OBJECTS) *~
	rm -f $(GNUPG_LIB)

test:
	echo "JAVA_HOME: --$(JAVA_HOME)--"
	echo "JNISRC: $(JNISRC)"
	echo "JNI_CLASSES: $(JNI_CLASSES)"
	echo "JNI_HEADERS: $(JNI_HEADERS)"
	echo "SOURCES: $(SOURCES)"


.PHONY: lib header mids clean test
