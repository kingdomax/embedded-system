# Define a variable for classpath
CLASS_PATH = ../bin

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : Application.dll

# $@ matches the target, $< matches the first dependancy
Application.dll : Application.o
	#gcc -Wl,--add-stdcall-alias -shared -L. -ldwf -o $@ $<
	gcc -Wl,--add-stdcall-alias -shared -o $@ $< -L. -ldwf

# $@ matches the target, $< matches the first dependancy
Application.o : ImplementApiFunctions.c Application.h
	gcc -I"C:\Program Files\Java\jdk1.7.0_02\include" -I"C:\Program Files\Java\jdk1.7.0_02\include\win32" -c $< -o $@

# $* matches the target filename without the extension
Application.h : Application.class
	javah -classpath $(CLASS_PATH) $*

clean :
	rm Application.h Application.o Application.dll