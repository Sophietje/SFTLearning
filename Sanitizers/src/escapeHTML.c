#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "hescape/hescape.h"

int main(int argc, char **argv)
{
    if (argc > 1) {
        uint8_t *dest, *src = argv[1];
        size_t len = hesc_escape_html(&dest, src, strlen(src));
        printf("%s\n", dest);
        if (len > strlen(src)) {
          free(dest);
        }
    }
}