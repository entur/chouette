package mobi.chouette.exchange.neptune.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.exporter.DataCollector;
import mobi.chouette.model.Line;

import java.time.LocalDate;

@Log4j
public class NeptuneDataCollector extends DataCollector {
	public NeptuneDataCollector(mobi.chouette.exchange.exporter.ExportableData collection, Line line, LocalDate startDate, LocalDate endDate) {
		super(collection, line, startDate, endDate, false, false, true);
	}

	public boolean collect() {
		boolean res =  super.collect();
		if (line.getNetwork() == null) {
			log.error("line " + line.getObjectId() + " : missing network");
			return false;
		}
		if (line.getCompany() == null) {
			log.error("line " + line.getObjectId() + " : missing company");
			return false;
		}
		return res;
	}



}
