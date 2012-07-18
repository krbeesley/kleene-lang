# This GNU makefile builds OpenFst bin files that we use.
# Written here by Phil Sours (not from OpenFst).
# Our version of OpenFst is already extracted and modified to support Windows.
#
# OpenFst is a free, open-source library for constructing and manipulating
# weighted finite-state transducers (FSTs).  http://www.openfst.org/
#

WINPORT_DIR=$(CURDIR)/../../winport
include $(WINPORT_DIR)/os_type.mk   # set os_type to standard name for OS type

OPENFST_BASENAME=openfst-1.3.2
OPENFST_ARCHIVE=$(OPENFST_BASENAME).tar.gz

OPENFST_LIBDIR=$(OPENFST_BASENAME)/src/lib

FSTLIB_BASENAME=libfst

ifeq ($(os_type),linux)
 DYLIBTYPE=so
endif
ifeq ($(os_type),mac)
 DYLIBTYPE=dylib
endif
ifeq ($(os_type),cygwin)
 DYLIBTYPE=dll
endif

FSTLIB=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)

default: $(FSTLIB)

# openfst is distributed as a tar.gz archive;
# unzip it, yielding the $(OPENFST_BASENAME) directory and all its contents,
# including $(OPENFST_BASENAME)/configure.ac
# touch configure.ac so that its date is later than the archive's
$(OPENFST_BASENAME)/configure.ac: $(OPENFST_ARCHIVE)
	tar xzf $(OPENFST_ARCHIVE)
	touch $(OPENFST_BASENAME)/configure.ac
	touch $(OPENFST_BASENAME)/src/include/fst/*.h

# call the configure script to create the platform-specific Makefile
$(OPENFST_BASENAME)/Makefile: $(OPENFST_BASENAME)/configure.ac
	( cd $(OPENFST_BASENAME) ; ./configure )

# call the platform-specific Makefile
$(FSTLIB): $(OPENFST_BASENAME)/Makefile
	( cd $(OPENFST_BASENAME) ; $(MAKE) )

# this clean should work when continuing to development on the same platform
clean:
	( cd $(OPENFST_BASENAME) ; $(MAKE) clean )

# super clean to start all over again, perhaps on a separate platform
clean-all:
	rm -rf $(OPENFST_BASENAME)
