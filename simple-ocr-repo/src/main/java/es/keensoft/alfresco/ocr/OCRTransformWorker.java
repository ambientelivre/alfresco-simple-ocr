package es.keensoft.alfresco.ocr;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// Transformer from plain PDF (images attached) to Searchable PDF by using OCR
public class OCRTransformWorker extends ContentTransformerHelper implements ContentTransformerWorker {
	
    private static final Log logger = LogFactory.getLog(OCRTransformWorker.class);
    
    private boolean verbose = false;

    private static final List<String> sourceMimeTypes =
            Arrays.asList(new String[] {"application/pdf", "image/bmp", "image/x-portable-anymap", "image/png", "image/jpeg", "image/tiff"});

    private static final String VAR_SOURCE = "source";
    private static final String VAR_TARGET = "target";

	public final static String SERVER_OS_LINUX = "linux";
	public final static String SERVER_OS_WINDOWS = "windows";


    private RuntimeExec executerLinux;
    private RuntimeExec executerWindows;
    private String serverOS;
    private RuntimeExec checkCommand;
    
    private MimetypeService mimetypeService;
    
    private boolean available = true;
    private Date lastChecked = new Date(0l);
    private int checkFrequencyInSeconds = 120;
    
    public final void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
    	
    	try {
    	
			String sourceMimetype = getMimetype(reader);
	        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
	        File sourceFile = TempFileProvider.createTempFile(getClass().getSimpleName() + "_source_", "." + sourceExtension);
	        reader.getContent(sourceFile);
	        
	        String path = sourceFile.getAbsolutePath();
	        String targetPath = path.substring(0, path.toLowerCase().lastIndexOf(".")) + "_ocr.pdf";

	        Map<String, String> properties = new HashMap<String, String>(1);
	
	        properties.put(VAR_SOURCE, sourceFile.getAbsolutePath());
	        properties.put(VAR_TARGET, targetPath);     
	        
	        RuntimeExec.ExecutionResult result = obtainExecuter(properties);
	        
	        if (verbose) {
	        	logger.info("EXIT VALUE: " + result.getExitValue());
	        	logger.info("STDOUT: " + result.getStdOut());
	        	logger.info("STDERR: " + result.getStdErr());
	        }
	        
	        if (result.getExitValue() == 143) {
	        	logger.warn(result.getStdErr());
	        }
	        else if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0) {
	            throw new ContentIOException("Failed to perform OCR transformation: \n" + result);
	        }
	        
	        File targetFile = new File(targetPath);
	        writer.putContent(targetFile);
	        
    	} catch (Throwable t) {
    		throw new RuntimeException(t);
    	}
    }
    
    private RuntimeExec.ExecutionResult obtainExecuter(Map<String, String> properties){
    	if (serverOS.equals(SERVER_OS_LINUX)) {
    		return executerLinux.execute(properties);    		
    	} else if (serverOS.equals(SERVER_OS_WINDOWS)) {
    		return executerWindows.execute(properties);    		
    	} else {
    		throw new ContentIOException("Failed to recognize the operative system: \n" + serverOS);
    	}
    }

	@Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
		if (targetMimetype.equals("application/pdf")) {
			if (sourceMimeTypes.contains(sourceMimetype)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getVersionString() {
		return "OCR Transformer V1.0";
	}
	
    public void setCheckFrequencyInSeconds(int frequency) {
    	checkFrequencyInSeconds=frequency;
    }
    
    public boolean isAvailable() {
    	Date refreshAvailabilityDate = new Date(lastChecked.getTime()+1000l*checkFrequencyInSeconds);
    	if (new Date().after(refreshAvailabilityDate)) {
    		test();
    	}
    	return available;
    }
    
    public void setMimetypeService(MimetypeService ms) {
    	mimetypeService = ms;
    }

    
    public void setCheckCommand(RuntimeExec checkCommand) {
        this.checkCommand = checkCommand;
    }
    
    protected void test() {
    	try {
    		logger.debug("Testing availability");
    		RuntimeExec.ExecutionResult result = checkCommand.execute();
    		available=result.getSuccess();
    		logger.info("Is OCR available? " + available);
    	}
    	catch (Exception e) {
    		available=false;
    		logger.warn("Check command [" + checkCommand.getCommand() + "] failed.  Registering transform as unavailable for the next " + checkFrequencyInSeconds + " seconds");
    	}
    }

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setExecuterLinux(RuntimeExec executerLinux) {
		this.executerLinux = executerLinux;
	}

	public void setExecuterWindows(RuntimeExec executerWindows) {
		this.executerWindows = executerWindows;
	}

	public void setServerOS(String serverOS) {
		this.serverOS = serverOS;
	}    
}