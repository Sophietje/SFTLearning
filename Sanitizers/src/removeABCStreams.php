<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo (str_replace(['a', 'b', 'c'], '', $line));
}
fclose($f);
?>