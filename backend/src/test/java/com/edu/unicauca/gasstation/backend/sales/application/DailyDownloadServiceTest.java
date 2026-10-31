package com.edu.unicauca.gasstation.backend.sales.application;

import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.DATE;
import static com.edu.unicauca.gasstation.backend.sales.DailyDownloadTestData.DATE_WITHOUT_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.application.DailyDownloadService.DownloadFile;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.VendorShiftCloseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.VendorCloseCalculationEngine;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException.Reason;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DailyDownloadWorkbookExporter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The service builds the daily files with the real SellerCloseQueryServiceImpl and the real
 * VendorCloseCalculationEngine. Only the repository is replaced, by an in-memory list.
 * The tolerance, staff and fuel price ports are null because the read queries do not use them.
 *
 * <p>Test data:
 * <ul>
 *   <li>Andres: delivers 165000 and the Dominus slip says 165000, so the status is CUADRA.</li>
 *   <li>Milena: delivers 80000 and the slip says 85000, so she lacks 5000 and the status is FALTA.</li>
 *   <li>Both sell the product 80216, so the day file must show it once, with the units added.</li>
 * </ul>
 *
 * @author javiersolanop777
 */
class DailyDownloadServiceTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("1000");

    private final InMemoryVendorShiftCloseRepository atrRepository = new InMemoryVendorShiftCloseRepository();
    private final VendorCloseCalculationEngine atrEngine = new VendorCloseCalculationEngine();
    private DailyDownloadService atrService;

    @BeforeEach
    void setUp() {
        SellerCloseQuery objQuery = new SellerCloseQueryServiceImpl(atrRepository, atrEngine, null, null, null);
        atrService = new DailyDownloadService(objQuery, new DailyDownloadWorkbookExporter());

        atrRepository.save(close("1061", "Andres", "DIA", 1,
                "100000", "20000", "30000", "10000", "5000", "10", "20", "5", "0", "165000",
                List.of(product("80216", "Aceite Test", "LUBRICANTE", 3, "20000"),
                        product("76102", "Aditivo Test", "ADITIVO", 2, "10000"))));
        atrRepository.save(close("1062", "Milena", "NOCHE", 2,
                "80000", "0", "0", "0", "0", "5", "5", "0", "2", "85000",
                List.of(product("80216", "Aceite Test", "LUBRICANTE", 2, "20000"),
                        product("76001", "Bolsas Test", "OTRO", 5, "1000"))));
    }

    @Test
    void salesSheetHasDateInTheFileNameAndIsAValidWorkbook() throws Exception {
        DownloadFile objFile = atrService.buildSalesSheet(DATE);
        assertEquals("planilla-ventas-2026-10-24.xlsx", objFile.atrFileName());
        try (XSSFWorkbook objWorkbook = open(objFile)) {
            assertEquals("PLANILLA VENTAS", objWorkbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void salesSheetShowsOneRowPerVendorWithTheRealStatus() throws Exception {
        try (XSSFWorkbook objWorkbook = open(atrService.buildSalesSheet(DATE))) {
            Sheet objSheet = objWorkbook.getSheetAt(0);

            assertEquals("Andres", objSheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals(35.0, objSheet.getRow(4).getCell(8).getNumericCellValue());
            assertEquals(2.0, objSheet.getRow(4).getCell(9).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(4).getCell(10).getNumericCellValue());
            assertEquals(0.0, objSheet.getRow(4).getCell(17).getNumericCellValue());
            assertEquals("CUADRA", objSheet.getRow(4).getCell(18).getStringCellValue());

            assertEquals("Milena", objSheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals(12.0, objSheet.getRow(5).getCell(8).getNumericCellValue());
            assertEquals(0.0, objSheet.getRow(5).getCell(9).getNumericCellValue());
            assertEquals(-5000.0, objSheet.getRow(5).getCell(17).getNumericCellValue());
            assertEquals("FALTA", objSheet.getRow(5).getCell(18).getStringCellValue());
        }
    }

    @Test
    void salesSheetDayTotalsMatchTheSumOfTheVendors() throws Exception {
        try (XSSFWorkbook objWorkbook = open(atrService.buildSalesSheet(DATE))) {
            Sheet objSheet = objWorkbook.getSheetAt(0);

            assertEquals("TOTAL DEL DÍA", objSheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals(47.0, objSheet.getRow(6).getCell(8).getNumericCellValue());
            assertEquals(2.0, objSheet.getRow(6).getCell(9).getNumericCellValue());
            assertEquals(180000.0, objSheet.getRow(6).getCell(10).getNumericCellValue());
            assertEquals(245000.0, objSheet.getRow(6).getCell(15).getNumericCellValue());
            assertEquals(250000.0, objSheet.getRow(6).getCell(16).getNumericCellValue());
            assertEquals(-5000.0, objSheet.getRow(6).getCell(17).getNumericCellValue());
        }
    }

    @Test
    void lubricantsFileHasDateInTheFileNameAndIsAValidWorkbook() throws Exception {
        DownloadFile objFile = atrService.buildLubricantsAndMisc(DATE);
        assertEquals("lubricantes-y-varios-2026-10-24.xlsx", objFile.atrFileName());
        assertTrue(objFile.atrContent().length > 0);
        try (XSSFWorkbook objWorkbook = open(objFile)) {
            assertEquals("LUBRICANTES Y VARIOS", objWorkbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void lubricantsFileShowsTheSameProductOnceWithTheUnitsOfBothVendors() throws Exception {
        try (XSSFWorkbook objWorkbook = open(atrService.buildLubricantsAndMisc(DATE))) {
            Sheet objSheet = objWorkbook.getSheetAt(0);

            assertEquals("80216", objSheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("ACEITES", objSheet.getRow(4).getCell(2).getStringCellValue());
            assertEquals(20000.0, objSheet.getRow(4).getCell(3).getNumericCellValue());
            assertEquals(5.0, objSheet.getRow(4).getCell(4).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(4).getCell(5).getNumericCellValue());

            assertEquals("MERCANCIA VARIA", objSheet.getRow(5).getCell(2).getStringCellValue());
            assertEquals("ADITIVOS Y OTROS", objSheet.getRow(6).getCell(2).getStringCellValue());

            assertEquals(12.0, objSheet.getRow(7).getCell(4).getNumericCellValue());
            assertEquals(125000.0, objSheet.getRow(7).getCell(5).getNumericCellValue());
            assertEquals(100000.0, objSheet.getRow(9).getCell(5).getNumericCellValue());
            assertEquals(5000.0, objSheet.getRow(10).getCell(5).getNumericCellValue());
            assertEquals(20000.0, objSheet.getRow(11).getCell(5).getNumericCellValue());
            assertEquals("VENTA TOTAL CANASTA", objSheet.getRow(12).getCell(1).getStringCellValue());
            assertEquals(125000.0, objSheet.getRow(12).getCell(5).getNumericCellValue());
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

    private static XSSFWorkbook open(DownloadFile prmFile) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(prmFile.atrContent()));
    }

    /** Builds a close with the real engine, so the total, the discrepancy and the status are calculated. */
    private VendorShiftClose close(String prmDocument, String prmName, String prmShift, int prmPump,
                                   String prmCash, String prmGa, String prmCredibanco, String prmQr,
                                   String prmRedemptions, String prmMotor, String prmDiesel, String prmExtra,
                                   String prmMaxpro, String prmTirilla, List<VendorCloseProduct> prmProducts) {
        return atrEngine.calculateVendorClose(UUID.randomUUID(), DATE, prmDocument, prmName, prmShift, prmPump,
                bd(prmCash), bd(prmGa), bd(prmCredibanco), bd(prmQr), bd(prmRedemptions),
                bd(prmMotor), bd(prmDiesel), bd(prmExtra), bd(prmMaxpro),
                BigDecimal.ZERO, bd(prmTirilla), TOLERANCE, prmProducts);
    }

    private static VendorCloseProduct product(String prmSku, String prmName, String prmCategory,
                                              int prmUnits, String prmPrice) {
        return new VendorCloseProduct(UUID.randomUUID(), prmSku, prmName, prmCategory, prmUnits, bd(prmPrice), null);
    }

    private static BigDecimal bd(String prmValue) {
        return new BigDecimal(prmValue);
    }

    /** Hand-written in-memory repository. It returns the closes in the order they were saved. */
    private static class InMemoryVendorShiftCloseRepository implements VendorShiftCloseRepository {

        private final List<VendorShiftClose> atrCloses = new ArrayList<>();

        @Override
        public VendorShiftClose save(VendorShiftClose prmClose) {
            atrCloses.add(prmClose);
            return prmClose;
        }

        @Override
        public List<VendorShiftClose> saveAll(List<VendorShiftClose> prmCloses) {
            atrCloses.addAll(prmCloses);
            return prmCloses;
        }

        @Override
        public List<VendorShiftClose> findByJournalDate(LocalDate prmDate) {
            return atrCloses.stream().filter(prmClose -> prmClose.journalDate().equals(prmDate)).toList();
        }

        @Override
        public Optional<VendorShiftClose> findByJournalDateAndVendorDocument(LocalDate prmDate, String prmDocument) {
            return findByJournalDate(prmDate).stream()
                    .filter(prmClose -> prmClose.vendorDocument().equals(prmDocument))
                    .findFirst();
        }

        @Override
        public boolean existsByJournalDate(LocalDate prmDate) {
            return !findByJournalDate(prmDate).isEmpty();
        }
    }
}