# This GNU makefile extracts the JavaCC pre-built bin files.
# Written here by Phil Sours (not from JavaCC).
#
# JavaCC is a free, open-source compiler compiler or parser generator
# for use by Java applications.  https://javacc.dev.java.net/

JAVACC_BASENAME=javacc-5.0
JAVACC_ARCHIVE=$(JAVACC_BASENAME).tar.gz

$(JAVACC_BASENAME): $(JAVACC_ARCHIVE)
	tar xzf $(JAVACC_ARCHIVE)

old:
	@make --no-print-directory clean
	# KRB 2012-04--2 had to remove the ./
	#tar -xzf $(JAVACC_ARCHIVE) ./$(JAVACC_BASENAME)/bin
	tar -xzf $(JAVACC_ARCHIVE) $(JAVACC_BASENAME)/bin
clean:
	rm -rf */
