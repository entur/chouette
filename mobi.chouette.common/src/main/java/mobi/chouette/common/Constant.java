package mobi.chouette.common;

public interface Constant {

	boolean ERROR = false;
	boolean SUCCESS = true;

	String INITIAL_CONTEXT = "initial_context";
	String BASE_URI = "base_uri";
	String JOB_ID = "job_id";
	String JOB_DATA = "job_data";
	String ROOT_PATH = "referentials";
	String CONFIGURATION = "configuration";
	String VALIDATION = "validation";
	String SOURCE = "source";
	String SOURCE_FILE = "source_file";
	String SOURCE_DATABASE = "source_database";
	String CODE_SPACE = "code_space";

	String OPTIMIZED = "optimized";
	String COPY_IN_PROGRESS = "copy_in_progress";
	String FILE_URL = "file_url";
	String FILE_NAME = "file_name";
	String SCHEMA = "schema";
	String IMPORTER = "importer";
	String EXPORTER = "exporter";
	String VALIDATOR = "validator";
	String INPUT = "input";
	String OUTPUT = "output";
	String PARAMETERS_FILE = "parameters.json";
	String ACTION_PARAMETERS_FILE = "action_parameters.json";
	String VALIDATION_PARAMETERS_FILE = "validation_parameters.json";
	String REPORT = "report";
	String SAVE_MAIN_VALIDATION_REPORT = "save_main_validation_report";
	String VALIDATION_REPORT = "validation_report";
	String REPORT_FILE = "action_report.json";
	String VALIDATION_FILE = "validation_report.json";
	String CANCEL_ASKED = "cancel_asked";
	String COMMAND_CANCELLED = "command_cancelled";

	String COLUMN_NUMBER = "column_number";
	String LINE_NUMBER = "line_number";
	// public static final String OBJECT_LOCALISATION = "object_localisation";
    String VALIDATION_CONTEXT = "validation_context";

	String REFERENTIAL = "referential";
	String CACHE = "cache";
	String PARSER = "parser";
	String AREA_BLOC = "area_bloc";
	String CONNECTION_LINK_BLOC = "connection_link_bloc";

	
	String VALIDATION_DATA = "validation_data";
	String EXPORTABLE_DATA = "exportable_data";
	String SHARED_DATA_KEYS = "shared_data_keys";
	String SHARED_DATA = "shared_data";
	String METADATA = "metadata";
	String LINE = "line";
	String LINE_ID = "line_id";

	char SEP = '|';
	String NULL = "\\N";
	
	String BUFFER = "buffer";

	String REFERENTIAL_LAST_UPDATE_TIMESTAMP = "REFERENTIAL_LAST_UPDATE_TIMESTAMP";
	
}
