# Generic Makefile

# replace XXX with bare name, minus the .tex extension
TARGET=kleene
# expanded version of paper submitted to FSMNLP 08

default: $(TARGET).pdf

$(TARGET).pdf: $(TARGET).tex chapt1.tex chapt2.tex chapt3.tex chapt4.tex chapt5.tex chapt6.tex chapt7.tex chapt8.tex chapt9.tex app1.tex app2.tex app3.tex app4.tex cheat1.tex cheat2.tex cheat3.tex cheat4.tex cheat5.tex
	xelatex $(TARGET)
	bibtex $(TARGET)
	xelatex $(TARGET)
	xelatex $(TARGET)
#	makeindex -cl -s $(NAME).ist $(NAME).idx
#	xelatex $(TARGET)
#	xelatex $(TARGET)

pdf: open

open: $(TARGET).pdf
	open $(TARGET).pdf

clean:  
	rm -f *.aux $(TARGET).bbl $(TARGET).blg $(TARGET).toc $(TARGET).dvi $(TARGET).ent $(TARGET).log $(TARGET).ps $(TARGET).pdf $(TARGET)$(SMALL).ps  $(TARGET)$(SMALL).pdf *.cb *.cb2 *~
