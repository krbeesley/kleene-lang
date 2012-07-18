# Generic Makefile

# replace XXX with bare name, minus the .tex extension
TARGET=case

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
	rm -f $(TARGET).aux $(TARGET).log $(TARGET).pdf *~
