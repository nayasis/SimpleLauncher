#include <windows.h>
#include <stdio.h>
#include <wchar.h>
#include <io.h>
#include <fcntl.h>

int main(int argc, char *argv[]) {
    int count = 10;
    if (argc > 1) {
        count = atoi(argv[1]);
        if (count <= 0) count = 10;
    }

    // Switch console to Unicode (UTF-16): use wprintf
    _setmode(_fileno(stdout), _O_U16TEXT);

    for (int i = 0; i < count; i++) {
        Sleep(1000);
        wprintf(L"%d초 경과\n", i + 1);
    }
    return 0;
}
