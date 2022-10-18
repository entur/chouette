package mobi.chouette.exchange.transfer.exporter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.metrics.CommandTimer;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.exchange.transfer.Constant;
import mobi.chouette.model.Line;
import mobi.chouette.persistence.hibernate.ContextHolder;
import mobi.chouette.service.JobService;
import mobi.chouette.service.JobServiceManager;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.eclipse.microprofile.metrics.MetricRegistry;

@Log4j
@Stateless(name = TransferExporterCommand.COMMAND)
public class TransferExporterCommand implements Command, Constant, ReportConstant {


	@EJB
	private JobServiceManager jobServiceManager;

	@Inject
	protected MetricRegistry metricRegistry;

	public static final String COMMAND = "TransferExporterCommand";

	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		// initialize reporting and progression
		ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext,
				ProgressionCommand.class.getName());

		progression.initialize(context, 4);  //  Must do recount
		context.put(PROGRESSION, progression);

		String currentTentant = ContextHolder.getContext();

		try {
			TransferExportParameters parameters = (TransferExportParameters) context.get(CONFIGURATION);
			jobServiceManager.validateReferential(parameters.getDestReferentialName());
			progression.execute(context);

			// TODO : Progression

			result = new CommandTimer(metricRegistry, "netex_transfer", "NeTEx transfer timer")
					.timed( () -> {
						Command dataLoader = CommandFactory.create(initialContext, TransferExportDataLoader.class.getName());
						dataLoader.execute(context);
						progression.execute(context);
						int numLines = ((List<Line>) context.get(LINES)).size();

						// Cancel existing jobs since this one is deleting all data
						for (JobService job : jobServiceManager.activeJobs()) {
							if (job.getReferential().equals(parameters.getDestReferentialName())) {
								jobServiceManager.cancel(job.getReferential(), job.getId());
							}
						}

						ContextHolder.setContext(parameters.getDestReferentialName());

						progression.start(context, numLines + 3); // separate saving of stopareas, connectionlinks and accesslinks

						Command dataWriter = CommandFactory.create(initialContext, TransferExportDataWriter.class.getName());
						dataWriter.execute(context);
						progression.execute(context);
						return SUCCESS;
					}, currentTentant.toUpperCase(Locale.ROOT));

			progression.terminate(context, 1);
			progression.execute(context);

		} catch (CommandCancelledException e) {
			reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
			log.error(e.getMessage());
		} catch (Exception e) {
			reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR, "Fatal :" + e);
			log.error(e.getMessage(), e);
		} finally {
			progression.dispose(context);
			ContextHolder.setContext(currentTentant);
			JamonUtils.logYellow(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.transfer/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
				// try another way on test context
				String name = "java:module/" + COMMAND;
				try {
					result = (Command) context.lookup(name);
				} catch (NamingException e1) {
					log.error(e);
				}
			}
			return result;
		}
	}

	static {
		CommandFactory.factories.put(TransferExporterCommand.class.getName(), new DefaultCommandFactory());
	}
}
