package es.keensoft.alfresco.ocr;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import es.keensoft.alfresco.ocr.model.OCRdModel;

public class OCRExtractAction extends ActionExecuterAbstractBase {
	
    private static final Log logger = LogFactory.getLog(OCRExtractAction.class);
	
	private NodeService nodeService;
	private ContentService contentService;
	private VersionService versionService;
	private TransactionService transactionService;
	
	private OCRTransformWorker ocrTransformWorker;

    private ThreadPoolExecutor threadPoolExecutor;

	// Continue current operation in case of OCR error
	private static final String PARAM_CONTINUE_ON_ERROR = "continue-on-error";
	// Force asynchronous mode
    private static final String PARAM_ASYNCHRONOUS = "asynchronous";
	
	public void init() {
		super.init();
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		
	    paramList.add(
	            new ParameterDefinitionImpl(
	            		PARAM_CONTINUE_ON_ERROR,
	                    DataTypeDefinition.BOOLEAN,
	                    false,
                        getParamDisplayLabel(PARAM_CONTINUE_ON_ERROR)));
        paramList.add(
                new ParameterDefinitionImpl(
                        PARAM_ASYNCHRONOUS,
                        DataTypeDefinition.BOOLEAN,
                        false,
                        getParamDisplayLabel(PARAM_ASYNCHRONOUS)));
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {

        if (nodeService.hasAspect(actionedUponNodeRef, OCRdModel.ASPECT_OCRD)) {

            String versionNode = nodeService.getProperty(actionedUponNodeRef, OCRdModel.PROP_APPLIED_VERSION).toString();
            String versionOCR = versionService.getCurrentVersion(actionedUponNodeRef).getVersionLabel().toString();

            if (versionNode.equals(versionOCR)) {
                return;
            }
        }

        ContentData contentData = (ContentData) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_CONTENT);

        // Exclude folders and other nodes without content
        if (contentData != null) {

            Boolean continueOnError = (Boolean) action.getParameterValue(PARAM_CONTINUE_ON_ERROR);
            if (continueOnError == null) continueOnError = true;

            Boolean forceAsync = (Boolean) action.getParameterValue(PARAM_ASYNCHRONOUS);
            if (forceAsync == null) forceAsync = false;

            // Share action set asynchronous as mandatory due to variations in response time for OCR processes when server is busy 
            if (forceAsync) {

            	    Runnable runnable = new ExtractOCRTask(actionedUponNodeRef, contentData);
            	    threadPoolExecutor.execute(runnable);

            } else {

                // # 5 Problem writing OCRed file
                // As action.getExecuteAsychronously() returns always FALSE (it's an Alfresco issue):
                // 1 - Try first with new Transaction
                // 2 - In case of error, try then with the current Transaction
                try {
                    executeInNewTransaction(actionedUponNodeRef, contentData);
                } catch (Throwable throwableNewTransaction) {
                    logger.warn(actionedUponNodeRef + ": " + throwableNewTransaction.getMessage());
                    try {
                        // Current transaction
                        executeImplInternal(actionedUponNodeRef, contentData);
                    } catch (Throwable throwableCurrentTransaction) {
                        if (continueOnError) {
                            logger.warn(actionedUponNodeRef + ": " + throwableNewTransaction.getMessage());
                        } else {
                            throw throwableCurrentTransaction;
                        }
                    }
                }
            }

        }

	}

    private class ExtractOCRTask implements Runnable {
        
        private NodeRef nodeToBeOCRd;
        private ContentData contentData;
         
        private ExtractOCRTask(NodeRef nodeToBeOCRd, ContentData contentData) {
            this.nodeToBeOCRd = nodeToBeOCRd;
            this.contentData = contentData;
        }
        
        @Override
        public void run() {
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
                public Void doWork() throws Exception {
                	    executeInNewTransaction(nodeToBeOCRd, contentData); 
                    return null;
                }
            });
        }
    }
    
    // Avoid ConcurrencyFailureException by using RetryingTransactionHelper
	private void executeInNewTransaction(final NodeRef nodeRef, final ContentData contentData) {
		
        RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
		    	executeImplInternal(nodeRef, contentData);
                return null;
            }
        };
        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        txnHelper.doInTransaction(callback, false, true);
	}
	
	private void executeImplInternal(NodeRef actionedUponNodeRef, ContentData contentData) {
		
		String originalMimeType = contentData.getMimetype();                    		
		
		ContentReader reader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(contentData.getMimetype());
		
	    try {
	        ocrTransformWorker.transform(reader, writer, null);
	    } catch (Exception e) {
	    	    throw new RuntimeException(e);
	    }
	    
	    // Set initial version if it's a new one
        versionService.ensureVersioningEnabled(actionedUponNodeRef, null);
	    if (!versionService.isVersioned(actionedUponNodeRef)) {
		    	Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
		    	versionProperties.put(Version.PROP_DESCRIPTION, "OCRd");
		    	versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
		    	versionService.createVersion(actionedUponNodeRef, versionProperties);
	    }
	    
	    ContentWriter writeOriginalContent = null;
	    // Update original PDF file
	    if (originalMimeType.equals(MimetypeMap.MIMETYPE_PDF)) {
    	        writeOriginalContent = contentService.getWriter(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
	    } else {
		    	// Create new PDF file
		    	String fileName = nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME) + ".pdf";
		    	Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		        props.put(ContentModel.PROP_NAME, fileName);
		    	NodeRef pdfNodeRef = createNode(nodeService.getPrimaryParent(actionedUponNodeRef).getParentRef(), fileName, props);
		    	writeOriginalContent = contentService.getWriter(pdfNodeRef, ContentModel.PROP_CONTENT, true);
		    	writeOriginalContent.setMimetype(MimetypeMap.MIMETYPE_PDF);
	    }
	    writeOriginalContent.putContent(writer.getReader());    	    
		
		// Manual versioning because of Alfresco insane rules for first version content nodes
		versionService.ensureVersioningEnabled(actionedUponNodeRef, null);
		Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
		versionProperties.put(Version.PROP_DESCRIPTION, "OCRd");
		versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
        versionService.createVersion(actionedUponNodeRef, versionProperties);
    	
	    // Set OCRd aspect to avoid future re-OCR process
	    Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
	    aspectProperties.put(OCRdModel.PROP_PROCESSED_DATE, new Date());
	    aspectProperties.put(OCRdModel.PROP_APPLIED_VERSION, versionService.getCurrentVersion(actionedUponNodeRef).getVersionLabel());
		nodeService.addAspect(actionedUponNodeRef, OCRdModel.ASPECT_OCRD, aspectProperties);
	}
		
	private NodeRef createNode(NodeRef parentNodeRef, String name, Map<QName, Serializable> props) {
	    return nodeService.createNode(
	                parentNodeRef, 
	                ContentModel.ASSOC_CONTAINS, 
	                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
	                ContentModel.TYPE_CONTENT, 
	                props).
	    	   getChildRef();
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setOcrTransformWorker(OCRTransformWorker ocrTransformWorker) {
		this.ocrTransformWorker = ocrTransformWorker;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }


}
