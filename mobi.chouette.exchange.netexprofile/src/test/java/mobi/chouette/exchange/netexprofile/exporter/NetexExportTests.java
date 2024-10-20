package mobi.chouette.exchange.netexprofile.exporter;

import static mobi.chouette.exchange.netexprofile.NetexTestUtils.createCodespace;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import mobi.chouette.dao.BlockDAO;
import mobi.chouette.dao.JourneyFrequencyDAO;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CodespaceDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.netexprofile.DummyChecker;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.NetexTestUtils;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.FileReport;
import mobi.chouette.exchange.report.ObjectReport;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.exchange.validation.report.CheckPointReport;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.Codespace;
import mobi.chouette.model.type.StopAreaImportModeEnum;
import mobi.chouette.persistence.hibernate.ContextHolder;

@Log4j
public class NetexExportTests extends Arquillian implements Constant, ReportConstant {

    protected static InitialContext initialContext;

    @EJB
    private CodespaceDAO codespaceDao;

    @EJB
    private BlockDAO blockDao;

    @EJB
    private LineDAO lineDao;

    @EJB
    private StopAreaDAO stopAreaDao;

    @EJB
    private JourneyFrequencyDAO journeyFrequencyDAO;

    @PersistenceContext(unitName = "referential")
    private EntityManager em;

    @Inject
    UserTransaction utx;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        EnterpriseArchive result;

        File[] files = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("mobi.chouette:mobi.chouette.exchange.netexprofile")
                .withTransitivity()
                .asFile();

        List<File> jars = new ArrayList<>();
        List<JavaArchive> modules = new ArrayList<>();

        for (File file : files) {
            if (file.getName().startsWith("mobi.chouette.exchange")) {
                String name = file.getName().split("\\-")[0] + ".jar";
                JavaArchive archive = ShrinkWrap
                        .create(ZipImporter.class, name)
                        .importFrom(file)
                        .as(JavaArchive.class);
                modules.add(archive);
            } else {
                jars.add(file);
            }
        }

        File[] filesDao = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("mobi.chouette:mobi.chouette.dao")
                .withTransitivity()
                .asFile();

        if (filesDao.length == 0) {
            throw new NullPointerException("no dao");
        }

        for (File file : filesDao) {
            if (file.getName().startsWith("mobi.chouette.dao")) {
                String name = file.getName().split("\\-")[0] + ".jar";
                JavaArchive archive = ShrinkWrap
                        .create(ZipImporter.class, name)
                        .importFrom(file)
                        .as(JavaArchive.class);
                modules.add(archive);

                if (!modules.contains(archive)) {
                    modules.add(archive);
                }
            } else {
                if (!jars.contains(file))
                    jars.add(file);
            }
        }

        final WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
                .addClass(NetexExportTests.class)
                .addClass(NetexTestUtils.class)
                .addClass(DummyChecker.class)
                .addClass(JobDataTest.class);

        result = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                .addAsLibraries(jars.toArray(new File[0]))
                .addAsModules(modules.toArray(new JavaArchive[0]))
                .addAsModule(testWar)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml");

