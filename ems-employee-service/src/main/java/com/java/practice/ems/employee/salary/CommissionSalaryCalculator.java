package com.java.practice.ems.employee.salary;

import org.springframework.stereotype.Component;

import com.java.practice.ems.employee.entity.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: Commission-based salary calculation.
 *
 * <p>
 * Commission employees receive a fixed base salary plus a variable commission
 * component tied to their performance (e.g., sales targets, KPI achievements).
 * </p>
 *
 * <p>
 * The {@code hoursWorked} parameter is repurposed here to carry the
 * <em>commission achievement percentage</em> (0.0–100.0). This is a pragmatic
 * trade-off for the shared interface — in a real system you would extend the
 * interface or pass a richer context object.
 * </p>
 *
 * <p>
 * <strong>OCP in action:</strong> Sales team onboarding required commission
 * pay.
 * This new class was added — no existing strategy or factory code was modified.
 * </p>
 */
@Component
public class CommissionSalaryCalculator implements SalaryCalculator {

    // Commission rate: 20% of base salary for every 100% target achievement
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20");
    private static final BigDecimal EPF_EMPLOYEE_RATE = new BigDecimal("0.11");
    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005");

    @Override
    public EmploymentType getSupportedType() {
        return EmploymentType.COMMISSION;
    }

    /**
     * Calculates net monthly pay = (base + commission) − deductions.
     *
     * <p>
     * Commission formula:
     * {@code commission = baseSalary × COMMISSION_RATE × (achievementPct / 100)}
     * </p>
     *
     * <p>
     * Example: base = MYR 3,000, achievement = 80%
     * </p>
     * 
     * <pre>
     *   commission = 3000 × 0.20 × (80/100) = 3000 × 0.20 × 0.80 = MYR 480
     *   gross      = 3000 + 480 = MYR 3,480
     *   EPF        = 3480 × 0.11 = MYR 382.80
     *   SOCSO      = 3480 × 0.005 = MYR 17.40
     *   net        = 3480 − 382.80 − 17.40 = MYR 3,079.80
     * </pre>
     *
     * @param employee the employee instance representing the fixed base salary and
     *                 achievement percentage
     * @return net monthly take-home pay
     */
    @Override
    public BigDecimal calculate(Employee employee) {
        BigDecimal baseSalary = employee.getBaseSalary();
        double hoursWorked = employee.getHoursWorked() != null ? employee.getHoursWorked() : 0.0;

        // hoursWorked carries achievementPercentage for this strategy
        double achievementPct = Math.min(100.0, Math.max(0.0, hoursWorked));

        BigDecimal commission = baseSalary
                .multiply(COMMISSION_RATE)
                .multiply(BigDecimal.valueOf(achievementPct / 100.0));

        BigDecimal grossPay = baseSalary.add(commission);
        BigDecimal epfDeduction = grossPay.multiply(EPF_EMPLOYEE_RATE);
        BigDecimal socsoDeduction = grossPay.multiply(SOCSO_RATE);

        return grossPay
                .subtract(epfDeduction)
                .subtract(socsoDeduction)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
