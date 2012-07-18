# Generic Makefile

TARGET=precedence

default: $(TARGET).pdf

$(TARGET).pdf: $(TARGET).tex
	pdflatex $(TARGET)
# bibtex $(TARGET)
#	pdflatex $(TARGET)
#	pdflatex $(TARGET)

pdf: open

open: $(TARGET).pdf
	open $(TARGET).pdf

clean:  
	rm -f $(TARGET).aux $(TARGET).bbl $(TARGET).blg $(TARGET).toc $(TARGET).dvi $(TARGET).ent $(TARGET).log $(TARGET).ps $(TARGET).pdf $(TARGET)$(SMALL).ps  $(TARGET)$(SMALL).pdf
