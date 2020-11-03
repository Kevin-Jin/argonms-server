# argonms

This is a fork of argonms.

## quickstart

Obtain a localhost v62 client and modify `127.0.0.1` to redirect to `localhost`
with a hex editor.

Before starting the server, add a `.env` file to the root:

```bash
DATA_DIR="/path/to/kvj"
```

Alternatively, export these variables to the current environment. Then run the
servers with the default testing settings.

```bash
docker-compose up
```

After exiting docker-compose, remove the database and the related network:

```bash
docker-compose down
```
