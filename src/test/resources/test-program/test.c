#include <windows.h>
#include <stdio.h>
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

    for (int i = 0; i < 5; i++) {
        Sleep(1000);
        wprintf(L"%d seconds elapsed\n", i + 1);
    }
    return 0;
}
