default: build run

build:
	@ gcc -o out/clox -Wfatal-errors src/*

build_opt:
	@ gcc -o out/clox -O3 src/*

run: 
	@ out/clox

clean:
	@ rm out/*

memcheck:
	@ valgrind out/clox

perf:
	@ perf stat -r 200 ./out/clox > /dev/null
