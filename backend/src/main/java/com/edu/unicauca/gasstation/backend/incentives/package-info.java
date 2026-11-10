/**
 * Incentive rules: pesos a seller earns per unit sold of an additive, from a date. It also keeps, for each closed
 * journal, the additive units each seller sold. Other modules use only
 * {@link com.edu.unicauca.gasstation.backend.incentives.IncentiveRuleQuery}; every subpackage is internal.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Incentives")
package com.edu.unicauca.gasstation.backend.incentives;
