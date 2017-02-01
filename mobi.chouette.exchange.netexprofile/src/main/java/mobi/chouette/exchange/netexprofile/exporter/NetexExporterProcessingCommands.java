package mobi.chouette.exchange.netexprofile.exporter;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.exporter.CompressCommand;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@Log4j
public class NetexExporterProcessingCommands implements ProcessingCommands, Constant {

    public static class DefaultFactory extends ProcessingCommandsFactory {

        @Override
        protected ProcessingCommands create() throws IOException {
            return new NetexExporterProcessingCommands();
        }
    }

    static {
        ProcessingCommandsFactory.factories.put(NetexExporterProcessingCommands.class.getName(),
                new NetexExporterProcessingCommands.DefaultFactory());
    }

    @Override
    public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
        InitialContext initCtx = (InitialContext) context.get(INITIAL_CONTEXT);
        List<Command> commands = new ArrayList<>();

        try {
            commands.add(CommandFactory.create(initCtx, NetexInitExportCommand.class.getName()));
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

    @Override
    public List<? extends Command> getLineProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        List<Command> commands = new ArrayList<>();

        try {
            if (withDao) {
                log.info("TODO : implement");
                //commands.add(CommandFactory.create(initialContext, DaoNeptuneLineProducerCommand.class.getName()));
            } else {
                log.info("TODO : implement");
                //commands.add(CommandFactory.create(initialContext, NeptuneLineProducerCommand.class.getName()));
            }
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;

    }

    @Override
    public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();

        try {

            if (parameters.isValidateAfterExport()) {
                log.info("TODO : implement");
                //commands.add(CommandFactory.create(initialContext, NeptuneValidateExportCommand.class.getName()));
            }
            if (parameters.isAddMetadata()) {
                log.info("TODO : implement");
                //commands.add(CommandFactory.create(initialContext, SaveMetadataCommand.class.getName()));
            }

            commands.add(CommandFactory.create(initialContext, CompressCommand.class.getName()));
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }
}
