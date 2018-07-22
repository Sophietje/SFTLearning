package main

import "os"
import "fmt"
import "github.com/microcosm-cc/bluemonday"

func main() {
    p := bluemonday.UGCPolicy()
    html := p.Sanitize(os.Args[1])

    fmt.Println(html)
}