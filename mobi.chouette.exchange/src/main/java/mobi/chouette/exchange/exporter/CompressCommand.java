package mobi.chouette.exchange.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.naming.InitialContext;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.file.FileStoreFactory;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import mobi.chouette.common.monitor.JamonUtils;
import org.apache.commons.io.FileUtils;

@Log4j
public class CompressCommand implements Command, Constant {

	public static final String COMMAND = "CompressCommand";

	@Override
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);
			String path = jobData.getPathName();
			String file = jobData.getOutputFilename();
			Path target = Paths.get(path, OUTPUT);

			Path tmpFilename= Paths.get(target.toString(),file);
			File tmpFile= tmpFilename.toFile();
			if (tmpFile.exists()) tmpFile.delete();

			FileUtil.compress(target.toString(), tmpFile.toString());

			// Store file in permanent storage
			Path filename = Paths.get(path, file);
			FileStoreFactory.getFileStore().writeFile(filename, FileUtils.openInputStream(tmpFile));

			tmpFile.delete();

			result = SUCCESS;
			try {
				FileUtils.deleteDirectory(target.toFile());
			} catch (Exception e) {
				log.warn("cannot purge output directory " + e.getMessage());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new CompressCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(CompressCommand.class.getName(), new DefaultCommandFactory());
	}
}
