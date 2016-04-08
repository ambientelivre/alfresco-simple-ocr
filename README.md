Alfresco Simple OCR Action
==========================

This addon provides an action to extract OCR text from images or plain PDFs in Alfresco.

**License**
The plugin is licensed under the [LGPL v3.0](http://www.gnu.org/licenses/lgpl-3.0.html). 

**State**
Current addon release is 1.0.0

**Compatibility**
The current version has been developed using Alfresco 5.1 and Alfresco SDK 2.1.1, although it should run in Alfresco 5.0.d and Alfresco 5.0.c

Browser compatibility: 100% supported

Supported OCR software:
* [pdfsandwich](http://www.tobias-elze.de/pdfsandwich/)
* [OCRmyPDF](https://github.com/jbarlow83/OCRmyPDF)
* [Windows.Media.OCR](https://www.nuget.org/packages/Microsoft.Windows.Ocr/) as local service

**Languages**
Currently Share interface is provided in English, Spanish, Brazilian Portuguese and German.
OCR supported languages catalog depends directly on selected OCR software ([Tesseract OCR](https://github.com/tesseract-ocr) or [Windows.Media.OCR](https://www.nuget.org/packages/Microsoft.Windows.Ocr/))

***No original Alfresco resources have been overwritten***

BeeCon 2016
-----------
This addon was presented a BeeCon 2016. You can find additionals details at [Integrating a simple OCR in Alfresco](http://beecon.buzz/talks/?id=20160125005)

Downloading the ready-to-deploy-plugin
--------------------------------------
The binary distribution is made of one amp file to be deployed in Alfresco:

* [repo AMP](https://github.com/keensoft/alfresco-simple-ocr/releases/download/1.0.0/simple-ocr-repo.amp)

You can install them by using standard [Alfresco deployment tools](http://docs.alfresco.com/community/tasks/dev-extensions-tutorials-simple-module-install-amp.html)

Building the artifacts
----------------------
If you are new to Alfresco and the Alfresco Maven SDK, you should start by reading [Jeff Potts' tutorial on the subject](http://ecmarchitect.com/alfresco-developer-series-tutorials/maven-sdk/tutorial/tutorial.html).

You can build the artifacts from source code using maven
```$ mvn clean package```

Installation
----------------------

OCR software for Linux depends on programs like `gs` or `ImageMagick`, which are also dependencies for Alfresco. In order to avoid problems, it's recommended to install Alfresco from scratch, letting the OS the installation of the packages. 

You can find detailed instructions to perform Alfresco installation from scratch at [Alfresco Documentation](http://docs.alfresco.com/community/tasks/alf-tomcat-install.html).

If you are using Linux and your Alfresco is installed by using [default wizards](http://docs.alfresco.com/community/concepts/simpleinstalls-community-intro.html), you must pay attention to environment execution for programs launched inside your JVM and you must adjust versions and path precedence.

You can find more options to solve this problem at the [FAQ](https://github.com/keensoft/alfresco-simple-ocr/wiki/FAQ) page.

Configuration
----------------------

After installation, following properties must be included in **alfresco-global.properties**

* If you are using **pdfsandwich**

```
ocr.command=/usr/local/bin/pdfsandwich
ocr.output.verbose=true
ocr.output.file.prefix.command=-o

ocr.extra.commands=-verbose -lang spa+eng+fra
ocr.server.os=linux

```

* If you are using **OCRmyPDF**

```
ocr.command=/usr/local/bin/ocrmypdf
ocr.output.verbose=true
ocr.output.file.prefix.command=

ocr.extra.commands=--verbose 1 --force-ocr -l spa+eng+fra
ocr.server.os=linux

```

* If you are using **Windows.OCR**

```
ocr.url=http://localhost:60064/api/OCR/
ocr.output.verbose=true

ocr.extra.commands=Spanish
ocr.server.os=windows

```

Usage
----------------------
* Including a rule on a folder by selecting **ocr-extract** action
* Every dropped image on this folder will be sent to OCR software in order to produce a searchable PDF file. 
* To perform this operation asynchronously, just use the check provided by Alfresco to configure the rule.
* To allow Alfresco operating in case of OCR error, set the rule check `Continue on error`
