#!/bin/bash

set -e

echo 'insert into accounts 
values (0, "testing", "testing", "salt", "1234", 10, NULL, 3, 0, NULL, NULL, 4, 0, 1);
' | mysql -h db --password=testing argonms
