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

clean-all:
	( cd Apache_Commons; $(MAKE) clean )
	( cd ICU4C; $(MAKE) clean-all )
	# ICU4J is distributed as a .jar file, no need for clean
	( cd JavaCC;  $(MAKE) clean )
	( cd Jaxen;   $(MAKE) clean )
	( cd OpenFst; $(MAKE) clean-all )
	# ROME is distributed as a .jar file, no need for clean
	( cd dom4j;   $(MAKE) clean )

