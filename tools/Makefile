default: build-libraries

build-libraries:
	( cd Apache_Commons; $(MAKE) )
	( cd ICU4C; $(MAKE) )
	# ICU4J is distributed as a .jar file, no need for make
	( cd JavaCC; $(MAKE) )
	( cd Jaxen; $(MAKE) )
	( cd OpenFst; $(MAKE) )
	# ROME is distributed as a .jar file, no need for make
	( cd dom4j; $(MAKE) )

links:
	( cd Apache_Commons; $(MAKE) link )
	( cd ICU4C; $(MAKE) link )
	( cd ICU4J; $(MAKE) link )
	( cd JavaCC; $(MAKE) link )
	( cd Jaxen; $(MAKE) link )
	( cd OpenFst; $(MAKE) link )
	( cd ROME; $(MAKE) link )
	( cd dom4j; $(MAKE) link )

# clean to rebuild with the same libraries, versions, platform
clean:
	( cd Apache_Commons; $(MAKE) clean )
	( cd ICU4C; $(MAKE) clean )
	# ICU4J is distributed as a .jar file, no need for clean
	( cd JavaCC;  $(MAKE) clean )
	( cd Jaxen;   $(MAKE) clean )
	( cd OpenFst; $(MAKE) clean )
	# ROME is distributed as a .jar file, no need for clean
	( cd dom4j;   $(MAKE) clean )

# clean-all to restart, perhaps with new libraries/versions,
# perhaps on a different platform, or just a library upgrade.
# After clean-all, you need to 'make links' again.
clean-all:
	( cd Apache_Commons; $(MAKE) clean-all )
	( cd ICU4C; $(MAKE) clean-all )
	( cd ICU4J; $(MAKE) clean-all )
	( cd JavaCC;  $(MAKE) clean-all )
	( cd Jaxen;   $(MAKE) clean-all )
	( cd OpenFst; $(MAKE) clean-all )
	( cd ROME; $(MAKE) clean-all )
	( cd dom4j;   $(MAKE) clean-all )

