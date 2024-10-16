package mobi.chouette.exchange.neptune.exporter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.naming.InitialContext;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.model.util.Referential;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import java.time.LocalDateTime;

@Log4j
public class NeptuneInitExportCommand implements Command, Constant {

	public static final String COMMAND = "NeptuneInitExportCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);
			jobData.setOutputFilename("export_" + jobData.getType() + "_" + jobData.getId() + ".zip");

			context.put(REFERENTIAL, new Referential());
			Metadata metadata = new Metadata(); // if not asked, will be used as
												// dummy
			metadata.setDate(LocalDateTime.now());
			metadata.setFormat("application/xml");
			metadata.setTitle("Export Neptune ");
			try {
				metadata.setRelation(new URL(
						"http://www.normes-donnees-tc.org/format-dechange/donnees-theoriques/neptune/"));
			} catch (MalformedURLException e1) {
				log.error(
						"problem with http://www.normes-donnees-tc.org/format-dechange/donnees-theoriques/neptune/ url",
						e1);
			}

			context.put(METADATA, metadata);
			// prepare exporter
			Path path = Paths.get(jobData.getPathName(), OUTPUT);
			if (!Files.exists(path)) {
				Files.createDirectories(path);
			}
			result = SUCCESS;

		} catch (Exception e) {
			log.error(e, e);
			throw e;
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new NeptuneInitExportCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(NeptuneInitExportCommand.class.getName(), new DefaultCommandFactory());
	}

}
