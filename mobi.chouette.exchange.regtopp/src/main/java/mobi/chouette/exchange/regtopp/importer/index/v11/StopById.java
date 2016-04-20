package mobi.chouette.exchange.regtopp.importer.index.v11;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.regtopp.importer.RegtoppImporter;
import mobi.chouette.exchange.regtopp.importer.index.Index;
import mobi.chouette.exchange.regtopp.importer.index.IndexFactory;
import mobi.chouette.exchange.regtopp.importer.index.IndexImpl;
import mobi.chouette.exchange.regtopp.importer.parser.FileContentParser;
import mobi.chouette.exchange.regtopp.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.AbstractRegtoppStopHPL;
import mobi.chouette.exchange.regtopp.validation.RegtoppException;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;

@Log4j
public class StopById extends IndexImpl<AbstractRegtoppStopHPL> {

	public StopById(RegtoppValidationReporter validationReporter, FileContentParser fileParser) throws Exception {
		super(validationReporter, fileParser);
	}

	@Override
	public boolean validate(AbstractRegtoppStopHPL bean, RegtoppImporter dao) {
		boolean result = true;

		//Obligatoriske felter
		if (bean.getAdminCode() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Adminkode", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getCounter() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Løpenr", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getStopId() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Holdeplassnr", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getFullName() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Fullstendig navn", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getInterchangeType() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Type", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getInterchangeMinutes() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Spes. omstigningstid", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		if (bean.getCoachClass() != null) {
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		}  else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Klasse", null, RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}


		//Holdeplass nr er riktig bygd opp (kommunenummer + 4 siffer)
		String stopId = bean.getStopId();
		String municipalityCodeString = stopId.substring(0, 4);
		String stopSequenceNumber = stopId.substring(4, 8);
		int municipalityCode = Integer.valueOf(municipalityCodeString);
		if (101 <= municipalityCode && municipalityCode <= 2211 && stopSequenceNumber.matches("\\d{4}")) {		//Lots of holes in the 101-2211 range
			bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
		} else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Holdeplassnr", bean.getStopId(), RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
			result = false;
		}

		// Koordinater ulike
		if (bean.getX() != null && bean.getY() != null){
			if (!bean.getX().equals(bean.getY())) {
				bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
			} else {
				bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "X = Y", bean.getX().toString(), RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
				result = false;
			}
		}

		// Sone 1 og 2 forskjellige
		if (bean.getZoneId1() != null && bean.getZoneId2() != null) {
			if (!bean.getZoneId1().equals(bean.getZoneId2())) {
				bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
			} else {
				bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Zone id 1 = Zone id 2", bean.getZoneId1().toString(), RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
				result = false;
			}
		}

		// Fullstendig navn !§= kortnavn
		if (bean.getShortName() != null) {
			if (!bean.getFullName().equals(bean.getShortName())) {
				bean.getOkTests().add(RegtoppException.ERROR.INVALID_FIELD_VALUE);
			} else {
				bean.getErrors().add(new RegtoppException(new FileParserValidationError(AbstractRegtoppStopHPL.FILE_EXTENSION, bean.getRecordLineNumber(), "Full name = short name", bean.getFullName(), RegtoppException.ERROR.INVALID_FIELD_VALUE, "")));
				result = false;
			}
		}

		return result;
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(RegtoppValidationReporter validationReporter, FileContentParser parser) throws Exception {
			return new StopById(validationReporter, parser);
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(StopById.class.getName(), factory);
	}

	@Override
	public void index() throws Exception {
		for (Object obj : parser.getRawContent()) {
			AbstractRegtoppStopHPL stop = (AbstractRegtoppStopHPL) obj;
			AbstractRegtoppStopHPL existing = index.put(stop.getFullStopId(), stop);
			if (existing != null) {
				// TODO fix exception/validation reporting
				validationReporter.reportError(new Context(), new RegtoppException(new FileParserValidationError()), null);
			}
		}
	}
}
