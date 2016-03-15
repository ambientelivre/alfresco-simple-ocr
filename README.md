# Simple OCR action #

After installation, following properties must be included in **alfresco-global.properties**

If you are using **pdfsandwich**

```
ocr.command=/usr/local/bin/pdfsandwich
ocr.output.verbose=true
ocr.output.file.prefix.command=-o

ocr.extra.commands=-verbose -lang spa+eng+fra
ocr.server.os=linux

# Thread execution pool parameters
ocr.pool.core.size=1
ocr.pool.maximum.size=1
ocr.pool.thread.priority=5

```

If you are using **OCRmyPDF**

```
ocr.command=/usr/local/bin/ocrmypdf
ocr.output.verbose=true
ocr.output.file.prefix.command=

ocr.extra.commands=--verbose 1 --force-ocr -l spa+eng+fra
ocr.server.os=linux

# Thread execution pool parameters
ocr.pool.core.size=1
ocr.pool.maximum.size=1
ocr.pool.thread.priority=5

```

If you are using **Windows.OCR**

```
ocr.url=http://localhost:60064/api/OCR/
ocr.output.verbose=true

ocr.extra.commands=Spanish
ocr.server.os=windows

# Thread execution pool parameters
ocr.pool.core.size=1
ocr.pool.maximum.size=1
ocr.pool.thread.priority=5

```