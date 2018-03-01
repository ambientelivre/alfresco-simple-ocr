
Alfresco Simple OCR Action
==========================

This addon provides an action to extract OCR text from images or plain PDFs in Alfresco.

**License**
The plugin is licensed under the [LGPL v3.0](http://www.gnu.org/licenses/lgpl-3.0.html). 

**State**
Current addon release is 2.3.1

**Compatibility**
The current version has been developed using Alfresco 5.2 and Alfresco SDK 3.0.2, although it should also run in Alfresco 5.1, 5.0 & 4.2 (as it is developed by using Alfresco SDK 3.0)

Browser compatibility: 100% supported

Supported OCR software:
* [pdfsandwich](http://www.tobias-elze.de/pdfsandwich/)
* [OCRmyPDF](https://github.com/jbarlow83/OCRmyPDF)
* [Windows.Media.OCR](https://www.nuget.org/packages/Microsoft.Windows.Ocr/) as local service (we are not providing this software, you have to build it by yourself)

**Languages**
Currently Share action interface is provided in English and the behaviour internface in English, Spanish, Brazilian Portuguese, German and Italian.
OCR supported languages catalog depends directly on selected OCR software ([Tesseract OCR](https://github.com/tesseract-ocr) or [Windows.Media.OCR](https://www.nuget.org/packages/Microsoft.Windows.Ocr/))

***No original Alfresco resources have been overwritten***


BeeCon 2016
-----------
This addon was presented a BeeCon 2016. You can find additionals details at [Integrating a simple OCR in Alfresco](http://beecon.buzz/talks/?id=20160125005)


Downloading the ready-to-deploy-plugin
--------------------------------------
The binary distribution is made of two jar files to be deployed in Alfresco as modules:

* [repo JAR](https://github.com/keensoft/alfresco-simple-ocr/releases/download/2.3.1/simple-ocr-repo-2.3.1.jar)
* [share JAR](https://github.com/keensoft/alfresco-simple-ocr/releases/download/2.3.1/simple-ocr-share-2.3.1.jar)

You can install them by putting the jar files in [module folder](http://docs.alfresco.com/community/concepts/dev-extensions-packaging-techniques-jar-files.html):

* Copy [repo JAR](https://github.com/keensoft/alfresco-simple-ocr/releases/download/2.3.1/simple-ocr-repo-2.3.1.jar) to `/opt/alfresco/modules/platform` (create the directory if it does not exist)
* Copy [share JAR](https://github.com/keensoft/alfresco-simple-ocr/releases/download/2.3.1/simple-ocr-share-2.3.1.jar) to `/opt/alfresco/modules/share`

Re-start Alfresco after copying the files.

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
ocr.command=/usr/bin/pdfsandwich
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


Usage of rule
----------------------
* Including a rule on a folder by selecting **Extract OCR** action
* Every dropped image on this folder will be sent to OCR software in order to produce a searchable PDF file. 
* To perform this operation asynchronously, just use the check provided by Alfresco to configure the rule.
* To allow Alfresco operating in case of OCR error, set the rule check `Continue on error`

Usage of action
----------------------
* Press the action **OCR** in document browser or document details
* The action will be executed in asynchronous mode, so the result will be available after a time

Known issues
-----------
* When using WebDAV to upload documents, only **asynchronous** rule execution is allowed
