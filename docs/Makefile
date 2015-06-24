# Taken from https://github.com/kjhealy/pandoc-templates/blob/master/examples/Makefile

## Put this Makefile in your project directory---i.e., the directory
## containing the paper you are writing. Assuming you are using the
## rest of the toolchain here, you can use it to create .html, .tex,
## and .pdf output files (complete with bibliography, if present) from
## your markdown file. 
## -	Change the paths at the top of the file as needed.
## -	Using `make` without arguments will generate html, tex, and pdf 
## 	output files from all of the files with the designated markdown
##	extension. The default is `.md` but you can change this. 
## -	You can specify an output format with `make tex`, `make pdf` or 
## - 	`make html`. 
## -	Doing `make clean` will remove all the .tex, .html, and .pdf files 
## 	in your working directory. Make sure you do not have files in these
##	formats that you want to keep!

SRC = manual.md

VERSION = v0.2.2

## Location of Pandoc support files.
PREFIX = .

## Location of your working bibliography file
BIB = manual.bib

## CSL stylesheet (located in the csl folder of the PREFIX directory).
CSL = ajps

PDFS=$(SRC:.md=.pdf)
HTML=$(SRC:.md=.html)
TEX=$(SRC:.md=.tex)
EPUB=$(SRC:.md=.epub)

all:	$(PDFS) $(HTML) $(TEX) $(EPUB)

pdf:	clean $(PDFS)
html:	clean $(HTML)
tex:	clean $(TEX)
epub:	clean $(EPUB)

%.html:	%.md
	pandoc -r markdown+pipe_tables+table_captions+yaml_metadata_block+escaped_line_breaks+subscript \
		--to=html5 \
		--toc \
		--smart \
		--html-q-tags \
		--self-contained \
		--reference-links \
		--template=$(PREFIX)/templates/html.template \
		--css=$(PREFIX)/marked/kultiad-serif.css \
		--filter pandoc-citeproc \
		--csl=$(PREFIX)/csl/$(CSL).csl \
		--bibliography=$(BIB) \
		-o $@ $< chapters/*.md

%.epub:	%.md
	pandoc -r markdown+pipe_tables+table_captions+yaml_metadata_block+escaped_line_breaks+subscript \
		--to=epub3 \
		--toc \
		--smart \
		--html-q-tags \
		--self-contained \
		--reference-links \
		--filter pandoc-citeproc \
		--csl=$(PREFIX)/csl/$(CSL).csl \
		--bibliography=$(BIB) \
		-o $@ $< chapters/*.md

%.tex: %.md
	pandoc -r markdown+pipe_tables+table_captions+yaml_metadata_block+escaped_line_breaks+subscript+definition_lists+compact_definition_lists \
		--to=latex \
		--standalone \
		--toc \
		--chapters \
		--smart \
		--no-tex-ligatures \
		--latex-engine=xelatex \
		--template=$(PREFIX)/templates/latex.template \
		--filter pandoc-citeproc \
		--csl=$(PREFIX)/csl/$(CSL).csl \
		--bibliography=$(BIB) \
		-o $@ $< chapters/*.md

%.pdf: %.md
	pandoc -r markdown+pipe_tables+table_captions+yaml_metadata_block+escaped_line_breaks+subscript+definition_lists+compact_definition_lists \
		--standalone \
		--toc \
		--chapters \
		--smart \
		--no-tex-ligatures \
		--latex-engine=xelatex \
		--template=$(PREFIX)/templates/latex.template \
		--filter pandoc-citeproc \
		--csl=$(PREFIX)/csl/$(CSL).csl \
		--bibliography=$(BIB) \
		-o $@ $< chapters/*.md

copy:
	cp manual.epub ../../widok.github.io/manual/widok-$(VERSION).epub
	cp manual.pdf ../../widok.github.io/manual/widok-$(VERSION).pdf
	cp manual.html ../../widok.github.io/manual/widok-$(VERSION).html

clean:
	rm -f *.html *.epub *.pdf *.tex
