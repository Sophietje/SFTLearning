<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo filter_var($argv[1], FILTER_SANITIZE_EMAIL);
}
fclose($f);
?>