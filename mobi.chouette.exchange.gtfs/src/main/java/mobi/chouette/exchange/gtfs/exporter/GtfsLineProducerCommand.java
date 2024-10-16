/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsRouteProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsServiceProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsShapeProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsTripProducer;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.metadata.NeptuneObjectPresenter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.DatedServiceJourney;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.util.NamingUtil;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import java.time.LocalDate;

/**
 *
 */
@Log4j
public class GtfsLineProducerCommand implements Command, Constant {
	public static final String COMMAND = "GtfsLineProducerCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		try {

			Line line = (Line) context.get(LINE);
			GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);

			ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
			if (collection == null) {
				collection = new ExportableData();
				context.put(EXPORTABLE_DATA, collection);
			}
			reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
					OBJECT_STATE.OK, IO_TYPE.OUTPUT);
			if (line.getCompany() == null && line.getNetwork() == null) {
				log.info("Ignoring line without company or network: " + line.getObjectId());
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.INVALID_FORMAT, "no company for this line");
				return SUCCESS;
			}

			LocalDate startDate = null;
			if (configuration.getStartDate() != null) {
				startDate = TimeUtil.toLocalDate(configuration.getStartDate());
			}

			LocalDate endDate = null;
			if (configuration.getEndDate() != null) {
				endDate = TimeUtil.toLocalDate(configuration.getEndDate());
			}

			GtfsDataCollector collector = new GtfsDataCollector(collection, line, startDate, endDate);
			boolean cont = collector.collect();
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 0);
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.JOURNEY_PATTERN,
					collection.getJourneyPatterns().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, collection
					.getRoutes().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,
					collection.getVehicleJourneys().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.INTERCHANGE,
					collection.getInterchanges().size());

			if (cont) {
				context.put(EXPORTABLE_DATA, collection);

				saveLine(context, line);
				reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
				result = SUCCESS;
			} else {
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.NO_DATA_ON_PERIOD, "no data on period");
				result = SUCCESS; // else export will stop here
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	private boolean saveLine(Context context,

	Line line) {
		Metadata metadata = (Metadata) context.get(METADATA);
		GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
		GtfsServiceProducer calendarProducer = new GtfsServiceProducer(exporter);
		GtfsTripProducer tripProducer = new GtfsTripProducer(exporter);
		GtfsRouteProducer routeProducer = new GtfsRouteProducer(exporter);
		GtfsShapeProducer shapeProducer = new GtfsShapeProducer(exporter);

		GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
		String prefix = configuration.getObjectIdPrefix();
		String sharedPrefix = prefix;
		ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		Map<String, List<Timetable>> timetables = collection.getTimetableMap();
		Set<JourneyPattern> jps = new HashSet<JourneyPattern>();

		boolean hasLine = false;
		boolean hasVj = false;
		// utiliser la collection
		if (!collection.getVehicleJourneys().isEmpty()) {
			for (VehicleJourney vj : collection.getVehicleJourneys()) {

				if (vj.hasTimetables() && vj.isNeitherCancelledNorReplaced()) {
					String timeTableServiceId = calendarProducer.key(vj.getTimetables(), sharedPrefix, configuration.isKeepOriginalId());
					if (timeTableServiceId != null) {
						if (tripProducer.save(vj, timeTableServiceId, prefix, sharedPrefix, configuration.isKeepOriginalId())) {
							hasVj = true;
							jps.add(vj.getJourneyPattern());
							if (!timetables.containsKey(timeTableServiceId)) {
								timetables.put(timeTableServiceId, new ArrayList<Timetable>(vj.getTimetables()));
							}
						}
					}
				} else if(vj.hasDatedServiceJourneys()
				   && vj.getDatedServiceJourneys().stream().anyMatch(DatedServiceJourney::isNeitherCancelledNorReplaced)) {
					if (tripProducer.save(vj, vj.getObjectId(), prefix, sharedPrefix, configuration.isKeepOriginalId())) {
						hasVj = true;
						jps.add(vj.getJourneyPattern());
					}
				}

			} // vj loop
			for (JourneyPattern jp : jps) {
				shapeProducer.save(jp, prefix, configuration.isKeepOriginalId());
			}
			if (hasVj) {
				routeProducer.save(line, prefix, configuration.isKeepOriginalId(),configuration.isUseTpegHvt());
				hasLine = true;
				if (metadata != null) {
					metadata.getResources().add(
                            new Metadata.Resource(NeptuneObjectPresenter.getName(line.getNetwork()),
                                    NeptuneObjectPresenter.getName(line)));
				}
			}
		}
		return hasLine;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsLineProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(GtfsLineProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
