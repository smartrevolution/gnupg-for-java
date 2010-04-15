# to use the JAVA_HOME var in the environment, use the `-e' flag to make
ifeq ($(JAVA_HOME),,)
JAVA_HOME := /www/home/tcadm/local/jdk-amd64
#JAVA_HOME := /opt/java/jdk1.5.0_06
#JAVA_HOME := /usr/lib/jvm/java-1.5.0-sun
#JAVA_HOME := /usr/local/jdk1.4.2
endif

# set this from the command line to enforce a particular OS-bitness
BITS := $(shell /bin/uname -m | grep ".*_[0-9]\+" | sed "s/.*_\([0-9]\+\)/\1/")

JAVAGNUPG_LIB := libjavagnupg
JNISRC := $(wildcard GnuPG*.c)
JNI_CLASSES := $(patsubst %.c, com.freiheit.gnupg.%, $(JNISRC))
JNI_HEADERS := $(patsubst %.c, com_freiheit_gnupg_%.h, $(JNISRC))
SOURCES := $(JNISRC) gpgmeutils.c
OBJECTS := $(SOURCES:.c=.o)

JAVAP := $(JAVA_HOME)/bin/javap
JAVAH := $(JAVA_HOME)/bin/javah
JAVA  := $(JAVA_HOME)/bin/java

CC := gcc
INSTALL := install -m 644

MARCH := 
ifneq ($(BITS),)
MARCH := -m$(BITS)
else
BITS := 32
endif
DEBUG := -g
CFLAGS := $(MARCH) -Werror -Wall -Wno-deprecated-declarations -fPIC $(shell gpgme-config --cflags)
CPPFLAGS := -D_REENTRANT -D_THREAD_SAFE -D_FILE_OFFSET_BITS=64 -DLARGEFILE_SOURCE=1 -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

C_IMPL := ./src/c
JAVA_BUILD := ../../build
LDFLAGS := $(shell gpgme-config --thread=pthread --libs)

lib: $(JAVAGNUPG_LIB).$(BITS).so

$(JAVAGNUPG_LIB).$(BITS).so: $(OBJECTS)
	$(CC) $(DEBUG) -shared $(CFLAGS) $^ -o $@ $(LDFLAGS)
	$(INSTALL) -m 755 -d $(JAVA_BUILD)/c
	$(INSTALL) $(JAVAGNUPG_LIB).$(BITS).so $(JAVA_BUILD)/c

header: $(JNI_HEADERS)

$(JNI_HEADERS):
	$(JAVAH) -classpath $(JAVA_BUILD) -jni $(JNI_CLASSES)

mids:
	$(JAVAP) -classpath $(JAVA_BUILD) -s -p $(JNI_CLASSES)

clean:
	@echo "NOT deleting the shared libraries. Use \`distclean' target to remove them."
	rm -f $(OBJECTS) *~

distclean:
	$(MAKE) clean
	rm -f $(JAVAGNUPG_LIB).*.so

%.o: %.c $(JNI_HEADERS) gpgmeutils.h
	$(CC) $(DEBUG) $(CFLAGS) $(CPPFLAGS) -c $<
