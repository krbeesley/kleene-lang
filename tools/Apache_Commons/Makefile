# This GNU makefile extracts Apache Commons files that we use.
# Written here by Phil Sours (not from Apache Commons).
#
# The Apache Commons is an Apache project focused on all aspects of
# reusable Java components.  http://commons.apache.org/
#
# We currently use the following Apache Commons components:
# Lang - Provides extra functionality for classes in java.lang.  

COMMONS_LANG_BASENAME=commons-lang3-3.1
COMMONS_LANG_ARCHIVE=$(COMMONS_LANG_BASENAME)-bin.tar.gz

# just unzip and detar the downloaded archive
$(COMMONS_LANG_BASENAME): $(COMMONS_LANG_ARCHIVE)
	tar xzf $(COMMONS_LANG_ARCHIVE)

link:
	ln -s ../../../kleene-lang-dependencies/$(COMMONS_LANG_BASENAME)-bin.tar.gz

old:
	@make --no-print-directory clean
	tar -xzf $(COMMONS_LANG_ARCHIVE) $(COMMONS_LANG_BASENAME)/$(COMMONS_LANG_BASENAME).jar
	@echo "(We can safely ignore the spurious message \"tar: A lone zero block at XXXX\".)"

# delete all directories
clean:
	rm -rf */

# delete link(s) to library package(s)
clean-all: clean
	find . -type l -delete