        return result;
    }

    protected void init() {
        Locale.setDefault(Locale.ENGLISH);

        if (initialContext == null) {
            try {
                initialContext = new InitialContext();
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
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
        configuration.setNoSave(false);
        configuration.setCleanRepository(true);
        configuration.setOrganisationName("organisation");
        configuration.setReferentialName("test");

        JobDataTest test = new JobDataTest();
        context.put(JOB_DATA, test);
        test.setPathName("target/referential/test");
        File file = new File("target/referential/test");

        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file.mkdirs();
        test.setReferential("chouette_gui");
        test.setAction(IMPORTER);
        test.setType("netexprofile");
        context.put("testng", "true");
        context.put(OPTIMIZED, Boolean.FALSE);
        return context;
    }

    protected Context initExportContext() {
        init();
        ContextHolder.setContext("chouette_gui"); // set tenant schema

        Context context = new Context();
        context.put(INITIAL_CONTEXT, initialContext);
        context.put(REPORT, new ActionReport());
        context.put(VALIDATION_REPORT, new ValidationReport());

        NetexprofileExportParameters configuration = new NetexprofileExportParameters();
        context.put(CONFIGURATION, configuration);
        configuration.setName("name");
        configuration.setUserName("userName");
        configuration.setOrganisationName("organisation");
        configuration.setReferentialName("test");
        configuration.setValidateAfterExport(true);
        JobDataTest test = new JobDataTest();
        context.put(JOB_DATA, test);

        test.setPathName("target/referential/test");
        File f = new File("target/referential/test");

        if (f.exists()) {
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        f.mkdirs();
        test.setReferential("chouette_gui");
        test.setAction(EXPORTER);
        test.setType("netexprofile");
        context.put("testng", "true");
        context.put(OPTIMIZED, Boolean.FALSE);
        return context;
    }

    private void clearOldDatabaseRecords() throws Exception {
        try {
            utx.begin();

            em.joinTransaction();
            log.info("Dumping old codespace records...");

            codespaceDao.deleteAll();
            codespaceDao.flush();

            blockDao.deleteAll();
            blockDao.flush();

            journeyFrequencyDAO.deleteAll();
            journeyFrequencyDAO.flush();

            lineDao.deleteAll();
            lineDao.flush();

            stopAreaDao.deleteAll();
            stopAreaDao.flush();

            utx.commit();

        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            while (cause != null) {
                log.error(cause);
                if (cause instanceof SQLException)
                    traceSqlException((SQLException) cause);
                cause = cause.getCause();
            }
            throw ex;
        }
    }

    private void insertCodespaceRecords(List<Codespace> codespaces) throws Exception {
        utx.begin();
        em.joinTransaction();
        log.info("Inserting codespace records...");

        for (Codespace codespace : codespaces) {
            codespaceDao.create(codespace);
        }

        codespaceDao.flush();
        utx.commit();
        codespaceDao.clear();
    }

    @Test(enabled = true, groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportFlexibleLine() throws Exception {
        importLines("C_NETEX_FLEXIBLE_LINE_1.xml", 1, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        NetexTestUtils.verifyValidationReport(context);


        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

        NetexTestUtils.verifyValidationReport(context);
    }

    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportAvinorLine() throws Exception {
        importLines("C_NETEX_1.xml", 1, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

		NetexTestUtils.verifyValidationReport(context);

        
        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

        NetexTestUtils.verifyValidationReport(context);
    }

    @Test(enabled = true, groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportAvinorMultipleLines() throws Exception {
        importLines("avinor_multiple_lines_with_commondata.zip", 4, 3, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        NetexTestUtils.verifyValidationReport(context);

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 4, "files reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 3, "lines reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }

    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportAvinorLineWithMixedDayTypes() throws Exception {
        importLines("C_NETEX_7.xml", 1, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

        NetexTestUtils.verifyValidationReport(context);
    }

    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportAvinorLineWithMultipleStops() throws Exception {
        importLines("C_NETEX_5.xml", 1, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

        NetexTestUtils.verifyValidationReport(context);
    }

    @Test(enabled = true, groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void exportLinesInGroups() throws Exception {
        importLines("avinor_multiple_groups_of_lines.zip", 13, 12, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        NetexTestUtils.verifyActionReport(context);
        NetexTestUtils.verifyValidationReport(context);

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 13, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 12, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }

    @Test(enabled = false, groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void verifyExportRuterLine() throws Exception {
        importLines("ruter_single_line_210_with_commondata.zip", 2, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "RUT", "http://www.rutebanken.org/ns/ruter"))
        );

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 1, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

        NetexTestUtils.verifyValidationReport(context);
    }

    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void exportLineWithNotices() throws Exception {
        importLines("avinor_single_line_with_notices.zip", 2, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );
        
        log.info("*********IMPORT COMPLETE, STARTING EXPORT**********@");

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }
        NetexTestUtils.verifyValidationReport(context);

        NetexTestUtils.verifyValidationReport(context);
        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }

    @Test(groups = {"ExportBlocks"}, description = "Export Plugin should export file")
    public void exportLineWithCommonBlocks() throws Exception {
        importLines("avinor_single_line_with_blocks.zip", 2, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );

        log.info("*********IMPORT COMPLETE, STARTING EXPORT**********@");

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setExportBlocks(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }
        NetexTestUtils.verifyValidationReport(context);

        NetexTestUtils.verifyValidationReport(context);
        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }


    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void exportLineWithSameLineInterchanges() throws Exception {
        importLines("avinor_single_line_with_interchanges.zip", 2, 1, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );
        
        log.info("*********IMPORT COMPLETE, STARTING EXPORT**********@");

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }
        NetexTestUtils.verifyValidationReport(context);

        NetexTestUtils.verifyValidationReport(context);
        
        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 2, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 1, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }

    @Test(groups = {"ExportLine"}, description = "Export Plugin should export file")
    public void exportLinesWithInterchanges() throws Exception {
        importLines("avinor_multiple_line_with_interchanges.zip", 3, 2, Arrays.asList(
                createCodespace(null, "NSR", "http://www.rutebanken.org/ns/nsr"),
                createCodespace(null, "AVI", "http://www.rutebanken.org/ns/avi"))
        );
        
        log.info("*********IMPORT COMPLETE, STARTING EXPORT**********@");

        Context context = initExportContext();
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
        configuration.setValidateAfterExport(true);
        configuration.setAddMetadata(false);
        configuration.setReferencesType("line");
        configuration.setExportStops(true);
        configuration.setDefaultCodespacePrefix("AVI");

        Command command = CommandFactory.create(initialContext, NetexprofileExporterCommand.class.getName());

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

        NetexTestUtils.verifyValidationReport(context);

        ActionReport report = (ActionReport) context.get(REPORT);
        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), 3, "file reported");

        for (FileReport info : report.getFiles()) {
            Reporter.log(info.toString(),true);
        }

        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), 2, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
            Reporter.log(info.toString(), true);
        }

    }

    private void importLines(String file, int fileCount, int lineCount, List<Codespace> codespaces) throws Exception {
        Context context = initImportContext();

        clearOldDatabaseRecords();
        insertCodespaceRecords(codespaces);

        NetexTestUtils.copyFile(file);
        JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
        jobData.setInputFilename(file);

        NetexprofileImporterCommand command = (NetexprofileImporterCommand) CommandFactory.create(initialContext, NetexprofileImporterCommand.class.getName());

        NetexprofileImportParameters configuration = (NetexprofileImportParameters) context.get(CONFIGURATION);
        configuration.setNoSave(false);
        configuration.setCleanRepository(true);
        configuration.setParseSiteFrames(true);
        configuration.setObjectIdPrefix("AVI");

        try {
            command.execute(context);
        } catch (Exception ex) {
            log.error("test failed", ex);
            throw ex;
        }

		NetexTestUtils.verifyValidationReport(context);

        
        ActionReport report = (ActionReport) context.get(REPORT);
        Reporter.log(report.toString(), true);
        ValidationReport valReport = (ValidationReport) context.get(VALIDATION_REPORT);

        Assert.assertEquals(report.getResult(), STATUS_OK, "result");
        Assert.assertEquals(report.getFiles().size(), fileCount, "file reported");
        Assert.assertNotNull(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE), "line reported");
        Assert.assertEquals(report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports().size(), lineCount, "line reported");

        for (ObjectReport info : report.getCollections().get(ActionReporter.OBJECT_TYPE.LINE).getObjectReports()) {
            Reporter.log("report line :" + info.toString(), true);
            Assert.assertEquals(info.getStatus(), ActionReporter.OBJECT_STATE.OK, "line status");
        }

        for (CheckPointReport cp : valReport.getCheckPoints()) {
            if (cp.getState().equals(ValidationReporter.RESULT.NOK)) {
                Reporter.log(cp.toString(), true);
            }
        }
    }


    private void traceSqlException(SQLException ex) {
        while (ex.getNextException() != null) {
            ex = ex.getNextException();
            log.error(ex);
        }
    }

}
