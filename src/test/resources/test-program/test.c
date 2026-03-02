#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <wchar.h>
#include <io.h>
#include <fcntl.h>

int main(int argc, char *argv[]) {

    // Switch console to Unicode (UTF-16): use wprintf
    _setmode(_fileno(stdout), _O_U16TEXT);

    if (argc == 0) {
        wprintf(L">>> no title\n");
    } else if (argc > 1) {
        wprintf(L">>> %hs\n", argv[1]);
    }

    int loop_count = 5;  // default value
    if (argc > 2) {
        char *endptr;
        loop_count = strtol(argv[2], &endptr, 10);
        if (*endptr != '\0') {
            wprintf(L"Error: Second argument must be a number\n");
            return 1;
        }
        if (loop_count < 0) {
            loop_count = 0;
        }
    }

    wprintf(L"Loop count: %d\n", loop_count);

    for (int i = 0; i < loop_count; i++) {
        Sleep(1000);
        wprintf(L"%d seconds elapsed\n", i + 1);
    }
    return 0;
}
