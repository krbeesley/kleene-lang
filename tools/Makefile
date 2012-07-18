
default:
	( cd Apache_Commons; $(MAKE) )
	( cd ICU4C; $(MAKE) )
	# ICU4J is distributed as a .jar file
	( cd JavaCC; $(MAKE) )
	( cd Jaxen; $(MAKE) )
	( cd OpenFst; $(MAKE) )
	# ROME is distributed as a .jar file
	( cd dom4j; $(MAKE) )


clean-all:
	( cd Apache_Commons; $(MAKE) clean )
	( cd ICU4C; $(MAKE) clean-all )
	# ICU4J is distributed as a .jar file
	( cd JavaCC;  $(MAKE) clean )
	( cd Jaxen;   $(MAKE) clean )
	( cd OpenFst; $(MAKE) clean-all )
	# ROME is distributed as a .jar file
	( cd dom4j;   $(MAKE) clean )

