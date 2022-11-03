package mobi.chouette.exchange.validation.checkpoint;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.exchange.validation.parameters.TransportModeParameters;
import mobi.chouette.exchange.validation.parameters.ValidationParameters;
import mobi.chouette.exchange.validation.report.CheckPointErrorReport;
import mobi.chouette.exchange.validation.report.CheckPointReport;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.exchange.validator.DummyChecker;
import mobi.chouette.exchange.validator.JobDataTest;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.TransportModeNameEnum;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

@Slf4j
public class ValidationJourneyPatterns extends AbstractTestValidation {
	private JourneyPatternCheckPoints checkPoint = new JourneyPatternCheckPoints();
	private ValidationParameters fullparameters;
	private JourneyPattern bean1;
	private JourneyPattern bean2;
	private List<JourneyPattern> beansFor4 = new ArrayList<>();

	@EJB 
	LineDAO lineDao;

	@PersistenceContext(unitName = "referential")
	EntityManager em;

	@Inject
	UserTransaction utx;

	@Deployment
	public static EnterpriseArchive createDeployment() {

		EnterpriseArchive result;
		File[] files = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.exchange.validator").withTransitivity().asFile();
		List<File> jars = new ArrayList<>();
		List<JavaArchive> modules = new ArrayList<>();
		for (File file : files) {
			if (file.getName().startsWith("mobi.chouette.exchange"))
			{
				String name = file.getName().split("\\-")[0]+".jar";
				JavaArchive archive = ShrinkWrap
						  .create(ZipImporter.class, name)
						  .importFrom(file)
						  .as(JavaArchive.class);
				modules.add(archive);
			}
			else
			{
				jars.add(file);
			}
		}
		File[] filesDao = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.dao").withTransitivity().asFile();
		if (filesDao.length == 0) 
		{
			throw new NullPointerException("no dao");
		}
		for (File file : filesDao) {
			if (file.getName().startsWith("mobi.chouette.dao"))
			{
				String name = file.getName().split("\\-")[0]+".jar";
				
				JavaArchive archive = ShrinkWrap
						  .create(ZipImporter.class, name)
						  .importFrom(file)
						  .as(JavaArchive.class);
				modules.add(archive);
				if (!modules.contains(archive))
				   modules.add(archive);
			}
			else
			{
				if (!jars.contains(file))
				   jars.add(file);
			}
		}
		final WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
				.addClass(DummyChecker.class)
				.addClass(JobDataTest.class)
				.addClass(AbstractTestValidation.class)
				.addClass(ValidationJourneyPatterns.class);
		
		result = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
				.addAsLibraries(jars.toArray(new File[0]))
				.addAsModules(modules.toArray(new JavaArchive[0]))
				.addAsModule(testWar)
				.addAsResource(EmptyAsset.INSTANCE, "beans.xml");
		return result;
	}

	@BeforeGroups(groups = { "journeyPattern" })
	public void initTest() {
		super.init();

		long id = 1;

		fullparameters = null;
		try {
			fullparameters = loadFullParameters();
			fullparameters.setCheckJourneyPattern(1);

			Line line = new Line();
			line.setId(id++);
			line.setObjectId("test1:Line:1");
			line.setName("test");
			Route route = new Route();
			route.setId(id++);
			route.setObjectId("test1:Route:1");
			route.setName("test1");
			route.setLine(line);
			bean1 = new JourneyPattern();
			bean1.setId(id++);
			bean1.setObjectId("test1:JourneyPattern:1");
			bean1.setName("test1");
			bean1.setRoute(route);
			bean2 = new JourneyPattern();
			bean2.setId(id++);
			bean2.setObjectId("test2:JourneyPattern:1");
			bean2.setName("test2");
			bean2.setRoute(route);

			beansFor4.add(bean1);
			beansFor4.add(bean2);
		} catch (Exception e) {
			fullparameters = null;
			e.printStackTrace();
		}

	}

