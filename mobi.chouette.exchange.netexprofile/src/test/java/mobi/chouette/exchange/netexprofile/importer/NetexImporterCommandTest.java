package mobi.chouette.exchange.netexprofile.importer;

import mobi.chouette.common.Context;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.netexprofile.DummyChecker;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.NetexTestUtils;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.FileReport;
import mobi.chouette.exchange.report.ObjectReport;
import mobi.chouette.exchange.validation.report.CheckPointReport;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.*;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static mobi.chouette.exchange.report.ReportConstant.STATUS_OK;
import static org.testng.Assert.assertEquals;

public class NetexImporterCommandTest extends Arquillian implements mobi.chouette.common.Constant {

	private InitialContext initialContext;

	private void init() {
		if (initialContext == null) {
			try {
				initialContext = new InitialContext();

			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
	}

	@EJB
	LineDAO lineDao;

	@EJB
	RouteDAO routeDao;

	@EJB
	VehicleJourneyDAO vjDao;

	@PersistenceContext(unitName = "referential")
	EntityManager em;

	@Inject
	UserTransaction utx;

	@Deployment
	public static WebArchive createDeploymentOld() {

		WebArchive result;

		File[] files = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.exchange.netexprofile:3.3.1", "mobi.chouette:mobi.chouette.dao:3.3.1","mobi.chouette:mobi.chouette.exchange:3.3.1")
				.withTransitivity().asFile();

		result = ShrinkWrap.create(WebArchive.class, "test.war")
				.addAsWebInfResource("postgres-ds.xml")
			    .addAsLibraries(files)
				.addClass(DummyChecker.class)
				.addClass(NetexTestUtils.class)
				.addClass(JobDataTest.class)
				.addAsResource(EmptyAsset.INSTANCE, "beans.xml");
		return result;

	}

	protected Context initImportContext() {
		init();
		ContextHolder.setContext("chouette_gui"); // set tenant schema

		Context context = new Context();
		context.put(INITIAL_CONTEXT, initialContext);
		context.put(REPORT, new ActionReport());
		context.put(VALIDATION_REPORT, new ValidationReport());
		NetexprofileImportParameters configuration = new NetexprofileImportParameters();
		context.put(CONFIGURATION, configuration);
		configuration.setName("name");
		configuration.setUserName("userName");
		configuration.setNoSave(true);
		configuration.setCleanRepository(true);
		configuration.setOrganisationName("organisation");
		configuration.setReferentialName("test");
		JobDataTest jobData = new JobDataTest();
		context.put(JOB_DATA, jobData);
		jobData.setPathName("target/referential/test");
		File f = new File("target/referential/test");
		if (f.exists())
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		f.mkdirs();
		jobData.setReferential("chouette_gui");
		jobData.setAction(IMPORTER);
		jobData.setType("netexprofile");
		context.put("testng", "true");
		context.put(OPTIMIZED, Boolean.FALSE);
		return context;

	}

	@Test(enabled = false) // Disabled due to jaxb class loading issues (works when deployed normally, just not inside arquillian/embedded jboss)
	public void importAvinor() throws Exception {
		// Prepare context
		Context context = initImportContext();

		File f = new File("src/test/data/avinor.zip");
		File dest = new File("target/referential/test");
		FileUtils.copyFileToDirectory(f, dest);
		JobDataTest job = (JobDataTest) context.get(JOB_DATA);
		job.setInputFilename(f.getName());

		NetexprofileImporterCommand command = (NetexprofileImporterCommand) CommandFactory.create(initialContext, NetexprofileImporterCommand.class.getName());

		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
		parameters.setNoSave(false);

		boolean result = command.execute(context);
		dumpReports(context);

		ActionReport report = (ActionReport) context.get(REPORT);
		ValidationReport validationReport = (ValidationReport) context.get(VALIDATION_REPORT);
		assertActionReport(report, STATUS_OK, 8, 1);
		assertStats(report, 1, 9);
		assertLine(report, ActionReporter.OBJECT_STATE.OK);
		assertValidationReport(validationReport, 14, 1, 40);

		// line should be saved
		utx.begin();
		em.joinTransaction();
		Line line = lineDao.findByObjectId("TST:Line:2306-2016-03-29");

		Assert.assertNotNull(line, "Line not found");
		Assert.assertNotNull(line.getNetwork(), "line must have a network");
		Assert.assertNotNull(line.getCompany(), "line must have a company");
		Assert.assertNotNull(line.getRoutes(), "line must have routes");
		assertEquals(line.getRoutes().size(), 9, "number of routes");
		Set<StopArea> bps = new HashSet<StopArea>();

		int numStopPoints = 0;
		int numVehicleJourneys = 0;
		int numJourneyPatterns = 0;

		for (Route route : line.getRoutes()) {
			Assert.assertNotNull(route.getName(), "No route name");

			Assert.assertNotEquals(route.getJourneyPatterns().size(), 0, "line routes must have journeyPattens");
			for (JourneyPattern jp : route.getJourneyPatterns()) {
				Assert.assertNotNull(jp.getName(), "No journeypattern name");
				Assert.assertNotEquals(jp.getStopPoints().size(), 0, "line journeyPattens must have stoppoints");
				for (StopPoint point : jp.getStopPoints()) {

					numStopPoints++;
					Assert.assertNotNull(point.getContainedInStopArea(), "stoppoints must have StopAreas");
					bps.add(point.getContainedInStopArea());

					Assert.assertNotNull(point.getForAlighting(), "no alighting info StopPoint=" + point);
					Assert.assertNotNull(point.getForBoarding(), "no boarding info StopPoint=" + point);

				}
				Assert.assertNotEquals(jp.getVehicleJourneys().size(), 0, " journeyPattern should have VehicleJourneys");
				for (VehicleJourney vj : jp.getVehicleJourneys()) {
					Assert.assertNotEquals(vj.getTimetables().size(), 0, " vehicleJourney should have timetables");
					assertEquals(vj.getVehicleJourneyAtStops().size(), jp.getStopPoints().size(),
							" vehicleJourney should have correct vehicleJourneyAtStop count");
					numVehicleJourneys++;
				}
				numJourneyPatterns++;
			}
		}

		assertEquals(numJourneyPatterns, 9, "number of journeyPatterns");
		assertEquals(numVehicleJourneys, 12, "number of vehicleJourneys");
		assertEquals(numStopPoints, 411, "number of stopPoints in journeyPattern");
		assertEquals(bps.size(), 90, "number boarding positions");

		// Check opposite routes
		Route outbound = routeDao.findByObjectId("TST:Route:2306103-2016-03-29");
		Route inbound = routeDao.findByObjectId("TST:Route:2306203-2016-03-29");

		Assert.assertNotNull(outbound, "Outbound route not found");
		Assert.assertNotNull(inbound, "Inbound route not found");

		Assert.assertNotNull(outbound.getOppositeRoute(), "Oppsite route to outbound not found");
		Assert.assertNotNull(inbound.getOppositeRoute(), "Oppsite route to inbound not found");

		Assert.assertTrue(outbound.getOppositeRoute().equals(inbound), "Opposite route incorrect");

		utx.rollback();

		Assert.assertTrue(result, "Importer command execution failed: " + report.getFailure());
	}

	private void assertGlobalLines(ActionReport report, int lines) {
		assertEquals(report.findObjectReport("global", ActionReporter.OBJECT_TYPE.LINE).getStats().get(ActionReporter.OBJECT_TYPE.LINE).intValue(), lines, "lines reported");
	}

	private void assertLine(ActionReport report, ActionReporter.OBJECT_STATE state) {
		assertEquals(report.getObjects().get(ActionReporter.OBJECT_TYPE.LINE).getStatus(), state);
	}

	private void assertStats(ActionReport report, int lines, int routes) {
		assertGlobalLines(report, lines);
		assertGlobalRoutes(report, routes);
	}

	private void assertGlobalRoutes(ActionReport report, int routes) {
		assertEquals(report.findObjectReport("global", ActionReporter.OBJECT_TYPE.ROUTE).getStats().get(ActionReporter.OBJECT_TYPE.ROUTE).intValue(), routes, "routes reported");
	}


	private void assertFiles(ActionReport report, int files) {
		assertEquals(report.getFiles().size(), files, "files reported");
	}

	private void assertStatus(ActionReport report, String status) {
		assertEquals(report.getResult(), status, "result");
	}

	private void assertValidationReport(ValidationReport validationReport, int expectedOk, int expectedNok, int expectedUncheck) {
		assertWarningOrOk(validationReport.getResult());
		int actualOk = 0;
		int actualNok = 0;
		int actualUncheck = 0;
		for (CheckPointReport checkPointReport : validationReport.getCheckPoints()) {
			if (checkPointReport.getState().equals(ValidationReporter.RESULT.OK)) {
				actualOk++;
			} else if (checkPointReport.getState().equals(ValidationReporter.RESULT.NOK)) {
				actualNok++;
			} else if (checkPointReport.getState().equals(ValidationReporter.RESULT.UNCHECK)) {
				actualUncheck++;
			}
		}
		assertEquals(actualOk, expectedOk, "ok");
		assertEquals(actualNok, expectedNok, "nok");
		assertEquals(actualUncheck, expectedUncheck, "uncheck");
	}

	private void assertWarningOrOk(ValidationReporter.VALIDATION_RESULT result) {
		if (result.equals(ValidationReporter.VALIDATION_RESULT.ERROR) || result.equals(ValidationReporter.VALIDATION_RESULT.NO_PROCESSING)){
			throw new AssertionError("Validation failed. Got " + result);
		}
	}

	private void assertActionReport(ActionReport report, String status, int files, int lines) {
		assertEquals(report.getResult(), status, "result");
		assertEquals(report.getFiles().size(), files, "file reported");
		assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), lines, "line reported");
	}

