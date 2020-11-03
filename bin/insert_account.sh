#!/bin/bash

set -e

echo 'INSERT INTO accounts (id, name, password, salt, pin, gm)
VALUES (0, "testing", "testing", "salt", "1234", 1);
' | mysql -h db --password=testing argonms
