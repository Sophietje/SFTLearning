package main

import "os"
import "fmt"
import "github.com/asaskevich/govalidator"

func main() {
    fmt.Println(govalidator.RemoveTags(os.Args[1]))
}
