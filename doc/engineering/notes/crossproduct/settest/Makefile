NAME=set
# for some reason, don't use name 'script'
CC=g++

DEBUG=
#DEBUG=-g
# -g causes compiled file to include info used by a debugger

#WARNINGS=
WARNINGS=-Wall

$(NAME): $(NAME).cc
	$(CC) $(DEBUG) $(WARNINGS) -o $(NAME) $(NAME).cc
	@echo "Executable created: try 'make run'"

run: $(NAME)
	$(NAME)

clean:
	rm -f $(NAME) *~
