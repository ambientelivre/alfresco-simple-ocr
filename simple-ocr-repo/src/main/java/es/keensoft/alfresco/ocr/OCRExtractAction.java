package es.keensoft.alfresco.ocr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.transaction.TransactionListenerAdapter;

import es.keensoft.alfresco.ocr.model.OCRdModel;

public class OCRExtractAction extends ActionExecuterAbstractBase {
	
	private NodeService nodeService;
	private ContentService contentService;
    private TransactionService transactionService;
    private TransactionListener transactionListener; 
    private ThreadPoolExecutor threadPoolExecutor;
	
	private OCRTransformWorker ocrTransformWorker;
	
    private static final String KEY_OCR_NODE = OCRExtractAction.class.getName() + ".OCRNode";
	
	public void init() {
		super.init();
		transactionListener = new ExtractOCRTransactionListener();
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		
		if (!nodeService.hasAspect(actionedUponNodeRef, OCRdModel.ASPECT_OCRD)) {
			// OCR performed after transaction commit (asynchronously)
	        AlfrescoTransactionSupport.bindListener(transactionListener);
	        // More than one node can be involved inside this transaction
	        List<NodeRef> nodeRefsToBeOCRd = null;
	        if (AlfrescoTransactionSupport.getResource(KEY_OCR_NODE) == null) {
	        	nodeRefsToBeOCRd = new ArrayList<NodeRef>();
	        } else {
	        	nodeRefsToBeOCRd = AlfrescoTransactionSupport.getResource(KEY_OCR_NODE);
	        }
        	nodeRefsToBeOCRd.add(actionedUponNodeRef);
            AlfrescoTransactionSupport.bindResource(KEY_OCR_NODE, nodeRefsToBeOCRd);
		}
        
	}
	
    private class ExtractOCRTransactionListener extends TransactionListenerAdapter implements TransactionListener {
 
        @Override
        public void afterCommit() {
        	List<NodeRef> nodesToBeOCRd = AlfrescoTransactionSupport.getResource(KEY_OCR_NODE);
        	for (NodeRef nodeToBeOCRd : nodesToBeOCRd) {
	            Runnable runnable = new ExtractOCRTask(nodeToBeOCRd);
	            threadPoolExecutor.execute(runnable);
        	}
        }
         
        @Override
        public void flush() {}
         
    }
    
    private class ExtractOCRTask implements Runnable {
         
        private NodeRef nodeToBeOCRd;
         
        private ExtractOCRTask(NodeRef nodeToBeOCRd) {
            this.nodeToBeOCRd = nodeToBeOCRd;
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
 
        @Override
        public void run() {
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
                 
                public Void doWork() throws Exception {
                     
                    RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
                         
                        @Override
                        public Void execute() throws Throwable {
                        	
                        	if (nodeService.exists(nodeToBeOCRd)) {
                        	
	                    		ContentData contentData = (ContentData) nodeService.getProperty(nodeToBeOCRd, ContentModel.PROP_CONTENT);
	                    		String originalMimeType = contentData.getMimetype();                    		
	                    		
	                    		ContentReader reader = contentService.getReader(nodeToBeOCRd, ContentModel.PROP_CONTENT);
	                    		
	                    		// Non PDF files (such as images)
	                    		if (!originalMimeType.equals(MimetypeMap.MIMETYPE_PDF)) {
	                    			
	                    		    // Try to transform any format to PDF
	                    	        ContentWriter writer = contentService.getTempWriter();
	                    	        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
	                    		    contentService.transform(reader, writer);
	                    		    
	                    		    // Set PDF as content reader
	                    		    reader = writer.getReader();
	                    		    
	                    		}
	                    		
	                	        ContentWriter writer = contentService.getTempWriter();
	                	        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
	                    		
	                    	    try {
	                    	        ocrTransformWorker.transform(reader, writer, null);
	                    	    } catch (Exception e) {
	                    	    	throw new RuntimeException(e);
	                    	    }
	                    	    
	                    	    ContentWriter writeOriginalContent = null;
	                    	    // Update original PDF file
	                    	    if (originalMimeType.equals(MimetypeMap.MIMETYPE_PDF)) {
		                    	    writeOriginalContent = contentService.getWriter(nodeToBeOCRd, ContentModel.PROP_CONTENT, true);
	                    	    } else {
	                    	    	// Create new PDF file
	                    	    	String fileName = nodeService.getProperty(nodeToBeOCRd, ContentModel.PROP_NAME) + ".pdf";
	                    	    	Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
	                    	        props.put(ContentModel.PROP_NAME, fileName);
	                    	    	NodeRef pdfNodeRef = createNode(nodeService.getPrimaryParent(nodeToBeOCRd).getParentRef(), fileName, props);
	                    	    	writeOriginalContent = contentService.getWriter(pdfNodeRef, ContentModel.PROP_CONTENT, true);
	                    	    	writeOriginalContent.setMimetype(MimetypeMap.MIMETYPE_PDF);
	                    	    }
	                    	    writeOriginalContent.putContent(writer.getReader());
	                    	    
	                    	    // Set OCRd aspect to avoid future re-OCR process
	                    	    Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
	                    	    aspectProperties.put(OCRdModel.PROP_PROCESSED_DATE, new Date());
								nodeService.addAspect(nodeToBeOCRd, OCRdModel.ASPECT_OCRD, aspectProperties);
								
                        	}
                            
                            return null;
                        }
                    };
                     
                    try {
                        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
                        txnHelper.doInTransaction(callback, false, true);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                     
                    return null;
                     
                }
            });
        }
    }

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
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

	public TransactionService getTransactionService() {
		return transactionService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}

}
