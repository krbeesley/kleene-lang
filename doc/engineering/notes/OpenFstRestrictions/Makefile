# Generic Makefile

# replace XXX with bare name, minus the .tex extension
TARGET=OpenFstRestrictions

default: $(TARGET).pdf

$(TARGET).pdf: $(TARGET).tex
	pdflatex $(TARGET)
# bibtex $(TARGET)
#	pdflatex $(TARGET)
#	pdflatex $(TARGET)

pdf: open

open: $(TARGET).pdf
	evince $(TARGET).pdf

clean:  
	rm -f $(TARGET).aux $(TARGET).bbl $(TARGET).blg $(TARGET).toc $(TARGET).dvi $(TARGET).ent $(TARGET).log $(TARGET).ps $(TARGET).pdf $(TARGET).cb* *~
