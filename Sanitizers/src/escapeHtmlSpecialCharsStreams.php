<?php
$f = fopen('php://stdin', 'r');

while ($line = fgets($f)) {
    echo htmlspecialchars($line);
}
fclose($f);
?>