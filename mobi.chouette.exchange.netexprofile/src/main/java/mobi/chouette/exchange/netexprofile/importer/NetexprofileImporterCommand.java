package mobi.chouette.exchange.netexprofile.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.metrics.CommandTimer;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.model.util.Referential;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = NetexprofileImporterCommand.COMMAND)
public class NetexprofileImporterCommand extends AbstractImporterCommand implements Command, Constant {

	public static final String COMMAND = "NetexprofileImporterCommand";

	@Inject
	protected MetricRegistry metricRegistry;

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public boolean execute(Context context) throws Exception {
		boolean result = SUCCESS;
		Monitor monitor = MonitorFactory.start(COMMAND);

		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		ActionReporter actionReporter = ActionReporter.Factory.getInstance();

		context.put(REFERENTIAL, new Referential());

		// initialize reporting and progression
		ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext,
				ProgressionCommand.class.getName());

		try {

		Object configuration = context.get(CONFIGURATION);
		if (!(configuration instanceof NetexprofileImportParameters)) {
			// fatal wrong parameters
			log.error("invalid parameters for netex import " + configuration.getClass().getName());
			actionReporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS, "invalid parameters for netex import " + configuration.getClass().getName());
			return false;
		}

		ProcessingCommands commands = ProcessingCommandsFactory.create(NetexImporterProcessingCommands.class.getName());

		result = new CommandTimer(metricRegistry, "netex_import", "NeTEx import timer")
				.timed(() -> process(context, commands, progression, true, Mode.line), ((NetexprofileImportParameters) configuration).getObjectIdPrefix());

		} catch (CommandCancelledException e) {
			actionReporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
			log.error(e.getMessage());
		} catch (Exception e) {
			String fileName = (String) context.get(FILE_NAME);
			log.error("Error parsing Netex file "+fileName+": "+e.getMessage(), e);
			actionReporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR, "Internal error while parsing Netex files: "+e.toString());
		} finally {
			progression.dispose(context);
			JamonUtils.logYellow(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.netexprofile/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
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
		CommandFactory.factories.put(NetexprofileImporterCommand.class.getName(), new DefaultCommandFactory());
	}
}
