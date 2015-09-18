# This GNU makefile extracts dom4j files that we use.
# Written here by Phil Sours (not from dom4j).
#
# dom4j is an open source Java library for XML, XPath, and XSLT.
# http://www.dom4j.org/

DOM4J_BASENAME=dom4j-1.6.1
DOM4J_ARCHIVE=$(DOM4J_BASENAME).tar.gz

$(DOM4J_BASENAME): $(DOM4J_ARCHIVE)
	tar xzf $(DOM4J_ARCHIVE)

link:
	ln -s ../../../kleene-lang-dependencies/$(DOM4J_BASENAME).tar.gz

old:
	@make --no-print-directory clean
	tar -xzf $(DOM4J_ARCHIVE) $(DOM4J_BASENAME)/$(DOM4J_BASENAME).jar
	@echo "(We can safely ignore the spurious message \"tar: A lone zero block at XXXX\".)"

clean:
	rm -rf */

clean-all: clean
	find . -type l -delete


