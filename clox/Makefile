default: build_debug run

build_debug:
	@ gcc -o out/clox -DDEBUG_PRINT_CODE -DDEBUG_TRACE_EXECUTION -DDEBUG_STRESS_GC -DDEBUG_LOG_GC -Wfatal-errors src/*

build_opt:
	@ gcc -o out/clox -O3 src/*

build_nodebug:
	@ gcc -o out/clox -O3 -UDEBUG_PRINT_CODE -UDEBUG_TRACE_EXECUTION -UDEBUG_STRESS_GC -UDEBUG_LOG_GC src/*

run: 
	@ out/clox

clean:
	@ rm out/*

memcheck:
	@ valgrind out/clox

perf:
	@ perf stat -r 200 ./out/clox > /dev/null
