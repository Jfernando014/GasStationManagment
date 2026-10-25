package com.edu.unicauca.gasstation.backend.sales.application;

import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.DATE;
import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.DATE_WITHOUT_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.FakeSellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.application.DailyDownloadService.DownloadFile;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException.Reason;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DailyDownloadWorkbookExporter;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * The service asks SellerCloseQuery for the data and names the files.
 *
 * @author javiersolanop777
 */
class DailyDownloadServiceTest {

    private final DailyDownloadService atrService =
            new DailyDownloadService(new FakeSellerCloseQuery(), new DailyDownloadWorkbookExporter());

    @Test
    void salesSheetHasDateInTheFileNameAndIsAValidWorkbook() throws Exception {
        DownloadFile objFile = atrService.buildSalesSheet(DATE);
        assertEquals("planilla-ventas-2026-10-24.xlsx", objFile.atrFileName());
        try (XSSFWorkbook objWorkbook = new XSSFWorkbook(new ByteArrayInputStream(objFile.atrContent()))) {
            assertEquals("PLANILLA VENTAS", objWorkbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void lubricantsFileHasDateInTheFileNameAndIsAValidWorkbook() throws Exception {
        DownloadFile objFile = atrService.buildLubricantsAndMisc(DATE);
        assertEquals("lubricantes-y-varios-2026-10-24.xlsx", objFile.atrFileName());
        assertTrue(objFile.atrContent().length > 0);
        try (XSSFWorkbook objWorkbook = new XSSFWorkbook(new ByteArrayInputStream(objFile.atrContent()))) {
            assertEquals("LUBRICANTES Y VARIOS", objWorkbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void dateWithoutClosingDataFailsWithNoData() {
        DailyDownloadException objSales = assertThrows(DailyDownloadException.class,
                () -> atrService.buildSalesSheet(DATE_WITHOUT_DATA));
        assertEquals(Reason.NO_DATA, objSales.getReason());
        DailyDownloadException objLubricants = assertThrows(DailyDownloadException.class,
                () -> atrService.buildLubricantsAndMisc(DATE_WITHOUT_DATA));
        assertEquals(Reason.NO_DATA, objLubricants.getReason());
    }
}
