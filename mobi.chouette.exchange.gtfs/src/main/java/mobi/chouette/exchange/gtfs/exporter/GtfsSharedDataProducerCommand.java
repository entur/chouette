/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.naming.InitialContext;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsAgencyProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsServiceProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsStopProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsTransferProducer;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Company;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.DatedServiceJourney;
import mobi.chouette.model.Interchange;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.Timetable;

/**
 *
 */
@Log4j
public class GtfsSharedDataProducerCommand implements Command, Constant {
	public static final String COMMAND = "GtfsSharedDataProducerCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		try {

			ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
			if (collection == null) {
				return ERROR;
			}

			saveData(context);
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.COMPANY, "companies", OBJECT_STATE.OK,
					IO_TYPE.OUTPUT);
			reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.COMPANY, OBJECT_TYPE.COMPANY, collection
					.getAgencyCompanies().size());
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.CONNECTION_LINK, "connection links",
					OBJECT_STATE.OK, IO_TYPE.OUTPUT);
			reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.CONNECTION_LINK, OBJECT_TYPE.CONNECTION_LINK,
					collection.getConnectionLinks().size());
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, "stop areas", OBJECT_STATE.OK,
					IO_TYPE.OUTPUT);
			reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, OBJECT_TYPE.STOP_AREA, collection
					.getCommercialStops().size() + collection.getPhysicalStops().size());
			reporter.addObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, "calendars", OBJECT_STATE.OK,
					IO_TYPE.OUTPUT);
			reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, OBJECT_TYPE.TIMETABLE, collection
					.getTimetables().size());
			result = SUCCESS;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	private void saveData(Context context) {
		Metadata metadata = (Metadata) context.get(METADATA);
		GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
		GtfsStopProducer stopProducer = new GtfsStopProducer(exporter);
		GtfsTransferProducer transferProducer = new GtfsTransferProducer(exporter);
		GtfsAgencyProducer agencyProducer = null;
		GtfsServiceProducer calendarProducer = null;

		GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
		TimeZone timezone = TimeZone.getTimeZone(configuration.getTimeZone());
		String prefix = configuration.getObjectIdPrefix();
		String sharedPrefix = prefix;
		ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		Map<String, List<Timetable>> timetables = collection.getTimetableMap();
		List<DatedServiceJourney> datedServiceJourneys = collection.getDatedServiceJourneys();
		Set<StopArea> commercialStops = collection.getCommercialStops();
		Set<StopArea> physicalStops = collection.getPhysicalStops();
		Set<ConnectionLink> connectionLinks = collection.getConnectionLinks();
		// Only export companies (agencies) actually referred to by routes.
		Set<Company> companies = collection.getAgencyCompanies();
		Set<Interchange> interchanges = collection.getInterchanges();
		if (!companies.isEmpty()) {
			agencyProducer = new GtfsAgencyProducer(exporter);
		}
		if (!timetables.isEmpty() || !datedServiceJourneys.isEmpty()) {
			calendarProducer = new GtfsServiceProducer(exporter);
		}

		for (Iterator<StopArea> iterator = commercialStops.iterator(); iterator.hasNext();) {
			StopArea stop = iterator.next();
			if (!stopProducer.save(stop, sharedPrefix, null, configuration.isKeepOriginalId(),configuration.isUseTpegHvt())) {
				iterator.remove();
			} else {
				if (metadata != null && stop.hasCoordinates())
					metadata.getSpatialCoverage().update(stop.getLongitude().doubleValue(),
							stop.getLatitude().doubleValue());
			}
		}
		for (StopArea stop : physicalStops) {
			stopProducer.save(stop, sharedPrefix, commercialStops, configuration.isKeepOriginalId(),configuration.isUseTpegHvt());
			if (metadata != null && stop.hasCoordinates())
				metadata.getSpatialCoverage().update(stop.getLongitude().doubleValue(),
						stop.getLatitude().doubleValue());
		}
		// remove incomplete connectionlinks
		for (ConnectionLink link : connectionLinks) {
			if (!physicalStops.contains(link.getStartOfLink()) && !commercialStops.contains(link.getStartOfLink())) {
				continue;
			} else if (!physicalStops.contains(link.getEndOfLink()) && !commercialStops.contains(link.getEndOfLink())) {
				continue;
			}
			transferProducer.save(link, sharedPrefix, configuration.isKeepOriginalId());
		}
		
		// produce interchanges
		for(Interchange interchange : interchanges) {
			if (isInterchangeValid(interchange)) {
				transferProducer.save(interchange, sharedPrefix, configuration.isKeepOriginalId());
			}
		}

		for (Company company : companies) {
			agencyProducer.save(company, prefix, timezone, configuration.isKeepOriginalId());
		}

		for (List<Timetable> tms : timetables.values()) {
			calendarProducer.save(tms, sharedPrefix, configuration.isKeepOriginalId());
			if (metadata != null) {
				for (Timetable tm : tms) {
					metadata.getTemporalCoverage().update(tm.getStartOfPeriod(), tm.getEndOfPeriod());
				}
			}
		}

		for (DatedServiceJourney datedServiceJourney : datedServiceJourneys) {
			// replaced and cancelled services are excluded from the GTFS export.
			if (datedServiceJourney.isNeitherCancelledNorReplaced()) {
				CalendarDay calendarDay = new CalendarDay();
				calendarDay.setDate(datedServiceJourney.getOperatingDay());
				calendarDay.setIncluded(true);
				calendarProducer.saveDay(datedServiceJourney.getVehicleJourney().getObjectId(), calendarDay);
			}
		}

	}

	/**
	 * Interchange relations are not enforced in db. Make sure they are valid before exporting.
	 */
	private boolean isInterchangeValid(Interchange interchange) {
		return interchange.getConsumerVehicleJourney() != null
				&& interchange.getFeederVehicleJourney() != null
				&& interchange.getConsumerStopPoint() != null
				&& interchange.getFeederStopPoint() != null
				&& interchange.getConsumerVehicleJourney().isNeitherCancelledNorReplaced()
				&& interchange.getFeederVehicleJourney().isNeitherCancelledNorReplaced();
	}


	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsSharedDataProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(GtfsSharedDataProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
