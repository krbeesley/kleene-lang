# This GNU makefile extracts and compiles ICU4C
# Written here by Ken Beesley (not from Apache Commons).
#
# See http://site.icu-project.org
#
## unpack the downloaded .tgz file manually
## creates an icu/ directory, which contains source/,
## which contains configure (which creates a platform-compatible
## Makefile)

# See two goals below:  call configure and make

# the winport script interrogates the operating system
# sets variable os_type to a standardized name for the OS type
WINPORT_DIR=$(CURDIR)/../../winport
include $(WINPORT_DIR)/os_type.mk

# the basename of the icu library
ICU4C_BASENAME=icu4c-49_1_2
ICU4C_ARCHIVE=$(ICU4C_BASENAME)-src.tgz

# the compiled libraries get stored in ./icu/src/lib
ICU4C_DIR=icu
ICU4C_SOURCE_DIR=$(ICU4C_DIR)/source
ICU4C_LIB_DIR=$(ICU4C_SOURCE_DIR)/lib

# a number of libraries are compiled, including libicudata
ICU4C_LIB_BASENAME=libicudata
# this is the naming-convention format for the compiled libraries
ICU4C_LIB_VERSION=49.1.2

# different operating systems use different extensions
# for the compiled libraries
ifeq ($(os_type),linux)
 DYLIBTYPE=so
endif
ifeq ($(os_type),mac)
 DYLIBTYPE=dylib
endif
ifeq ($(os_type),cygwin)
 DYLIBTYPE=dll
endif

ICU4CLIB=$(ICU4C_LIB_DIR)/$(ICU4C_LIB_DIR).$(DYLIBTYPE).$(ICU_LIB_VERSION)

#default goal
target: $(ICU4CLIB)

# untarring the downloaded .tgz archive creates the icu/ dir
# including icu/source/configure

$(ICU4C_SOURCE_DIR)/configure: $(ICU4C_ARCHIVE)
	tar xzf $(ICU4C_ARCHIVE)
	touch $(ICU4C_SOURCE_DIR)/configure

# call configure to create a Makefile
$(ICU4C_SOURCE_DIR)/Makefile: $(ICU4C_SOURCE_DIR)/configure
	(cd $(ICU4C_SOURCE_DIR); ./configure )

# if the Makefile already exists, call it
$(ICU4CLIB): $(ICU4C_SOURCE_DIR)/Makefile
	( cd $(ICU4C_SOURCE_DIR) ; $(MAKE) )

# clean preparatory to remaking on the same platform
clean:
	( cd $(ICU4C_SOURC_DIR) ; $(MAKE) clean )

# delete the whole icu/ directory, preparatory to starting over
# from scratch, perhaps on a different platform
clean-all:
	rm -rf */
