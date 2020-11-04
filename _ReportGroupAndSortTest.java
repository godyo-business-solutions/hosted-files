package com.godyo.p5.reports;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.crystaldecisions.sdk.occa.report.application.DBOptions;
import com.crystaldecisions.sdk.occa.report.application.DataDefController;
import com.crystaldecisions.sdk.occa.report.application.DatabaseController;
import com.crystaldecisions.sdk.occa.report.application.GroupSortController;
import com.crystaldecisions.sdk.occa.report.application.RecordSortController;
import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.application.RowsetController;
import com.crystaldecisions.sdk.occa.report.data.ConnectionInfo;
import com.crystaldecisions.sdk.occa.report.data.DBField;
import com.crystaldecisions.sdk.occa.report.data.FieldValueType;
import com.crystaldecisions.sdk.occa.report.data.Groups;
import com.crystaldecisions.sdk.occa.report.data.IConnectionInfo;
import com.crystaldecisions.sdk.occa.report.data.IDataDefinition;
import com.crystaldecisions.sdk.occa.report.data.IDatabase;
import com.crystaldecisions.sdk.occa.report.data.IGroup;
import com.crystaldecisions.sdk.occa.report.data.ISort;
import com.crystaldecisions.sdk.occa.report.data.ITable;
import com.crystaldecisions.sdk.occa.report.data.Sort;
import com.crystaldecisions.sdk.occa.report.data.SortDirection;
import com.crystaldecisions.sdk.occa.report.data.Sorts;
import com.crystaldecisions.sdk.occa.report.lib.PropertyBag;
import com.crystaldecisions.sdk.occa.report.lib.PropertyBagHelper;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKSortException;
import com.godyo.p5.test.base.PDFTextExtractor;

public class _ReportGroupAndSortTest {

	private static final String CONNECTION_URL = "jdbc:oracle:thin:@youroracledbhostname:1519:yoursid";
	private static final String LOGIN_NAME = "\"username\"";
	private static final String LOGIN_PASSWORD = "\"phoenix\"";

	private static final String REPORT_NAME = "ReportGroupAndSort.RPT";

	@Test
	public void testReportWithGroupAndDifferentSorts() throws Exception {
		final ReportClientDocument doc = createClientDoc(REPORT_NAME);
		final File toPDF = convertToPDF(doc);
		final String contentFromPDF = new PDFTextExtractor(toPDF).extractContentFromPDF();
		checkInOrder(contentFromPDF, "LieferantA", "70007", "310035", "310038", "Seite 1", "5.968,15", "LieferantB",
				"70010", "90310064", "Seite 2", "2.079,00", "90310068", "90310069");
	}

	private File convertToPDF(final ReportClientDocument doc) throws IOException, ReportSDKException {
		final ReportConverter converter = new ReportConverter(doc);
		final File tempFile = File.createTempFile("ReportGroupAndSortTest", ".pdf");
		converter.exportToPDF(tempFile);
		doc.getReportSource().dispose();
		doc.close();
		return tempFile;
	}

	private ReportClientDocument createClientDoc(final String reportName)
			throws ReportSDKException, URISyntaxException, IOException {
		final File tempFile = createReportFile(reportName);
		ReportClientDocument reportClientDoc = ReportClientDocument.openReport(tempFile);

		final DatabaseController databaseController = reportClientDoc.getDatabaseController();
		final String tableAlias = databaseController.getDatabase().getTables().getTable(0).getAlias();

		changeDataSource(reportClientDoc);

		reportClientDoc = changeSorts(reportClientDoc, tableAlias);
		return reportClientDoc;
	}

	private File createReportFile(final String reportName) throws IOException, FileNotFoundException {
		final File tempFile = File.createTempFile("TMP", "");

		tempFile.deleteOnExit();
		IOUtils.copy(getClass().getResourceAsStream(reportName), java.nio.file.Files.newOutputStream(tempFile.toPath()));

		return tempFile;
	}

