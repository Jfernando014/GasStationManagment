package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Explicit workbook parameter fixture, not a production profile or live sales source. */
public final class SalesTargetDistributionTestData {
    private SalesTargetDistributionTestData() { }
    public static DistributionProfile workbook() {
        var hours = Arrays.stream("1.1,0.9,0.5,0.4,0.5,1.85,5.3,6.6,7,8.1,8.05,9,9.3,9.6,9.15,9.6,9.2,9.85,13.3,12.7,10.9,8.2,4.9,2.1".split(","))
                .map(BigDecimal::new).toList();
        var days = new EnumMap<DayOfWeek, BigDecimal>(DayOfWeek.class);
        var values = "701.54,705.34,764.49,756.2,799.65,883.42,625.86".split(",");
        for (var day : DayOfWeek.values()) days.put(day, new BigDecimal(values[day.getValue()-1]));
        return new DistributionProfile("Workbook fixture", "PARAMETROS B5:B28/B32:B38/B71:B80; snapshot July 2026",
                hours, days, Set.of("12-7","6-9y5-9","2-9pm","6-9am","2-6pm","6-9y2-6"),
                new BigDecimal("0.7"), new BigDecimal("0.3"), new BigDecimal("0.5"), new BigDecimal("0.5"));
    }
    public static List<Coverage> fullMonth() {
        var coverage = new ArrayList<Coverage>();
        var month = YearMonth.of(2026,8);
        for (int day=1; day<=31; day++) {
            coverage.add(new Coverage(new Assignment(month.atDay(day), new UUID(0,1), "DIA"), hours(6,18)));
            var night = new TreeSet<>(hours(0,6)); night.addAll(hours(18,24));
            coverage.add(new Coverage(new Assignment(month.atDay(day), new UUID(0,2), "NOCHE"), night));
            coverage.add(new Coverage(new Assignment(month.atDay(day), new UUID(0,3), "12-7"), hours(12,19)));
        }
        return coverage;
    }
    public static Set<Integer> hours(int start, int end) {
        var hours = new TreeSet<Integer>(); for(int i=start;i<end;i++) hours.add(i); return hours;
    }
    /** Exact ESQUEMA TURNOS C5:AG8 rows, with fixture UUIDs (not production worker IDs). */
    public static List<Coverage> workbookSchedule() {
        var rows = List.of(
                "DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA6,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA6,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA,9-6",
                "NOCHE,DESCANSO,DESCANSO,DIA,DIA6,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA,DIA6,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE",
                "DESCANSO,DIA,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,DIA6,DIA,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE,DESCANSO,DESCANSO,9-6,9-6,NOCHE,NOCHE,DESCANSO,DESCANSO",
                "12-7,DESCANSO,12-7,12-7,12-7,12-7,12-7,12-7,DESCANSO,12-7,12-7,12-7,12-7,12-7,12-7,DESCANSO,DESCANSO,6-9y5-9,6-9y5-9,6-9y5-9,6-9y5-9,6-9y5-9,DESCANSO,6-9y5-9,6-9y5-9,6-9y5-9,6-9y5-9,6-9y5-9,6-9y5-9,DESCANSO,6-9y5-9");
        var result = new ArrayList<Coverage>();
        for (int worker=0;worker<rows.size();worker++) {
            var codes=rows.get(worker).split(",");
            for(int day=1;day<=codes.length;day++) {
                var hours = switch(codes[day-1]) {
                    case "DIA" -> hours(6,18); case "DIA6" -> hours(6,12); case "9-6" -> hours(9,18);
                    case "12-7" -> hours(12,19); case "DESCANSO" -> Set.<Integer>of();
                    case "NOCHE" -> { var h=new TreeSet<>(hours(0,6));h.addAll(hours(18,24));yield h; }
                    case "6-9y5-9" -> { var h=new TreeSet<>(hours(6,9));h.addAll(hours(17,21));yield h; }
                    default -> throw new IllegalArgumentException();
                };
                result.add(new Coverage(new Assignment(LocalDate.of(2026,8,day),new UUID(0,worker+1),codes[day-1]),hours));
            }
        }
        return result;
    }
}
