package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException.Code;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers an already announced closed journal using public sales and day-staff queries only.
 * Sources own their read transactions; the repository owns atomic writes and recoverable storage failures.
 * Suspending a caller transaction avoids reusing a completed closing transaction in future listeners.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ShiftSaleHistoryRegistrationService {

    private final SellerCloseQuery sales;
    private final DayStaffQuery staff;
    private final ShiftSaleHistoryRepository history;

    public ShiftSaleHistoryRegistrationService(
            SellerCloseQuery sales, DayStaffQuery staff, ShiftSaleHistoryRepository history) {
        this.sales = sales;
        this.staff = staff;
        this.history = history;
    }

    public JournalSalesProcessing registerClosedJournal(ClosedJournalSnapshot journal) {
        Objects.requireNonNull(journal, "journal");
        var previous = history.findProcessing(journal.journalDate());
        if (previous.isPresent() && previous.get().status() == JournalSalesProcessing.Status.PROCESSED) {
            return previous.get();
        }

        List<ShiftSaleHistory> snapshots;
        try {
            snapshots = readSnapshots(journal);
        } catch (RuntimeException failure) {
            try {
                var state = history.markPending(journal, describe(failure));
                // A concurrent successful registration takes precedence over a failed source read.
                if (state.status() == JournalSalesProcessing.Status.PROCESSED) {
                    return state;
                }
            } catch (RuntimeException markerFailure) {
                failure.addSuppressed(markerFailure);
            }
            throw failure;
        }
        // Storage failures already roll back and record PENDING in the repository: do not count them twice.
        return history.saveJournal(journal, snapshots);
    }

    private List<ShiftSaleHistory> readSnapshots(ClosedJournalSnapshot journal) {
        List<VendorCloseSummary> closes = sales.findVendorClosesByDate(journal.journalDate());
        if (closes == null || closes.stream().anyMatch(Objects::isNull)) {
            throw issue(Code.INVALID_SALES_RESPONSE, "Sales must return a non-null list without null closes");
        }
        if (closes.isEmpty()) {
            return List.of();
        }
        if (closes.stream().anyMatch(close -> !journal.journalDate().equals(close.date()))) {
            throw issue(Code.INVALID_SALES_RESPONSE, "A close belongs to another operational date");
        }

        DayStaff day = staff.getDayStaff(journal.journalDate());
        if (day == null || !journal.journalDate().equals(day.date())) {
            throw issue(Code.INVALID_STAFF_RESPONSE, "Day staff must describe the requested operational date");
        }
        if (day.step1Status() != DayStaff.Step1Status.CONFIRMED || !day.noveltiesApplied()) {
            throw issue(Code.STAFF_NOT_CONFIRMED, "Effective staff and novelties are not confirmed");
        }

        var membersByWorker = day.members().stream().collect(Collectors.groupingBy(DayStaff.Member::workerId));
        var identities = new HashSet<ShiftSaleIdentity>();
        var workedShifts = new HashSet<WorkerShift>();
        var snapshots = new ArrayList<ShiftSaleHistory>();
        for (VendorCloseSummary close : closes) {
            UUID workerId = resolveWorkerId(close.vendorDocument());
            var candidates = membersByWorker.get(workerId);
            if (candidates == null) {
                throw issue(Code.WORKER_NOT_IN_DAY_STAFF,
                        "Worker " + workerId + " is not in effective staff; inactive workers may be omitted by the source");
            }
            var matches = candidates.stream().filter(member -> member.workedCode().equals(close.shiftName())).toList();
            if (matches.isEmpty()) {
                throw issue(Code.SHIFT_MISMATCH,
                        "Worker " + workerId + " did not work reported shift " + close.shiftName());
            }
            if (matches.size() != 1) {
                throw issue(Code.AMBIGUOUS_SHIFT,
                        "More than one effective member matches worker " + workerId + " and shift " + close.shiftName());
            }
            ShiftSaleHistory snapshot = snapshot(journal, close, matches.get(0));
            if (!identities.add(snapshot.identity())
                    || !workedShifts.add(new WorkerShift(workerId, snapshot.workedCode()))) {
                throw issue(Code.DUPLICATE_CLOSE,
                        "Repeated close for worker " + workerId + " and shift " + close.shiftName());
            }
            snapshots.add(snapshot);
        }
        return List.copyOf(snapshots);
    }

    private static UUID resolveWorkerId(String originalIdentifier) {
        try {
            if (originalIdentifier == null) {
                throw new IllegalArgumentException("Missing vendor identifier");
            }
            UUID workerId = UUID.fromString(originalIdentifier);
            // UUID.fromString also accepts abbreviated groups: require a full UUID, allowing hex letter case only.
            if (!workerId.toString().equalsIgnoreCase(originalIdentifier)) {
                throw new IllegalArgumentException("Vendor identifier is not a full UUID");
            }
            return workerId;
        } catch (IllegalArgumentException failure) {
            throw new ShiftSaleHistoryIntegrationException(Code.INVALID_VENDOR_IDENTIFIER,
                    "vendorDocument must be a full worker UUID; cedula lookup is not provided by the public contracts",
                    failure);
        }
    }

    private static ShiftSaleHistory snapshot(
            ClosedJournalSnapshot journal, VendorCloseSummary close, DayStaff.Member member) {
        String roleName = member.dayRole().name();
        if (!"TITULAR".equals(roleName) && !"APOYO".equals(roleName)) {
            throw issue(Code.UNSUPPORTED_ROLE, "Unsupported effective role " + roleName);
        }
        var role = member.support() ? ShiftSaleHistory.EffectiveRole.APOYO : ShiftSaleHistory.EffectiveRole.TITULAR;
        if (!role.name().equals(roleName)) {
            throw issue(Code.INCONSISTENT_SUPPORT, "Effective role and day-staff support flag disagree");
        }
        try {
            return new ShiftSaleHistory(journal,
                    new ShiftSaleIdentity(journal.journalDate(), close.vendorDocument(), close.shiftName()),
                    member.workerId(), close.vendorName(), member.workedCode(), role,
                    new ShiftSaleAmounts(close.gallonsMotor(), close.gallonsDiesel(), close.gallonsExtra(),
                            close.gallonsMaxpro(), close.basketDominusAmount(), close.dominusTirilla(),
                            close.totalDelivered()));
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new ShiftSaleHistoryIntegrationException(Code.INVALID_SALE,
                    "Missing or invalid recorded values for worker " + member.workerId() + " and shift " + close.shiftName(),
                    failure);
        }
    }

    private static ShiftSaleHistoryIntegrationException issue(Code code, String detail) {
        return new ShiftSaleHistoryIntegrationException(code, detail);
    }

    private static String describe(RuntimeException failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record WorkerShift(UUID workerId, String workedCode) {
    }
}
