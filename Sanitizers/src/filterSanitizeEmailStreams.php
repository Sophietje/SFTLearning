<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo (filter_var($line, FILTER_SANITIZE_EMAIL)."\n");
}
fclose($f);
?>