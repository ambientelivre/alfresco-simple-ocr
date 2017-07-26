package es.keensoft.alfresco.ocr.model;

import org.alfresco.service.namespace.QName;
	 
public interface OCRdModel {
    static final String URI = "http://www.keensoft.es/model/content/ocr/1.0";
    static final QName ASPECT_OCRD = QName.createQName(URI, "ocrd");
    static final QName PROP_PROCESSED_DATE = QName.createQName(URI, "processedDate");
}

