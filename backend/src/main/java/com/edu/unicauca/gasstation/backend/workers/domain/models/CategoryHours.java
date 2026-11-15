package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** Hours of each category of the overtime sheet. Whole hours: the segments kept per day are whole hours. */
public record CategoryHours(int rdf, int rno, int rnf, int hedo, int heno, int hedf, int henf) {

    public static final CategoryHours ZERO = new CategoryHours(0, 0, 0, 0, 0, 0, 0);

    public int get(OvertimeCategory category) {
        return switch (category) {
            case RDF -> rdf;
            case RNO -> rno;
            case RNF -> rnf;
            case HEDO -> hedo;
            case HENO -> heno;
            case HEDF -> hedf;
            case HENF -> henf;
        };
    }

    /** A copy with {@code hours} added to one category; negative hours subtract. */
    public CategoryHours plus(OvertimeCategory category, int hours) {
        return new CategoryHours(
                rdf + (category == OvertimeCategory.RDF ? hours : 0),
                rno + (category == OvertimeCategory.RNO ? hours : 0),
                rnf + (category == OvertimeCategory.RNF ? hours : 0),
                hedo + (category == OvertimeCategory.HEDO ? hours : 0),
                heno + (category == OvertimeCategory.HENO ? hours : 0),
                hedf + (category == OvertimeCategory.HEDF ? hours : 0),
                henf + (category == OvertimeCategory.HENF ? hours : 0));
    }

    public CategoryHours plus(CategoryHours other) {
        return new CategoryHours(rdf + other.rdf, rno + other.rno, rnf + other.rnf, hedo + other.hedo,
                heno + other.heno, hedf + other.hedf, henf + other.henf);
    }
}
