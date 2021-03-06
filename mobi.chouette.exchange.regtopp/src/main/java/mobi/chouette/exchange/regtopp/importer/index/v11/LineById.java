package mobi.chouette.exchange.regtopp.importer.index.v11;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.regtopp.importer.RegtoppImporter;
import mobi.chouette.exchange.regtopp.importer.index.Index;
import mobi.chouette.exchange.regtopp.importer.index.IndexFactory;
import mobi.chouette.exchange.regtopp.importer.index.IndexImpl;
import mobi.chouette.exchange.regtopp.importer.parser.FileContentParser;
import mobi.chouette.exchange.regtopp.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppLineLIN;
import mobi.chouette.exchange.regtopp.validation.RegtoppException;
import mobi.chouette.exchange.regtopp.validation.RegtoppException.ERROR;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;
import org.apache.commons.lang.StringUtils;

import static mobi.chouette.exchange.regtopp.messages.RegtoppMessages.getMessage;

@Log4j
public class LineById extends IndexImpl<RegtoppLineLIN> {

	public LineById(Context context, RegtoppValidationReporter validationReporter, FileContentParser<RegtoppLineLIN> fileParser) throws Exception {
		super(context, validationReporter, fileParser);
	}

	@Override
	public boolean validate(RegtoppLineLIN bean, RegtoppImporter dao) {
		boolean result = true;

		if (StringUtils.trimToNull(bean.getName()) != null) {
			bean.getOkTests().add(RegtoppException.ERROR.LIN_INVALID_FIELD_VALUE);
		} else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(getUnderlyingFilename(), bean.getRecordLineNumber(), getMessage("label.regtoppLineLIN.name"), bean.getName(), RegtoppException.ERROR.LIN_INVALID_FIELD_VALUE, getMessage("label.validation.invalidFieldValue"))));
			result = false;
		}

		return result;
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(Context context, RegtoppValidationReporter validationReporter, FileContentParser parser) throws Exception {
			return new LineById(context, validationReporter, parser);
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(LineById.class.getName(), factory);
	}

	@Override
	public void index() throws Exception {
		
		for (Object obj : parser.getRawContent()) {
			RegtoppLineLIN newRecord = (RegtoppLineLIN) obj;
			RegtoppLineLIN existingRecord = index.put(newRecord.getLineId(), newRecord);
			if (existingRecord != null) {
				log.warn("Duplicate key in LIN file. Existing: "+existingRecord+" Ignored duplicate: "+newRecord);
				validationReporter.reportError(context, new RegtoppException(new FileParserValidationError(getUnderlyingFilename(),
						newRecord.getRecordLineNumber(), getMessage("label.regtoppLineLIN.lineId"), newRecord.getLineId(), ERROR.LIN_DUPLICATE_KEY, getMessage("label.validation.duplicateKeyError"))), getUnderlyingFilename());
			}
		}
	}
}
