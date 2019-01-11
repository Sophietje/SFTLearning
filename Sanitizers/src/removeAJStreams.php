<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo (str_replace(['a', 'b', 'c', 'd', 'e', 'f', 'g'], '', $line));
}
fclose($f);
?>