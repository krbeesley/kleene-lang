# This GNU makefile extracts Jaxen files that we use.
# Written here by Phil Sours (not from Jaxen).
#
# Jaxen is an open source XPath library written in Java.
# http://jaxen.codehaus.org/

JAXEN_BASENAME=jaxen-1.1.6
JAXEN_ARCHIVE=$(JAXEN_BASENAME)-bin.tar.gz

$(JAXEN_BASENAME): $(JAXEN_ARCHIVE)
	tar xzf $(JAXEN_ARCHIVE)

link:
	ln -s ../../../kleene-lang-dependencies/$(JAXEN_BASENAME)-bin.tar.gz

old:
	@make --no-print-directory clean
	tar -xzf $(JAXEN_ARCHIVE) $(JAXEN_BASENAME)/$(JAXEN_BASENAME).jar
	@echo "(We can safely ignore the spurious message \"tar: A lone zero block at XXXX\".)"

clean:
	rm -rf */
