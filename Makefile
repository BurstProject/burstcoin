all:    burst.jar
	./compile.sh
doc:
	javadoc -d html/ui/doc/ -sourcepath src/java/ -subpackages nxt
clean:
	rm burst.jar
