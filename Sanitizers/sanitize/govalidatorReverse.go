package main

import "os"
import "github.com/asaskevich/govalidator"
import "fmt"

func main() {
    fmt.Println(govalidator.Reverse(os.Args[1]))
}
