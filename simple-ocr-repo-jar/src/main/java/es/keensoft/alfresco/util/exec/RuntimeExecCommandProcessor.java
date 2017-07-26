package es.keensoft.alfresco.util.exec;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.util.exec.RuntimeExec;

public class RuntimeExecCommandProcessor extends RuntimeExec {
		
	@Override
	public ExecutionResult execute(Map<String, String> properties, long timeoutMs) {
		ExecutionResult executionResult = null;		
		executionResult = super.execute(properties, timeoutMs);		
		return executionResult;
	}

	@Override
	public void setCommandsAndArguments(Map<String, String[]> commandsByOS) {
		super.setCommandsAndArguments(processCommandLine(commandsByOS));
	}
	
	// Separating parameters by space delimiter
	private Map<String, String[]> processCommandLine(Map<String, String[]> commandsByOS) {
		for (String key : commandsByOS.keySet()) {
			String[] options = commandsByOS.get(key);
			List<String> processedOptions = new ArrayList<String>();
			for (String option : options) {
				if (option != null && !"".equals(option)) {
					for(String part : option.split(" ")) {
						processedOptions.add(part);
					}
				}
			}
			commandsByOS.put(key, processedOptions.toArray(new String[processedOptions.size()]));
		}
		return commandsByOS;
	}
	
	public static ExecutionResult getInstance(final Process process,
            final String[] command,
            final Set<Integer> errCodes,
            final int exitValue,
            final String stdOut,
            final String stdErr) {
		try {
			Constructor<ExecutionResult> constructor = ExecutionResult.class.getDeclaredConstructor(Process.class, String[].class, Set.class, int.class, String.class, String.class);
			constructor.setAccessible(true);
			return constructor.newInstance(process, command, errCodes, exitValue, stdOut, stdErr);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}	
}
