# - configuration --
SDK_ROOT=/Developer/Android
NDK_ROOT=$(SDK_ROOT)/ndk
DEVICE=HT95LKF03972

# - paths --
SYS_ROOT=$(NDK_ROOT)/build/platforms/android-4/arch-arm
TOOLCHAIN=$(NDK_ROOT)/build/prebuilt/darwin-x86/arm-eabi-4.2.1
ADB=$(SDK_ROOT)/tools/adb -s $(DEVICE)
DEVICE_DEST=/data/data/net.open-mesh.batman

# - toolchain --
CC=$(TOOLCHAIN)/bin/arm-eabi-gcc
LD=$(TOOLCHAIN)/bin/arm-eabi-gcc
STRIP=$(TOOLCHAIN)/bin/arm-eabi-strip
TARGET_CFLAGS=--sysroot=$(SYS_ROOT) -mandroid
TARGET_LDFLAGS=$(TARGET_CFLAGS)
CFLAGS=$(TARGET_CFLAGS) 
LDFLAGS=$(TARGET_LDFLAGS)

# - sources --
_SOURCES=batman.c originator.c schedule.c list-batman.c allocate.c bitarray.c \
				 hash.c profile.c ring_buffer.c hna.c 
_SOURCES+=linux/route.c linux/tun.c linux/kernel.c 
_SOURCES+=posix/init.c posix/posix.c posix/tunnel.c posix/unix_socket.c
# for dprintf
_SOURCES+=bsd/compat.c
SOURCES=$(_SOURCES:%=jni/%)
_HEADERS=batman.h originator.h schedule.h list-batman.h os.h allocate.h \
				 bitarray.h hash.h profile.h packet.h types.h ring_buffer.h hna.h
HEADERS=$(_HEADERS:%=jni/%)
OBJECTS=$(SOURCES:.c=.o)

CFLAGS+=-DANDROID -I./jni/
#CFLAGS+=-pedantic -Wall -W -O1 -g3 -std=gnu99
CFLAGS+=-std=gnu99
#CFLAGS+=-DDEBUG_MALLOC -DMEMORY_USAGE -DPROFILE_DATA -DREVISION_VERSION=\"\ rv1534\"
CFLAGS+=-DREVISION_VERSION=\"\ rv1534\"
LDFLAGS+=-static


batmand : $(OBJECTS) $(HEADERS) 
	$(LD) $(LDFLAGS) -o $@ $(OBJECTS) 
	$(STRIP) batmand
	mv batmand batmand-rv1543_armv6l

.c.o :
	$(CC) $(CFLAGS) -MD -c $< -o $@

push :
	$(ADB) shell rm $(DEVICE_DEST)/batmand
	$(ADB) push ./batmand $(DEVICE_DEST)


clean :
	find . -name *~  | xargs rm
	find . -name *.o | xargs rm
	find . -name *.d | xargs rm
	rm -rf *.o *~ libs batmand-*
