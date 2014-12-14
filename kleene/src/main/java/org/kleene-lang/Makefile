##	Makefile
##
##	The Kleene Programming Language

##   Copyright 2006-2012 SAP AG

##   Licensed under the Apache License, Version 2.0 (the "License");
##   you may not use this file except in compliance with the License.
##   You may obtain a copy of the License at
##
##       http://www.apache.org/licenses/LICENSE-2.0
##
##   Unless required by applicable law or agreed to in writing, software
##   distributed under the License is distributed on an "AS IS" BASIS,
##   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
##   See the License for the specific language governing permissions and
##   limitations under the License.

##   Authors: Phil Sours
##			  ken.beesley@sap.com (Kenneth R. Beesley)

##	Principal Makefile for the Kleene language

# This Makefile is intended to work on Linux and OS X
# See Makefile_nmake for Windows

WINPORT_DIR=$(CURDIR)/../../../../../../winport
include $(WINPORT_DIR)/os_type.mk   # set os_type to standard name for OS type

# In 2011 SAP declared the final version to 1.0.0.0, but in 2012, when
# Kleene was released as Open Source, KRB set the version back to
# 0.9.1.0 as a "beta".  There is still work to do before KRB considers
# Kleene to be 1.0.0.0.

VERSION=0.9.4.2
NAME=Kleene
RELEASE_BASENAME_NOVERSION=kleene-$(os_type)
RELEASE_BASENAME=$(RELEASE_BASENAME_NOVERSION)-$(VERSION)

KLEENE_LANG_DIR=../../../../../..
RELEASE_DIRS=$(KLEENE_LANG_DIR)/dotkleene lib

# the tools/ directory has subdirectories for
# Java .jar libraries used by Kleene, and the OpenFst and ICU4C
# Libraries (C++).  Some of the Java libraries are distributed
# as plain .jar files, others are gzipped into .jar.gz files.

TOOLSDIR=$(CURDIR)/../../../../../../tools

COMMONS_MAKEDIR=$(TOOLSDIR)/Apache_Commons
COMMONS_LANG_BASENAME=commons-lang3-3.1
COMMONS_LANG_DIR=$(COMMONS_MAKEDIR)/$(COMMONS_LANG_BASENAME)
COMMONS_LANG_JAR=$(COMMONS_LANG_BASENAME).jar

DOM4J_MAKEDIR=$(TOOLSDIR)/dom4j
DOM4J_BASENAME=dom4j-1.6.1
DOM4J_DIR=$(DOM4J_MAKEDIR)/$(DOM4J_BASENAME)
DOM4J_JAR=$(DOM4J_BASENAME).jar

ICU4J_BASENAME=icu4j-51_1
ICU4J_DIR=$(TOOLSDIR)/ICU4J
ICU4J_JAR=$(ICU4J_BASENAME).jar

JAVACC_MAKEDIR=$(TOOLSDIR)/JavaCC
JAVACC_BASENAME=javacc-5.0
JAVACC_BINDIR=$(JAVACC_MAKEDIR)/$(JAVACC_BASENAME)/bin

JAXEN_MAKEDIR=$(TOOLSDIR)/Jaxen
JAXEN_BASENAME=jaxen-1.1.6
JAXEN_DIR=$(JAXEN_MAKEDIR)/$(JAXEN_BASENAME)
JAXEN_JAR=$(JAXEN_BASENAME).jar

ROME_BASENAME=rome-1.0
ROME_DIR=$(TOOLSDIR)/ROME
ROME_JAR=$(ROME_BASENAME).jar

