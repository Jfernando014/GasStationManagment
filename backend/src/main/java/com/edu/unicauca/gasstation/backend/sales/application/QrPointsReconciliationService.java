package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.QrPointsRepository;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-35: QR from the Redeban CSV (attributed by shift) and Puntos Colombia redemptions. */
@Service
@RequiredArgsConstructor
public class QrPointsReconciliationService {
    private final CardReconciliationRepository cards;
    private final QrPointsRepository qrPoints;
    private final ScheduleWorkerDirectory workers;
    private final ShiftExternalService shifts;

    /**
     * Reads the Redeban CSV, keeps the QR rows of the operative day (06:00 to 06:00), attributes each one to the vendor whose shift covers
     * its hour (never guessing when two vendors overlap) and replaces what was stored for the date.
     */
    @Transactional
    public RedebanImportResult importRedeban(LocalDate date, byte[] content) {
        List<RedebanTransaction> all = RedebanCsvParser.parse(decode(content));
        List<RedebanTransaction> qr = all.stream().filter(RedebanTransaction::isQr).toList();
        List<RedebanTransaction> approved = qr.stream().filter(RedebanTransaction::isApproved).toList();
        List<RedebanTransaction> rows = approved.stream()
                .filter(t -> t.operativeDate() == null || t.operativeDate().equals(date))
                .toList();
        if (rows.isEmpty()) {
            throw new ReconciliationValidationException("file",
                    "El archivo no tiene transacciones QR aprobadas del día operativo " + date);
        }
        List<VendorDeclaration> declarations = cards.findByDate(date);
        Map<String, ShiftCodeInfo> catalog = shifts.getActiveCatalog().stream()
                .collect(Collectors.toMap(ShiftCodeInfo::code, Function.identity(), (a, b) -> a));
        List<VendorShift> vendorShifts = declarations.stream()
                .filter(d -> d.shiftCode() != null && catalog.containsKey(d.shiftCode()))
                .map(d -> new VendorShift(d.workerId(), VendorShift.hoursOf(catalog.get(d.shiftCode()).segments()
                        .stream().map(s -> new int[]{s.startHour(), s.endHour()}).toList())))
                .toList();
        qrPoints.replaceRedeban(date, QrShiftAssigner.assign(rows, vendorShifts));
        return new RedebanImportResult(qr(date), all.size(), rows.size(), all.size() - qr.size(),
                qr.size() - approved.size(), approved.size() - rows.size());
    }

    /** HU-42 #4: text to copy and send to a vendor (the image download is drawn by the client). */
    @Transactional(readOnly = true)
    public String whatsappSummary(LocalDate date, UUID workerId) {
        QrReconciliationResult result = qr(date);
        if (!result.redebanLoaded()) {
            throw new ReconciliationValidationException("file", "Cargue primero el CSV de Redeban del " + date);
        }
        VendorQrCheck vendor = result.vendors().stream().filter(v -> v.workerId().equals(workerId)).findFirst()
                .orElseThrow(() -> new ReconciliationValidationException("workerId",
                        "Ese vendedor no tiene datos el " + date));
        return QrWhatsappSummary.text(date, vendor, result.perVendorReliable());
    }

    @Transactional(readOnly = true)
    public QrReconciliationResult qr(LocalDate date) {
        List<VendorDeclaration> declarations = cards.findByDate(date);
        return QrReconciliationCalculator.calculate(date, declarations, names(declarations),
                qrPoints.findRedeban(date));
    }

    /** Loads the day total of Puntos Colombia (typed until the Excel reading is defined). */
    @Transactional
    public PointsResult savePoints(LocalDate date, BigDecimal total) {
        List<VendorDeclaration> declarations = cards.findByDate(date);
        PointsReconciliation.compare(date, declarations, names(declarations), total, "MANUAL"); // validates the value
        qrPoints.savePoints(date, total.setScale(2), "MANUAL");
        return points(date);
    }

    @Transactional(readOnly = true)
    public PointsResult points(LocalDate date) {
        List<VendorDeclaration> declarations = cards.findByDate(date);
        var stored = qrPoints.findPoints(date);
        return PointsReconciliation.compare(date, declarations, names(declarations),
                stored.map(QrPointsRepository.PointsDay::total).orElse(null),
                stored.map(QrPointsRepository.PointsDay::source).orElse(null));
    }

    private Map<UUID, String> names(List<VendorDeclaration> declarations) {
        var ids = declarations.stream().map(VendorDeclaration::workerId).distinct().toList();
        Map<UUID, String> names = new HashMap<>();
        workers.getWorkersByIds(ids).forEach((id, info) -> names.put(id, info.fullName()));
        return names;
    }

    /** UTF-8 when valid, otherwise ISO-8859-1 (bank exports often come from Excel in Latin-1). */
    private static String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            return new String(content, StandardCharsets.ISO_8859_1);
        }
    }
}
