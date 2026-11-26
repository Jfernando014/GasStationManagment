/** Rules of the monthly fuel variation alerts, valid from a date on. */
export interface VariationAlertSettingRequest {
  /** ISO date (yyyy-MM-dd) the rules apply from. */
  validFrom: string;
  /** Accumulated difference of the month, in percent, above which the month needs a review. */
  cumulativeLimitPercentage: number;
  /** Days in a row that raise the level of the alert. */
  escalationDays: number;
  /** Days with shortage allowed in a month, for each size of the shortage in gallons. */
  shortageLimit1To49: number;
  shortageLimit50To74: number;
  shortageLimit75To124: number;
  shortageLimit125To249: number;
  shortageLimit250To499: number;
  shortageLimit500OrMore: number;
}

export interface VariationAlertSettingResponse extends VariationAlertSettingRequest {
  id: number;
  createdAt: string;
}