# C++ libraries (OpenFst and ICU4C)
#
OPENFST_MAKEDIR=$(TOOLSDIR)/OpenFst
OPENFST_BASENAME=openfst-1.3.3
OPENFST_DIR=$(OPENFST_MAKEDIR)/$(OPENFST_BASENAME)
OPENFST_LIBDIR=$(OPENFST_DIR)/src/lib
OPENFST_BINDIR=$(OPENFST_DIR)/src/bin
OPENFST_INCDIR=$(OPENFST_DIR)/src/include
OPENFST_INCS=$(OPENFST_INCDIR)/fst/*.h

# name of the dynamic JNI library, including the JNI
# "bridge" code, which links in the OpenFst library.
LIBNAME=kleeneopenfst

CC=g++
OPT=-O2

######################### LINUX ############################

ifeq ($(os_type),linux)
 DOT=/usr/bin/dot
 JNILIBTYPE=so
 DYLIBTYPE=so
 ICU4C_NAME_VERSION=51.1
 ICU4C_MAKEDIR=$(TOOLSDIR)/ICU4C
 ICU4C_DIR=$(ICU4C_MAKEDIR)/icu
 ICU4C_SOURCE_DIR=$(ICU4C_DIR)/source
 ICU4C_INCDIR=$(ICU4C_SOURCE_DIR)/common
 ICU4C_LIBDIR=$(ICU4C_SOURCE_DIR)/lib
 # on Linux, at least, ICU4C 51.1 builds a library named
 # libicudata.so.51.1, with the DYLIBTYPE followed by the version
 # number; it also creates symbolic links named libicudata.so and
 # libicudata.so.51, both pointing to libicudata.so.51.1
 ICU4CDT_BASENAME=libicudata
 ICU4CDT_DLL=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME).$(DYLIBTYPE)
 ICU4CDT_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME).$(DYLIBTYPE)*
 ICU4CIN_BASENAME=libicui18n
 ICU4CIN_DLL=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME).$(DYLIBTYPE)
 ICU4CIN_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME).$(DYLIBTYPE)*
 ICU4CUC_BASENAME=libicuuc
 ICU4CUC_DLL=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME).$(DYLIBTYPE)
 ICU4CUC_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME).$(DYLIBTYPE)*
 CFLAGS=$(OPT)  # for 64-bit Linux, add -fPIC
 CINC=-I$(OPENFST_INCDIR) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(ICU4C_INCDIR)
 LDLFLAGS=-Wl,-L$(OPENFST_LIBDIR)/.libs,-L$(ICU4C_LIBDIR)
 #LDRFLAGS=-Wl,-rpath,$(OPENFST_LIBDIR)/.libs
 LDRFLAGS=
 LDFLAGS=$(LDLFLAGS) $(LDRFLAGS)
 SOFLAGS=-shared
 FSTLIB_BASENAME=libfst
 FSTLIB=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)
 # FSTLIB_AND_LINKS is intended to cover libfst.so and 
 # libfst.so.1 and libfst.so.1.2, etc.  In Linux the numbers, if any, come
 # after the .so
 FSTLIB_AND_LINKS=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)*
 # flags to copy pointers as pointers (differ on Linux and OS X at least)
 CPFLAGS=-P
 # for the command-line link args, strip the "lib" off the names
 LIBS=-lfst -lm -lpthread -ldl \
      -l$(subst lib,,$(ICU4CDT_BASENAME)) \
      -l$(subst lib,,$(ICU4CIN_BASENAME)) \
      -l$(subst lib,,$(ICU4CUC_BASENAME))
 FRAMEWORK=
 LIBFULLNAME=lib$(LIBNAME).$(JNILIBTYPE)
 CLASSPATHFLAG=-classpath "lib/$(COMMONS_LANG_JAR):lib/$(DOM4J_JAR):lib/$(ICU4J_JAR):lib/$(JAXEN_JAR):lib/$(ROME_JAR):."
 # shared libraries on Linux have .so followed by major.minor version
 # numbers, e.g.  libname.so and libname.so.51 are links to name.so.51.1
 RELEASE_FILES=$(FSTLIB_BASENAME).$(DYLIBTYPE)* \
 			  $(ICU4CDT_BASENAME).$(DYLIBTYPE)* \
              $(ICU4CIN_BASENAME).$(DYLIBTYPE)* \
              $(ICU4CUC_BASENAME).$(DYLIBTYPE)* \
			  $(NAME).jar \
			  $(LIBFULLNAME) \
              $(KLEENE_LANG_DIR)/LICENSE \
			  $(KLEENE_LANG_DIR)/NOTICE \
			  $(KLEENE_LANG_DIR)/README \
			  $(KLEENE_LANG_DIR)/README.git \
			  $(KLEENE_LANG_DIR)/README.build \
			  $(KLEENE_LANG_DIR)/README.install \
			  $(KLEENE_LANG_DIR)/README.version
endif

ifeq ($(os_type),mac)
 #DOT=/Applications/Graphviz.app/Contents/MacOS/dot
 DOT=/usr/local/bin/dot
 JNILIBTYPE=jnilib
 DYLIBTYPE=dylib
 ICU4C_NAME_VERSION=51.1
 ICU4C_MAKEDIR=$(TOOLSDIR)/ICU4C
 ICU4C_DIR=$(ICU4C_MAKEDIR)/icu
 ICU4C_SOURCE_DIR=$(ICU4C_DIR)/source
 ICU4C_INCDIR=$(ICU4C_SOURCE_DIR)/common
 ICU4C_LIBDIR=$(ICU4C_SOURCE_DIR)/lib
 ICU4CDT_BASENAME=libicudata
 ICU4CDT_DLL=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME).$(DYLIBTYPE)
 ICU4CDT_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME)*.$(DYLIBTYPE)
 ICU4CIN_BASENAME=libicui18n
 ICU4CIN_DLL=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME).$(DYLIBTYPE)
 ICU4CIN_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME)*.$(DYLIBTYPE)
 ICU4CUC_BASENAME=libicuuc
 ICU4CUC_DLL=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME).$(DYLIBTYPE)
 ICU4CUC_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME)*.$(DYLIBTYPE)
 CFLAGS=$(OPT)
 CINC=-I$(OPENFST_INCDIR) -I/System/Library/Frameworks/JavaVM.framework/Headers -I$(ICU4C_INCDIR)
 LDLFLAGS=-Wl,-L$(OPENFST_LIBDIR)/.libs,-L$(ICU4C_LIBDIR)
 LDRFLAGS=
 LDFLAGS=$(LDLFLAGS) $(LDRFLAGS)
 SOFLAGS=-dynamiclib -flat_namespace -undefined suppress
 FSTLIB_BASENAME=libfst
 FSTLIB=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)
 # FSTLIB_AND_LINKS is intended to cover libfst.dylib and libfst.0.dylib
 FSTLIB_AND_LINKS=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME)*.$(DYLIBTYPE)
 # flags to copy links as links (different on OS X and Linux at least)
 CPFLAGS=-R -P
 LIBS=-lfst \
      -l$(subst lib,,$(ICU4CDT_BASENAME)) \
      -l$(subst lib,,$(ICU4CIN_BASENAME)) \
      -l$(subst lib,,$(ICU4CUC_BASENAME))
 FRAMEWORK=-framework JavaVM
 LIBFULLNAME=lib$(LIBNAME).$(JNILIBTYPE)
 CLASSPATHFLAG=-classpath "lib/$(COMMONS_LANG_JAR):lib/$(DOM4J_JAR):lib/$(ICU4J_JAR):lib/$(JAXEN_JAR):lib/$(ROME_JAR):."
 # shared library names on OS X have the version numbers in the middle, e.g.
 # libfst.dylib is a link to libfst.0.dylib
 # libicudata.dylib and libicudata.51.dylib are links to
 # libicudata.51.1.dylib
 RELEASE_FILES=$(FSTLIB_BASENAME)*.$(DYLIBTYPE) \
			  $(ICU4CDT_BASENAME)*.$(DYLIBTYPE) \
              $(ICU4CIN_BASENAME)*.$(DYLIBTYPE) \
              $(ICU4CUC_BASENAME)*.$(DYLIBTYPE) \
			  $(NAME).jar \
			  $(LIBFULLNAME) \
              $(KLEENE_LANG_DIR)/LICENSE \
			  $(KLEENE_LANG_DIR)/NOTICE \
			  $(KLEENE_LANG_DIR)/README \
			  $(KLEENE_LANG_DIR)/README.git \
			  $(KLEENE_LANG_DIR)/README.build \
			  $(KLEENE_LANG_DIR)/README.install \
			  $(KLEENE_LANG_DIR)/README.version
endif

ifeq ($(os_type),cygwin)
 DOT="C:/Program Files/Graphviz 2.21/bin/dot"
 JNILIBTYPE=dll
 DYLIBTYPE=dll
 ICU4C_OS_VERSION=    # TBD
 ICU4C_CPP_VERSION=   # TBD
 ICU4C_NAME_VERSION=  # TBD
 ICU4C_MAKEDIR=		  # TBD
 ICU4C_DIR=           # TBD
 ICU4C_INCDIR=        # TBD
 ICU4C_LIBDIR=        # TBD
 ICU4CDT_BASENAME=libicudata  # TBD
 ICU4CDT_DLL=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME).$(DYLIBTYPE)
 # check this for cygwin
 ICU4CDT_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CDT_BASENAME).$(DYLIBTYPE)*
 ICU4CIN_BASENAME=libicui18n  # TBD
 ICU4CIN_DLL=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME).$(DYLIBTYPE)
 # check this for cygwin
 ICU4CIN_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CIN_BASENAME).$(DYLIBTYPE)*
 ICU4CUC_BASENAME=libicuuc    # TBD
 ICU4CUC_DLL=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME).$(DYLIBTYPE)
 # check this for cygwin
 ICU4CUC_DLL_AND_LINKS=$(ICU4C_LIBDIR)/$(ICU4CUC_BASENAME).$(DYLIBTYPE)*
 CFLAGS=$(OPT) -frepo -mno-cygwin -D__int64=long\ long
 CINC=-I$(OPENFST_INCDIR) -I$(WINPORT_DIR) -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" -I$(ICU4C_INCDIR)
 LDLFLAGS=
 LDRFLAGS=
 LDFLAGS=$(LDLFLAGS) $(LDRFLAGS)
 SOFLAGS=-shared -mno-cygwin -Wl,--kill-at -Wl,--enable-auto-import
 FSTLIB_BASENAME=libfst
 FSTLIB=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)
 # FSTLIB_AND_LINKS is intended to cover main "real" library file and links to it; see
 # notes below for RELEASE_FILES.  May be wrong for cygwin.  Check it.
 FSTLIB_AND_LINKS=$(OPENFST_LIBDIR)/.libs/$(FSTLIB_BASENAME).$(DYLIBTYPE)*
 # flags to copy links as links (check this for windows)
 CPFLAGS=-P
 LIBS=$(FSTLIB:$(DYLIBTYPE)=dll.a) \
      $(ICU4CDT_DLL:$(DYLIBTYPE)=dll.a) \
      $(ICU4CIN_DLL:$(DYLIBTYPE)=dll.a) \
      $(ICU4CUC_DLL:$(DYLIBTYPE)=dll.a)
 LIBFULLNAME=$(LIBNAME).$(JNILIBTYPE)
 CLASSPATHFLAG=-classpath "lib\$(COMMONS_LANG_JAR);lib\$(DOM4J_JAR);lib\$(ICU4J_JAR);lib\$(JAXEN_JAR);lib\$(ROME_JAR);."
 # here the use of wildcard * may be wrong for Windows; see notes for linux
 # and macos above
 RELEASE_FILES=$(FSTLIB_BASENAME).$(DYLIBTYPE)* \
              $(ICU4CDT_BASENAME).$(DYLIBTYPE)* \
              $(ICU4CIN_BASENAME).$(DYLIBTYPE)* \
              $(ICU4CUC_BASENAME).$(DYLIBTYPE)* \
			  $(NAME).jar \
			  $(LIBFULLNAME) \
              $(KLEENE_LANG_DIR)/LICENSE \
			  $(KLEENE_LANG_DIR)/NOTICE \
			  $(KLEENE_LANG_DIR)/README \
			  $(KLEENE_LANG_DIR)/README.git \
			  $(KLEENE_LANG_DIR)/README.build \
			  $(KLEENE_LANG_DIR)/README.install_windows \
			  $(KLEENE_LANG_DIR)/README.version
endif

# The default is to build the Kleene jar file (which requires library, etc.).
default: externals jar

###############################################################################
# Required external items.
###############################################################################

# External directories to make.
# These commands are always executed, to make sure the dirs are up-to-date.
# Note anything that depends on a "phony" target will also always be executed.
# N.B. ICU4J and ROME are distributed as .jar files, so there's nothing to make.
DIRS_TO_MAKE:
	@echo ""
	@echo "Building required external items..."
	( cd $(OPENFST_MAKEDIR) ; $(MAKE) )
	( cd $(ICU4C_MAKEDIR) ; $(MAKE) )
	( cd $(COMMONS_MAKEDIR) ; $(MAKE) )
	( cd $(DOM4J_MAKEDIR) ; $(MAKE) )
	( cd $(JAVACC_MAKEDIR) ; $(MAKE) )
	( cd $(JAXEN_MAKEDIR) ; $(MAKE) )

# Files to copy in.
# These commands are executed if needed (as normal).
# For locally-built & p4 files, we can copy sym links as sym links (using cp -P).
# On OS X, it seems like we need cp -R -P to do that.
# Now use $(CPFLAGS) to get -P for Linux and -R -P for OS X
EXTERNAL_FILES=$(FSTLIB_BASENAME).$(DYLIBTYPE) \
               $(ICU4CDT_BASENAME).$(DYLIBTYPE) \
               $(ICU4CIN_BASENAME).$(DYLIBTYPE) \
               $(ICU4CUC_BASENAME).$(DYLIBTYPE) \
               lib/$(COMMONS_LANG_JAR) \
			   lib/$(DOM4J_JAR) \
               lib/$(ICU4J_JAR) \
			   lib/$(JAXEN_JAR) \
			   lib/$(ROME_JAR)
# $? is the names of all the prerequisites that are newer than the
# target
# $(@D) is the directory part of the file name of the target, with
# the trailing slash removed.
$(FSTLIB_BASENAME).$(DYLIBTYPE): $(FSTLIB)
	#cp $(CPFLAGS) $?* $(@D)/
	cp $(CPFLAGS) $(FSTLIB_AND_LINKS) $(@D)/
$(ICU4CDT_BASENAME).$(DYLIBTYPE): $(ICU4CDT_DLL)
	#cp $(CPFLAGS) $?* $(@D)/
	cp $(CPFLAGS) $(ICU4CDT_DLL_AND_LINKS) $(@D)/
$(ICU4CIN_BASENAME).$(DYLIBTYPE): $(ICU4CIN_DLL)
	#cp $(CPFLAGS) $?* $(@D)/
	cp $(CPFLAGS) $(ICU4CIN_DLL_AND_LINKS) $(@D)/
$(ICU4CUC_BASENAME).$(DYLIBTYPE): $(ICU4CUC_DLL)
	#cp $(CPFLAGS) $?* $(@D)/
	cp $(CPFLAGS) $(ICU4CUC_DLL_AND_LINKS) $(@D)/
lib:
	mkdir lib
lib/$(COMMONS_LANG_JAR): $(COMMONS_LANG_DIR)/$(COMMONS_LANG_JAR)
	cp $? $@
lib/$(DOM4J_JAR): $(DOM4J_DIR)/$(DOM4J_JAR)
	cp $? $@
lib/$(ICU4J_JAR): $(ICU4J_DIR)/$(ICU4J_JAR)
	cp $? $@
lib/$(JAXEN_JAR): $(JAXEN_DIR)/$(JAXEN_JAR)
	cp $? $@
lib/$(ROME_JAR): $(ROME_DIR)/$(ROME_JAR)
	cp $? $@

externals: DIRS_TO_MAKE lib $(EXTERNAL_FILES)

###############################################################################
# Compile the Kleene grammar into Java code from our supplied .jjt file.
###############################################################################

# Build Kleene .jj file from our supplied .jjt file.
# Copy in our hand-modified .java files -- jjtree will skip overwriting them
# if they already exist.
# Since jjtree doesn't delete target on failure, we append commands to manually handle it.

JJTREE_DIR=$(CURDIR)/../../../jjtree/org/kleene-lang

$(NAME).jj: $(JJTREE_DIR)/$(NAME).jjt
	@echo ""
	@echo "Compiling the Kleene grammar into Java code from our supplied .jjt file..."
	cp -f hand-modified/* .
# HACK: jjtree 4.2 overwrites this hand-modified file!
	chmod a+w JJTKleeneState.java
# compile the .jjt file; if not successful, delete the .jj file and exit
	$(JAVACC_BINDIR)/jjtree -JDK_VERSION="1.5" $(JJTREE_DIR)/$(NAME).jjt || (rm $@ ; exit 1)
	cp JJTKleeneState.java JJTKleeneState.java.generated
# HACK: jjtree 4.2 overwrites this hand-modified file!
	cp hand-modified/JJTKleeneState.java .

# Building Kleene java file from its .jj file.
KleeneVisitor.java: $(NAME).java
$(NAME).java: $(NAME).jj
	$(JAVACC_BINDIR)/javacc -JDK_VERSION="1.5" $(NAME).jj
	sed -e 's/protected JJTKleeneState jjtree/protected static JJTKleeneState jjtree/' Kleene.java > Kleene.temp
	mv Kleene.temp Kleene.java

###############################################################################
# Build all the Kleene class files from their java files.
###############################################################################

# Build all the Kleene class files from their java files.
# Note that javac silently compiles all referenced java source files as
# needed, so it's best to just consciously compile all of them at once.
class: $(NAME).class
$(NAME).class OpenFstLibraryWrapper.class: *.java
	@echo ""
	@echo "Building all the Kleene class files from their java files..."
	javac $(CLASSPATHFLAG) *.java

###############################################################################
# Build the JNI bridge library so we can use OpenFst from Java.
###############################################################################

# Generate OpenFstLibraryWrapper.h from its class file.
.PRECIOUS: OpenFstLibraryWrapper.h
OpenFstLibraryWrapper.h: KleeneVisitor.java OpenFstLibraryWrapper.class
	@echo ""
	@echo "Building the JNI bridge library so we can use OpenFst from Java..."
	javah $(CLASSPATHFLAG) -jni OpenFstLibraryWrapper

# Compile library .cc file, which includes OpenFstLibraryWrapper.h.
# # KRB 2012-06-13 Linux make wouldn't accept $(OPENFST_INCS) as a
#  	prerequisite for $(LIBNAME).o (it seemed to reference perfectly
#  	a whole directory of include files, see definition above.  Perhaps
#  	it didn't like the *h on the end?) It complained that there was
#  	no rule to generate /the/big/path/*.h.
#  	In any case, everything worked when I took it out.

CPP_DIR=$(CURDIR)/../../../cpp/org/kleene-lang

#$(LIBNAME).o: $(FSTLIB) $(LIBNAME).cc $(OPENFST_INCS) OpenFstLibraryWrapper.h
$(LIBNAME).o: $(FSTLIB) $(CPP_DIR)/$(LIBNAME).cc  OpenFstLibraryWrapper.h
	@echo ""
	@echo "Building kleeneopenfst.o"
	@echo ""
	#$(CC) $(CFLAGS) -o $@ -c $(CINC) $*.cc
	$(CC) $(CFLAGS) -o $@ -c $(CINC) -I$(CURDIR) $(CPP_DIR)/$(LIBNAME).cc

# Build Kleene JNI library, which uses OpenFst fst dynamic library.
library sharedlib sharedlibrary: $(LIBFULLNAME)
$(LIBFULLNAME): $(LIBNAME).o
	@echo ""
	@echo "Building the Kleene JNI library"
	@echo ""
	$(CC) $(SOFLAGS) $(LDFLAGS) -o $@ $(LIBNAME).o $(LIBS) $(FRAMEWORK)

###############################################################################
# Build the Kleene jar file, which packages up most of what's needed to run Kleene.
###############################################################################

jar: $(NAME).jar
$(NAME).jar: $(LIBFULLNAME) $(NAME).class images
	@echo ""
	@echo "Building the Kleene jar file..."
# Dynamically build manifest file indicating our required .jar files.
	@echo>$(NAME).mf  Manifest-Version: 1.0
	@echo>>$(NAME).mf Main-Class: $(NAME)
	@echo>>$(NAME).mf Class-Path: lib/$(COMMONS_LANG_JAR) lib/$(DOM4J_JAR) lib/$(ICU4J_JAR) lib/$(JAXEN_JAR) lib/$(ROME_JAR)
# Use $(JAVA_HOME)/bin/jar since Cygwin has its own version of "jar".
	"$(JAVA_HOME)/bin/jar" cmf $(NAME).mf $(NAME).jar *.class images
	@echo ""
	@echo "To run the executable jar:  java -jar $(NAME).jar"

###############################################################################
# Additional items for managing and working with Kleene.
###############################################################################

# Run the Kleene environment.
run: jar
	java -jar $(NAME).jar

# Run Kleene, forcing UTF-8 system encoding (Typically for OS X,
# which reports MacRoman when the locale is UTF-8)
run8: jar
	java -Dfile.encoding=UTF-8 -jar $(NAME).jar

runmac: jar
	java -Dfile.encoding=UTF-8 -jar $(NAME).jar


# Generate documentation for the BNF grammar of Kleene.
bnf: $(NAME).html
doc: $(NAME).html
$(NAME).html: $(NAME).jj
	$(JAVACC_BINDIR)/jjdoc $(NAME).jj

# Off-line utility to display the last network developed in the Kleene environment.
last:
	$(OPENFST_BINDIR)/fstprint last.binary last.txt
	cat last.txt
# $(OPENFST_BINDIR)/fstdraw last.binary last.dot
# last.dot is produced for each ASTnet_assignment (alt. -Tpdf)
draw:
	$(DOT) -Tps last.dot > last.ps
	open last.ps
#draw:
#	$(DOT) -Tpdf last.dot > last.pdf
#	open last.pdf

# Build the binary release file (.tar.gz, the "release package") for Kleene.
# We set up a staging area to avoid having read-only files in the archive and
# to place all the files in a $(RELEASE_BASENAME) subdirectory in the archive.
# Note that we create an empty dotkleene/tmp directory needed by Kleene.
# N.B. the release file includes lib/, which contains .jar files that Kleene
# depends on.  (You can't package a .jar file inside another .jar file, like
# an executable .jar -- the Kleene.mf manifest file indicates where Kleene.jar
# can find them at run-time.)

gz: release
release: jar
	mkdir $(RELEASE_BASENAME)
	cp -R $(RELEASE_DIRS) $(RELEASE_BASENAME)/
	cp $(CPFLAGS) $(RELEASE_FILES) $(RELEASE_BASENAME)/
	chmod -R u+w $(RELEASE_BASENAME)/
	rm -rf $(RELEASE_BASENAME)/dotkleene/tmp/
	mkdir $(RELEASE_BASENAME)/dotkleene/tmp
	tar cvf $(RELEASE_BASENAME).tar $(RELEASE_BASENAME)
	gzip $(RELEASE_BASENAME).tar

# clean
# Delete all the temporary java files and $(NAME).java.
# (The originals of our hand-modified files are kept in the hand-modified
# subdirectory so they won't be deleted here.)
clean:
	rm -f $(NAME).jj $(NAME).java *.class *~ $(NAME).html
	rm -f AST*.java JJTKleeneState.java
	rm -f KleeneTreeConstants.java KleeneVisitor.java
	rm -f Node.java SimpleNode.java
	rm -f KleeneConstants.java KleeneTokenManager.java
	rm -f ParseException.java Token.java TokenMgrError.java JavaCharStream.java
	rm -f $(LIBFULLNAME) $(LIBNAME).o
	rm -f OpenFstLibraryWrapper.h
	rm -f $(NAME).jar $(NAME).mf
	rm -f $(EXTERNAL_FILES)
	rm -rf lib/
	rm -rf $(RELEASE_BASENAME_NOVERSION)*/
	rm -f $(RELEASE_BASENAME_NOVERSION)*.tar*
	rm -f *.o *.rpo *.lib *.a *.$(DYLIBTYPE)* *~
	rm -f *.generated

# Clean-all to clean the tools libraries as well.
# Prepare to re-unzip the Java libraries distributed as .tar.gz or .tgz.
# Prepare to recompile the C++ OpenFst and ICU4C libaries
# from scratch, e.g. for a different platform
clean-all: clean
	( cd $(COMMONS_MAKEDIR) ; $(MAKE) clean )
	( cd $(JAXEN_MAKEDIR)   ; $(MAKE) clean )
	( cd $(DOM4J_MAKEDIR)   ; $(MAKE) clean )
	( cd $(JAVACC_MAKEDIR)  ; $(MAKE) clean )
	( cd $(OPENFST_MAKEDIR) ; $(MAKE) clean-all )
	( cd $(ICU4C_MAKEDIR)   ; $(MAKE) clean-all )