	private ReportClientDocument changeSorts(final ReportClientDocument clientDoc, final String tableAlias) {
		final Sorts buildSorts = buildSorts(tableAlias);

		try {

			// Wenn eine Sortierung in den Metadaten vorhanden ist wird eine
			// eventuelle Sortierung
			// aus dem Report entfernt.
			if (!buildSorts.isEmpty()) {
				final DataDefController dataDefCtrl = clientDoc.getDataDefController();
				final RecordSortController rsCtrl = dataDefCtrl.getRecordSortController();
				final IDataDefinition dataDef = dataDefCtrl.getDataDefinition();
				final Sorts sorts = dataDef.getSorts();
				final Groups groups = dataDef.getGroups();
				final GroupSortController gsCtrl = dataDefCtrl.getGroupSortController();

				// Bei Gruppensortierungen die nicht gelöscht werden
				// können, die Sortierung auf no sort setzen.
				for (final ISort iSort : sorts) {

					final ISort findSort = gsCtrl.findSort(iSort.getSortField());
					if (findSort == null) {
						continue;
					}

					try {
						gsCtrl.modifySortDirection(iSort, SortDirection.noSort);
					} catch (final ReportSDKException | IndexOutOfBoundsException e) {
					}
				}

				int anz = 0;
				final int size = groups.size();
				for (int i = 0; i < buildSorts.size(); i++) {
					if (i == size) {
						// keine gruppe mehr da
						break;
					}

					// Gruppensortierungen überprüfen, anpassen
					final ISort bSort = buildSorts.get(i);

					final IGroup iGroup = groups.get(i);

					try {

						if (iGroup.getSort().getSortField().getFormulaForm().equals(bSort.getSortField().getFormulaForm())) {
							gsCtrl.modifySortDirection(iGroup.getSort(), bSort.getDirection());
							anz++;
						} else {
							// andere Sortierreihenfolge als Gruppierung => abbrechen
							break;
						}
					} catch (final IndexOutOfBoundsException | ReportSDKException e) {
					}
				}

				int k = 0;
				while (sorts.size() > k) {
					try {
						final ISort findSort = rsCtrl.findSort(sorts.get(k).getSortField());
						if (findSort == null) {
							k++;
							continue;
						}
						rsCtrl.remove(findSort);
					} catch (final ReportSDKException e) {
						k++;
					}
				}
				sorts.removeAllElements();

				int sortAnz = anz;
				for (int i = anz; i < buildSorts.size(); i++) {

					final Sort newSort = (Sort) buildSorts.get(i);

					// alte Sortierungen entfernen (auch nach einem Refresh die
					// neuen)
					final ISort findSort = rsCtrl.findSort((newSort).getSortField());
					if (findSort != null) {
						rsCtrl.remove(findSort);
					}

					// neue Sortierung hinzufügen
					try {
						rsCtrl.add(i, newSort);
						sortAnz++;
					} catch (final ReportSDKSortException e) {
					}
				}

				for (int i = 0; i < groups.size(); i++) {
					final ISort gSort = groups.get(i).getSort();
					if (gSort.getDirection() == SortDirection.noSort && rsCtrl.findSort((gSort).getSortField()) == null) {
						final ISort newGSort = new Sort(gSort);
						newGSort.setDirection(SortDirection.ascendingOrder);
						rsCtrl.add(sortAnz + i, newGSort);
					}
				}

			}

			final RowsetController rowsetController = clientDoc.getRowsetController();
			rowsetController.refresh();
		} catch (final ReportSDKException e) {
		} finally {
		}
		return clientDoc;
	}

	private static Sorts buildSorts(final String tableAlias) {
		final Sorts sorts = new Sorts();

		final Sort sort1 = new Sort();
		sort1.setSortField(createDbField(tableAlias, "BESTBEZ"));
		sort1.setDirection(SortDirection.ascendingOrder);
		sorts.add(sort1);

		final Sort sort2 = new Sort();
		sort2.setSortField(createDbField(tableAlias, "POSBEZ"));
		sort2.setDirection(SortDirection.ascendingOrder);
		sorts.add(sort2);

		return sorts;
	}

	private static DBField createDbField(final String aTablename, final String aFieldname) {
		final DBField dBField = new DBField();
		dBField.setTableAlias(aTablename);
		dBField.setName(aFieldname);
		dBField.setType(FieldValueType.stringField);
		return dBField;
	}

	private void checkInOrder(final String pdf, final String... substr) throws IOException {
		int index = 0;

		for (final String search : substr) {
			final int indexOf = pdf.indexOf(search, index);
			assertTrue("Substring '" + search + "' not found, after index " + index, indexOf != -1);
			index = indexOf;
		}
	}

	private static void changeDataSource(final ReportClientDocument clientDoc)
			throws ReportSDKException {

		final Map<String, String> bag = new HashMap<>();
		bag.put(PropertyBagHelper.CONNINFO_JDBC_CONNECTION_URL, CONNECTION_URL);
		bag.put(PropertyBagHelper.CONNINFO_SERVER_TYPE, "JDBC (JNDI)");
		bag.put(PropertyBagHelper.CONNINFO_DATABASE_DLL, "crdb_jdbc.dll");
		bag.put(PropertyBagHelper.CONNINFO_JDBC_DATABASECLASSNAME, "oracle.jdbc.driver.OracleDriver");

		final DatabaseController dc = clientDoc.getDatabaseController();
		changeQualifiedTableNames(dc.getDatabase());

		for (final IConnectionInfo oldci : dc.getConnectionInfos(null)) {
			final IConnectionInfo newci = new ConnectionInfo();

			newci.setAttributes(new PropertyBag(bag));
			newci.setUserName(LOGIN_NAME);
			newci.setPassword(LOGIN_PASSWORD);

			dc.replaceConnection(oldci, newci, null,
					DBOptions._useDefault + DBOptions._doNotVerifyDB + DBOptions._ignoreCurrentTableQualifiers);
		}
	}

	private static void changeQualifiedTableNames(final IDatabase db) {
		for (final ITable table : db.getTables()) {
			table.setQualifiedName(table.getName());
		}
	}
}
