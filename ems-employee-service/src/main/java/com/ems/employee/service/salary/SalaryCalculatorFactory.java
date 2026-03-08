package com.ems.employee.service.salary;


import com.ems.employee.model.enums.EmploymentType;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ SalaryCalculatorFactory — Strategy Resolver ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>HOW THE FACTORY USES SPRING'S DEPENDENCY INJECTION</h2>
 *
 * <p>
 * This factory demonstrates <strong>Dependency Inversion Principle</strong> at
 * the
 * collection level. Spring automatically discovers ALL beans implementing
 * {@link SalaryCalculator} and injects them as a {@code List}. The factory then
 * builds a {@code Map<EmploymentType, SalaryCalculator>} for O(1) lookup.
 * </p>
 *
 * <p>
 * <strong>How Spring collects all strategies automatically:</strong>
 * </p>
 * 
 * <pre>
 *   Spring context startup
 *       │
 *       ├─ Scans classpath for @Component beans
 *       ├─ Finds: MonthlySalaryCalculator, HourlySalaryCalculator,
 *       │         DailySalaryCalculator, CommissionSalaryCalculator
 *       │
 *       └─ Injects List&lt;SalaryCalculator&gt; here ─► factory builds Map
 * </pre>
 *
 * <p>
 * When you add a NEW strategy (e.g., {@code ContractSalaryCalculator}),
 * you simply annotate it with {@code @Component}. Spring adds it to the
 * injected
 * list automatically — this factory and the service remain completely
 * untouched.
 * This is the <strong>Open/Closed Principle</strong> at its most elegant.
 * </p>
 *
 * <h2>JAVA 21 PATTERN MATCHING FOR SWITCH</h2>
 *
 * <p>
 * The {@link #getCalculator(String)} method uses Java 21's Pattern Matching
 * for switch to deliver a clear, type-safe resolution from String input.
 * See the method Javadoc for a detailed comparison with old if-else chains.
 * </p>
 */
@Component
public class SalaryCalculatorFactory {

    /**
     * Map from EmploymentType → its corresponding calculator.
     * Populated once at startup via constructor injection. Immutable at runtime.
     */
    private final Map<EmploymentType, SalaryCalculator> calculators;

    /**
     * Constructor-based injection of all SalaryCalculator implementations.
     *
     * <p>
     * <strong>Why constructor injection?</strong>
     * </p>
     * <ul>
     * <li><strong>Immutability:</strong> The {@code calculators} map is set once
     * and never changes — no risk of null during the object's lifetime.</li>
     * <li><strong>Testability:</strong> Pass a list of mock calculators directly
     * in unit tests — no Spring context required.</li>
     * <li><strong>Explicit dependencies:</strong> What this factory needs is
     * visible
     * in the constructor signature — no hidden {@code @Autowired} field magic.</li>
     * </ul>
     *
     * <p>
     * Spring automatically collects ALL beans implementing {@link SalaryCalculator}
     * into the {@code List} parameter. The {@code Collectors.toMap} call builds
     * the lookup map from each strategy's declared {@code getSupportedType()}.
     * </p>
     *
     * @param allCalculators all {@link SalaryCalculator} beans in the Spring
     *                       context
     */
    public SalaryCalculatorFactory(List<SalaryCalculator> allCalculators) {
        // Build an immutable lookup map: EmploymentType → Calculator
        // Collectors.toUnmodifiableMap ensures the map cannot be mutated after creation
        this.calculators = allCalculators.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SalaryCalculator::getSupportedType,
                        Function.identity()));
    }

    /**
     * Resolves the correct {@link SalaryCalculator} for the given employment type
     * string.
     *
     * <h3>JAVA 21 PATTERN MATCHING FOR SWITCH — In-Depth Explanation</h3>
     *
     * <p>
     * Java 21 Pattern Matching for switch (JEP 441) is a major evolution
     * from the old {@code if-else instanceof} chains and pre-Java-14 switch
     * statements.
     * </p>
     *
     * <p>
     * <strong>OLD approach (Java 8-16) — verbose, error-prone:</strong>
     * </p>
     * 
     * <pre>{@code
     * // ❌ Old: requires explicit cast after instanceof check
     * if (typeStr == null) {
     *     return calculators.get(EmploymentType.MONTHLY); // default
     * } else if (typeStr.equalsIgnoreCase("HOURLY")) {
     *     return calculators.get(EmploymentType.HOURLY);
     * } else if (typeStr.equalsIgnoreCase("DAILY")) {
     *     return calculators.get(EmploymentType.DAILY);
     * } else {
     *     throw new IllegalArgumentException("Unknown type: " + typeStr);
     * }
     * }</pre>
     *
     * <p>
     * <strong>NEW approach (Java 21) — concise, exhaustive, readable:</strong>
     * </p>
     * 
     * <pre>{@code
     * return switch (typeStr) {
     *     case null, "MONTHLY" -> calculators.get(EmploymentType.MONTHLY);
     *     case "HOURLY"        -> calculators.get(EmploymentType.HOURLY);
     *     default              -> throw new IllegalArgumentException(...);
     * };
     * }</pre>
     *
     * <p>
     * <strong>Key Java 21 improvements used here:</strong>
     * </p>
     * <ol>
     * <li><strong>Null handling in switch:</strong> Java 21 allows
     * {@code case null}
     * directly in switch expressions, eliminating a separate null check.</li>
     * <li><strong>Arrow cases:</strong> {@code ->} syntax is an expression (returns
     * a value)
     * rather than a statement (fall-through). No {@code break} needed, no
     * accidental
     * fall-through bugs.</li>
     * <li><strong>Exhaustiveness:</strong> The compiler verifies all enum cases are
     * covered.
     * Adding a new {@link EmploymentType} value causes a compilation error until
     * handled.</li>
     * <li><strong>Expression form:</strong> The entire switch is an expression
     * assigned to
     * a variable — more composable, works inside lambdas and streams.</li>
     * </ol>
     *
     * @param employmentTypeStr the employment type string from the API request
     *                          (case-insensitive)
     * @return the matching {@link SalaryCalculator} strategy
     * @throws IllegalArgumentException if the employment type is not recognized
     */
    public SalaryCalculator getCalculator(String employmentTypeStr) {
        // ── Java 21 Pattern Matching for switch ─────────────────────────────────
        //
        // NOTE: Java 21 does NOT allow combining 'null' and String constants in the
        // same case label (e.g., "case null, "MONTHLY"" is invalid). The null guard
        // is handled explicitly before the switch for clarity and compatibility.
        //
        // Why String → EmploymentType mapping here (not in EmployeeService)?
        // → Single Responsibility Principle: the service just calls getCalculator().
        //
        // Null → default to MONTHLY (sensible fallback for unspecified type)
        if (employmentTypeStr == null) {
            return calculators.get(EmploymentType.MONTHLY);
        }
        //
        // ── Java 21 switch expression on String ───────────────────────────────
        // Arrow syntax (->): each case is an expression, no break/fall-through
        // possible.
        // The compiler enforces a default case for String switches (non-sealed type).
        EmploymentType type = switch (employmentTypeStr) {
            case "MONTHLY" -> EmploymentType.MONTHLY;
            case "HOURLY" -> EmploymentType.HOURLY;
            case "DAILY" -> EmploymentType.DAILY;
            case "CONTRACT" -> EmploymentType.CONTRACT;
            case "COMMISSION" -> EmploymentType.COMMISSION;

            // ── Exhaustive guard clause ──────────────────────────────────────────
            // The 'default' case is required for String switches (non-exhaustive type).
            default -> throw new IllegalArgumentException(
                    "Unknown employment type: '%s'. Valid types: MONTHLY, HOURLY, DAILY, CONTRACT, COMMISSION"
                            .formatted(employmentTypeStr));
        };

        // Lookup in the pre-built Map for O(1) strategy resolution
        SalaryCalculator calculator = calculators.get(type);

        if (calculator == null) {
            // This indicates a programming error: a new EmploymentType enum value was added
            // but no corresponding @Component calculator was implemented.
            throw new IllegalStateException(
                    "No SalaryCalculator bean found for EmploymentType: %s. "
                            .formatted(type) +
                            "Ensure a @Component implementing SalaryCalculator exists for this type.");
        }

        return calculator;
    }

    /**
     * Resolves a calculator directly from the {@link EmploymentType} enum.
     *
     * <p>
     * Used internally when the type is already validated and parsed.
     * </p>
     *
     * @param type the employment type enum constant
     * @return the matching calculator strategy
     */
    public SalaryCalculator getCalculator(EmploymentType type) {
        // ── Java 21 Pattern Matching for switch on sealed/enum type ─────────────
        //
        // When switching on an ENUM, Java 21's exhaustiveness check is
        // compiler-enforced.
        // If a new EmploymentType value is added and not handled here:
        // → Compilation ERROR: "the switch expression does not cover all possible input
        // values"
        //
        // This is far safer than if-else chains where missing a case fails SILENTLY at
        // runtime.
        //
        return switch (type) {
            case MONTHLY -> calculators.get(EmploymentType.MONTHLY);
            case HOURLY -> calculators.get(EmploymentType.HOURLY);
            case DAILY -> calculators.get(EmploymentType.DAILY);
            case CONTRACT -> calculators.get(EmploymentType.MONTHLY); // CONTRACT uses monthly logic
            case COMMISSION -> calculators.get(EmploymentType.COMMISSION);
            // NOTE: No 'default' needed for exhaustive enum switch — compiler guarantees
            // coverage
        };
    }
}
