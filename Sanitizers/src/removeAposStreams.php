<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo (str_replace("'", '', $line));
}
fclose($f);
?>