	@Test(groups = { "journeyPattern" }, description = "4-JourneyPattern-1 no test", priority = 1)
	public void verifyTest4_1_notest() throws Exception {
		// 4-JourneyPattern-1 : check columns
		log.info(Color.BLUE + "4-JourneyPattern-1 no test" + Color.NORMAL);
		Context context = initValidatorContext();
		Assert.assertNotNull(fullparameters, "no parameters for test");
		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		fullparameters.setCheckJourneyPattern(0);
		ValidationData data = new ValidationData();
		data.getJourneyPatterns().addAll(beansFor4);
		context.put(VALIDATION_DATA, data);

		checkPoint.validate(context, null);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertTrue(report.findCheckPointReportByName("4-JourneyPattern-1") == null, " report must not have item 4-JourneyPattern-1");

		fullparameters.setCheckJourneyPattern(1);

		context.put(VALIDATION_REPORT, new ValidationReport());

		checkPoint.validate(context, null);
		report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertTrue(report.findCheckPointReportByName("4-JourneyPattern-1") != null, " report must have item 4-JourneyPattern-1");
		Assert.assertEquals(report.findCheckPointReportByName("4-JourneyPattern-1").getCheckPointErrorCount(), 0,
				" checkpoint must have no detail");

	}

	@Test(groups = { "journeyPattern" }, description = "4-JourneyPattern-1 unicity", priority = 2)
	public void verifyTest4_1_unique() throws Exception {
		// 4-JourneyPattern-1 : check columns
		log.info(Color.BLUE + "4-JourneyPattern-1 unicity" + Color.NORMAL);
		Context context = initValidatorContext();
		Assert.assertNotNull(fullparameters, "no parameters for test");

		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		fullparameters.setCheckJourneyPattern(1);
		fullparameters.getJourneyPattern().getObjectId().setUnique(1);

		ValidationData data = new ValidationData();
		data.getJourneyPatterns().addAll(beansFor4);
		context.put(VALIDATION_DATA, data);

		checkPoint.validate(context, null);
		fullparameters.getJourneyPattern().getObjectId().setUnique(0);
		// unique
		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);

