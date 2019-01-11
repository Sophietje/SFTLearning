<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo (str_replace("b", '', str_replace("a", '', $line)));
}
fclose($f);
?>