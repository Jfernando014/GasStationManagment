package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException;
import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException.Reason;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DailyDownloadWorkbookExporter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Gets the closing data of one journal from SellerCloseQuery and builds the daily Excel files.
 *
 * @author javiersolanop777
 */
@Service
public class DailyDownloadService {

    private final SellerCloseQuery atrSellerCloseQuery;
    private final DailyDownloadWorkbookExporter atrExporter;

    public DailyDownloadService(SellerCloseQuery prmSellerCloseQuery, DailyDownloadWorkbookExporter prmExporter) {
        this.atrSellerCloseQuery = prmSellerCloseQuery;
        this.atrExporter = prmExporter;
    }

    /** A file ready to download. */
    public record DownloadFile(String atrFileName, byte[] atrContent) {
    }

    public DownloadFile buildSalesSheet(LocalDate prmDate) {
        DaySummary objDay = findDay(prmDate);
        List<VendorCloseSummary> objVendors = atrSellerCloseQuery.findVendorClosesByDate(prmDate);
        try {
            return new DownloadFile("planilla-ventas-" + prmDate + ".xlsx", atrExporter.exportSalesSheet(objDay, objVendors));
        } catch (IOException objException) {
            throw new DailyDownloadException(Reason.GENERATION_FAILED, "Could not build the PLANILLA VENTAS file", objException);
        }
    }

    public DownloadFile buildLubricantsAndMisc(LocalDate prmDate) {
        DaySummary objDay = findDay(prmDate);
        try {
            return new DownloadFile("lubricantes-y-varios-" + prmDate + ".xlsx", atrExporter.exportLubricants(objDay));
        } catch (IOException objException) {
            throw new DailyDownloadException(Reason.GENERATION_FAILED, "Could not build the LUBRICANTES Y VARIOS file", objException);
        }
    }

    private DaySummary findDay(LocalDate prmDate) {
        return atrSellerCloseQuery.findDaySummary(prmDate)
                .orElseThrow(() -> new DailyDownloadException(Reason.NO_DATA, "There is no closing data for " + prmDate));
    }
}