	public void dumpReports(Context context) {
		ActionReport actionReport = (ActionReport) context.get(REPORT);

		ReflectionToStringBuilder builder = new ReflectionToStringBuilder(actionReport, ToStringStyle.MULTI_LINE_STYLE, null, null, true, true);
		builder.setExcludeFieldNames(new String[] { "files", "lines" });
		System.out.println(builder.toString());
		for (FileReport fileReport : actionReport.getFiles()) {
			String toString = ToStringBuilder.reflectionToString(fileReport, ToStringStyle.SHORT_PREFIX_STYLE, true);
			if (fileReport.getStatus() == ActionReporter.FILE_STATE.ERROR) {
				logError(toString);
			} else {
				logOk(System.out, toString);
			}
		}
		for (ObjectReport object : actionReport.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
			String toString = ToStringBuilder.reflectionToString(object, ToStringStyle.SHORT_PREFIX_STYLE, true);
			if (object.getStatus() == ActionReporter.OBJECT_STATE.ERROR) {
				logError(toString);
			} else {
				logOk(System.out, toString);
			}
		}

		ValidationReport validationReport = (ValidationReport) context.get(VALIDATION_REPORT);
		ReflectionToStringBuilder validationBuilder = new ReflectionToStringBuilder(validationReport, ToStringStyle.SHORT_PREFIX_STYLE, null, null, true, true);
		validationBuilder.setExcludeFieldNames(new String[] { "checkPoints" });
		System.out.println(validationBuilder.toString());

		for (CheckPointReport object : validationReport.getCheckPoints()) {
			ReflectionToStringBuilder checkpointBuilder = new ReflectionToStringBuilder(object, ToStringStyle.SHORT_PREFIX_STYLE, null, null, true, true);
			checkpointBuilder.setExcludeFieldNames(new String[] { "details" });
			String checkpointAsString = checkpointBuilder.toString();

			List<String> lines = new ArrayList<String>();
			lines.add(checkpointAsString);

			if (object.getState() == ValidationReporter.RESULT.NOK) {
				logError(lines.toArray(new String[0]));
			} else {
				logOk(System.out, lines.toArray(new String[0]));
			}
		}
	}

	private void logError(String... data) {
		logOk(System.err, data);
	}

	private void logOk(PrintStream stream, String... data) {
		for (String d : data) {
			stream.println(d);
		}

	}

}