		List<CheckPointErrorReport> details = checkReportForTest(report, "4-JourneyPattern-1", 1);
		for (CheckPointErrorReport detail : details) {
			Assert.assertEquals(detail.getReferenceValue(), "ObjectId", "detail must refer column");
			Assert.assertEquals(detail.getValue(), bean2.getObjectId().split(":")[2], "detail must refer value");
		}
	}

	@Test(groups = { "journeyPattern" }, description = "3-JourneyPattern-1", priority = 3)
	public void verifyTest3_1() throws Exception {
		// 3-JourneyPattern-1 : check if two journey patterns use same stops
		log.info(Color.BLUE + "3-JourneyPattern-1" + Color.NORMAL);
		Context context = initValidatorContext();
		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("3-JourneyPattern-1.xml", 1, 1, true);

		utx.begin();
		em.joinTransaction();

		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);

		Route route1 = null;
		
		ValidationData data = new ValidationData();
		for (Route route : line1.getRoutes()) {
			data.getJourneyPatterns().addAll(route.getJourneyPatterns());
			if (route.getJourneyPatterns().size() == 2)
			{
				route1 = route;
			}
		}

		route1.setObjectId("NINOXE:Route:checkedRoute");
		JourneyPattern jp1 = route1.getJourneyPatterns().get(0);
		
		jp1.setObjectId("NINOXE:JourneyPattern:checkedJP");

		context.put(VALIDATION_DATA, data);

		data.getRoutes().addAll(line1.getRoutes());

		checkPoint.validate(context, null);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 0, " report must have items");
		
		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-JourneyPattern-1");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-JourneyPattern-1 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		Assert.assertEquals(checkPointReport.getSeverity(), CheckPointReport.SEVERITY.WARNING,
				" checkPointReport must be on level warning");
		Assert.assertEquals(checkPointReport.getCheckPointErrorCount(), 1, " checkPointReport must have 1 item");

		// check detail keys
		String detailKey = "3-JourneyPattern-1".replaceAll("-", "_").toLowerCase();
		List<CheckPointErrorReport> details = checkReportForTest(report,"3-JourneyPattern-1",-1);
	
		for (CheckPointErrorReport detail : details) {
			Assert.assertTrue(detail.getKey().startsWith(detailKey),
					"details key should start with test key : expected " + detailKey + ", found : " + detail.getKey());
			Assert.assertEquals(detail.getSource().getObjectId(), jp1.getObjectId(),
					"jp 1 must be source of error");
		}
		utx.rollback();

	}
	
	private void createLineRouteSection() throws Exception {
		utx.begin();
		em.joinTransaction();

		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);
		createRouteSection(line1);
		
		utx.commit();
	}
	
	@Test(groups = { "journeyPattern" }, description = "3-JourneyPattern-2", priority = 4, enabled = false)
	public void verifyTestJourneyPattern_3_2() throws Exception {
		// 3-RouteSection-1 : Check if route section distance doesn't exceed gap as parameter
		log.info(Color.BLUE + "3-JourneyPattern-2" + Color.NORMAL);
		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("Ligne_OK.xml", 1, 1, true);

		createLineRouteSection();
		Context context = initValidatorContext();
		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		
		utx.begin();
		em.joinTransaction();
		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);
		
		JourneyPattern jp = line1.getRoutes().get(0).getJourneyPatterns().get(0);
		
		
		ValidationData data = new ValidationData();
		data.getJourneyPatterns().add(jp);
		context.put(VALIDATION_DATA, data);
		
		
		checkPoint.validate(context, null);
		
		
		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 0, " report must have items");
		
		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-JourneyPattern-2");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-JourneyPattern-2 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.OK, " checkPointReport must be ok");
		
		
		jp.getRouteSections().remove(0);
		context.put(VALIDATION_REPORT, new ValidationReport());
		checkPoint.validate(context, null);
		
		ValidationReport report2 = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report2.getCheckPoints().size(), 0, " report must have items");
		
		CheckPointReport checkPointReport2 = report2.findCheckPointReportByName("3-JourneyPattern-2");
		Assert.assertNotNull(checkPointReport2, "report must contain a 3-JourneyPattern-2 checkPoint");
		
		Assert.assertEquals(checkPointReport2.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		
		
		utx.rollback();
	}
	
	@Test(groups = { "journeyPattern" }, description = "3-RouteSection-1", priority = 5)
	public void verifyTestRouteSection_3_1() throws Exception {
		// 3-RouteSection-1 : Check if route section distance doesn't exceed gap as parameter
		log.info(Color.BLUE + "3-RouteSection-1" + Color.NORMAL);
		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("Ligne_OK.xml", 1, 1, true);

		createLineRouteSection();
		Context context = initValidatorContext();
		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		
		utx.begin();
		em.joinTransaction();
		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);
		
		JourneyPattern jp = line1.getRoutes().get(0).getJourneyPatterns().get(0);
		
		RouteSection rs = jp.getRouteSections().get(0);
		
		
		ValidationData data = new ValidationData();
		data.getJourneyPatterns().add(jp);
		context.put(VALIDATION_DATA, data);

		StopArea fromStopArea = rs.getFromScheduledStopPoint().getContainedInStopAreaRef().getObject();
		StopArea toStopArea = rs.getToScheduledStopPoint().getContainedInStopAreaRef().getObject();
		if (fromStopArea == null || toStopArea == null) {
			return;
		}

		double plotFirstLat = fromStopArea.getLatitude().doubleValue();
		double plotLastLat = toStopArea.getLatitude().doubleValue();
		double plotFirstLong = fromStopArea.getLongitude().doubleValue();
		double plotLastLong = toStopArea.getLongitude().doubleValue();
		double distance = 0, distance2 = 0;
		String modeKey = jp.getRoute().getLine().getTransportModeName().toString();
		
		TransportModeParameters parameters = AbstractValidation.getModeParameters(fullparameters, modeKey, log);
		fromStopArea.setLatitude(BigDecimal.valueOf(plotFirstLat + 0.0002));
		// Departure
		distance = AbstractValidation.quickDistanceFromCoordinates(fromStopArea.getLatitude().doubleValue(), plotFirstLat, fromStopArea.getLongitude().doubleValue(), plotFirstLong);
		parameters.setRouteSectionStopAreaDistanceMax(distance * 2);
		checkPoint.validate(context, null);
		
		
		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 0, " report must have items");
		
		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-RouteSection-2-1");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-RouteSection-2-1 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.OK, " checkPointReport must be ok");
		
		
		fromStopArea.setLatitude(BigDecimal.valueOf(plotFirstLat));
		toStopArea.setLatitude(BigDecimal.valueOf(plotLastLat + 0.0003));
		
		//Arrival
		distance2 = AbstractValidation.quickDistanceFromCoordinates(toStopArea.getLatitude().doubleValue(), plotLastLat, toStopArea.getLongitude().doubleValue(), plotLastLong);
		// If route section distance doesn't exceed gap    as parameter
		parameters.setRouteSectionStopAreaDistanceMax(distance2 / 2);
		context.put(VALIDATION_REPORT, new ValidationReport());
		checkPoint.validate(context, null);
		
		ValidationReport report2 = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report2.getCheckPoints().size(), 0, " report must have items");
		
		CheckPointReport checkPointReport2 = report2.findCheckPointReportByName("3-RouteSection-2-2");
		Assert.assertNotNull(checkPointReport2, "report must contain a 3-RouteSection-2-2 checkPoint");
		
		Assert.assertEquals(checkPointReport2.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		
		
		utx.rollback();
	}


	@Test(groups = { "journeyPattern" }, description = "3-JourneyPattern-RB-1", priority = 6)
	public void verifyTest3_rb_1() throws Exception {
		// 3-JourneyPatter-RB-1 : check distance between stops
		log.info(Color.BLUE + "3-JourneyPattern-RB-1" + Color.NORMAL);
		Context context = initValidatorContext();
		context.put(VALIDATION_REPORT, new ValidationReport());

		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("Ligne_OK.xml", 1, 1, true);

		utx.begin();
		em.joinTransaction();

		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);

		line1.setTransportModeName(TransportModeNameEnum.Bus);
		JourneyPattern jp1 = line1.getRoutes().get(0).getJourneyPatterns().get(0);

		StopArea area0 = jp1.getStopPoints().get(0).getScheduledStopPoint().getContainedInStopAreaRef().getObject();
		double distanceMin = 10000000;
		double distanceMax = 0;
		for (int i = 1; i < jp1.getStopPoints().size(); i++) {
			StopArea area1 = jp1.getStopPoints().get(i).getScheduledStopPoint().getContainedInStopAreaRef().getObject();
			double distance = distance(area0, area1);
			if (distance > distanceMax)
				distanceMax = distance;
			if (distance < distanceMin)
				distanceMin = distance;
			area0 = area1;
		}

		fullparameters.getModeBus().setInterStopAreaDistanceMin((int) distanceMin + 10);
		fullparameters.getModeBus().setInterStopAreaDistanceMax((int) distanceMax - 10);
		context.put(VALIDATION, fullparameters);

		ValidationData data = new ValidationData();
		context.put(VALIDATION_DATA, data);

		data.getJourneyPatterns().add(jp1);

		checkPoint.validate(context, null);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 0, " report must have items");

		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-JourneyPattern-rutebanken-1");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-JourneyPattern-rutebanken-1 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		Assert.assertEquals(checkPointReport.getSeverity(), CheckPointReport.SEVERITY.WARNING,
				" checkPointReport must be on level warning");
		Assert.assertEquals(checkPointReport.getCheckPointErrorCount(), 1, " checkPointReport must have 2 item");

		String detailKey = "3-JourneyPattern-rutebanken-1".replaceAll("-", "_").toLowerCase();
		List<CheckPointErrorReport> details = checkReportForTest(report,"3-JourneyPattern-rutebanken-1",-1);
		for (CheckPointErrorReport detail : details) {
			Assert.assertTrue(detail.getKey().startsWith(detailKey),
					"details key should start with test key : expected " + detailKey + ", found : " + detail.getKey());
		}
		// check detail keys
		boolean jp1objectIdFound = false;
		for (CheckPointErrorReport detailReport : details) {

			if (detailReport.getSource().getObjectId().equals(jp1.getObjectId()))
				jp1objectIdFound = true;
		}
		Assert.assertTrue(jp1objectIdFound, "detail report must refer journey pattern 1");
		utx.rollback();

	}

	@Test(groups = { "journeyPattern" }, description = "3-JourneyPattern-rb-2", priority = 7)
	public void verifyTest3_rb_2() throws Exception {
		// 3-JourneyPattern-rutebanken-2 : check if two successive stops are in same area
		log.info(Color.BLUE + "3-JourneyPattern-rb-2" + Color.NORMAL);
		Context context = initValidatorContext();
		context.put(VALIDATION, fullparameters);
		context.put(VALIDATION_REPORT, new ValidationReport());

		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("Ligne_OK.xml", 1, 1, true);

		utx.begin();
		em.joinTransaction();

		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);

		JourneyPattern jp1 = line1.getRoutes().get(0).getJourneyPatterns().get(0);
		jp1.getStopPoints().get(1).getScheduledStopPoint().setContainedInStopAreaRef(new SimpleObjectReference<>(jp1.getStopPoints().get(0).getScheduledStopPoint().getContainedInStopAreaRef().getObject()));

		ValidationData data = new ValidationData();
		context.put(VALIDATION_DATA, data);

		data.getJourneyPatterns().add(jp1);

		checkPoint.validate(context, null);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 1, " report must have items");

		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-JourneyPattern-rutebanken-2");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-JourneyPattern-rutebanken-2 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		Assert.assertEquals(checkPointReport.getSeverity(), CheckPointReport.SEVERITY.WARNING,
				" checkPointReport must be on level warning");
		Assert.assertEquals(checkPointReport.getCheckPointErrorCount(), 1, " checkPointReport must have 1 item");

		String detailKey = "3-JourneyPattern-rutebanken-2".replaceAll("-", "_").toLowerCase();
		List<CheckPointErrorReport> details = checkReportForTest(report,"3-JourneyPattern-rutebanken-2",-1);
		for (CheckPointErrorReport detail : details) {
			Assert.assertTrue(detail.getKey().startsWith(detailKey),
					"details key should start with test key : expected " + detailKey + ", found : " + detail.getKey());
		}
		// check detail keys
		for (CheckPointErrorReport detail : details) {
			Assert.assertEquals(detail.getSource().getObjectId(), jp1.getObjectId(),
					"journey pattern 1 must be source of error");
		}
		utx.rollback();
	}


	@Test(groups = { "journeyPattern" }, description = "3-JourneyPattern-rutebanken-4", priority = 8)
	public void verifyTest3_rb_4() throws Exception {
		// 3-JourneyPattern-rutebanken-4 : check if journey pattern has minimum 2 StopPoints
		log.info(Color.BLUE + "3-JourneyPattern-rutebanken-4" + Color.NORMAL);
		Context context = initValidatorContext();
		context.put(VALIDATION_REPORT, new ValidationReport());

		Assert.assertNotNull(fullparameters, "no parameters for test");

		importLines("Ligne_OK.xml", 1, 1, true);

		utx.begin();
		em.joinTransaction();

		List<Line> beans = lineDao.findAll();
		Assert.assertFalse(beans.isEmpty(), "No data for test");
		Line line1 = beans.get(0);

		JourneyPattern jp1 = line1.getRoutes().get(0).getJourneyPatterns().get(0);
		jp1.getStopPoints().forEach(stopPoint -> toString()); // Force load collection (clear was not working)
		jp1.getStopPoints().clear();

		jp1.setObjectId("NINOXE:JourneyPattern:first");

		context.put(VALIDATION, fullparameters);
		ValidationData data = new ValidationData();
		context.put(VALIDATION_DATA, data);
		data.getJourneyPatterns().add(jp1);

		checkPoint.validate(context, null);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		Assert.assertNotEquals(report.getCheckPoints().size(), 0, " report must have items");

		CheckPointReport checkPointReport = report.findCheckPointReportByName("3-JourneyPattern-rutebanken-4");
		Assert.assertNotNull(checkPointReport, "report must contain a 3-JourneyPattern-rutebanken-4 checkPoint");

		Assert.assertEquals(checkPointReport.getState(), ValidationReporter.RESULT.NOK, " checkPointReport must be nok");
		Assert.assertEquals(checkPointReport.getSeverity(), CheckPointReport.SEVERITY.ERROR,
				" checkPointReport must be on level warning");
		Assert.assertEquals(checkPointReport.getCheckPointErrorCount(), 1, " checkPointReport must have 1 item");

		String detailKey = "3-JourneyPattern-rutebanken-4".replaceAll("-", "_").toLowerCase();
		List<CheckPointErrorReport> details = checkReportForTest(report,"3-JourneyPattern-rutebanken-4",-1);
		for (CheckPointErrorReport detail : details) {
			Assert.assertTrue(detail.getKey().startsWith(detailKey),
					"details key should start with test key : expected " + detailKey + ", found : " + detail.getKey());
		}
		boolean route1objectIdFound = false;
		for (CheckPointErrorReport detailReport : details) {

			if (detailReport.getSource().getObjectId().equals(jp1.getObjectId()))
				route1objectIdFound = true;
		}
		Assert.assertTrue(route1objectIdFound, "detail report must refer journey pattern 1");
		utx.rollback();

	}

}
