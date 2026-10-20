package ru.edu.pr03;

public class ApprovalRouter {

    /** Порог финансового контроля (R3, R4). */
    public static final long FINANCE_LIMIT = 500_000L;

    /** Порог «стандартной» заявки (R5, вариант 4). */
    public static final long STANDARD_LIMIT = 50_000L;

    public ApprovalRoute route(long amount, boolean urgent,
                               RiskLevel risk, boolean containsPersonalData) {
        checkContract(amount, risk);

        // R1: персональные данные — высший приоритет
        if (requiresSecurityReview(containsPersonalData)) return ApprovalRoute.SECURITY_REVIEW;
        // R2: высокий риск
        if (requiresDirectorReview(risk))                 return ApprovalRoute.DIRECTOR_REVIEW;
        // R3: сумма >= FINANCE_LIMIT
        if (requiresFinanceReview(amount))                return ApprovalRoute.FINANCE_REVIEW;
        // R4: срочная и сумма < FINANCE_LIMIT
        if (requiresFastTrack(amount, urgent))            return ApprovalRoute.FAST_TRACK;
        // R5 (вариант 4): строго «малая, несрочная, LOW, без personal»
        if (requiresStandard(amount, urgent, risk, containsPersonalData))
                                                          return ApprovalRoute.STANDARD;
        // R6 (вариант 4): страховка для нестандартных заявок
        return ApprovalRoute.FINANCE_REVIEW;
    }

    /** Часть Б варианта 4: предикат «заявка стандартная». */
    public boolean addStandardEligibility(long amount, boolean urgent,
                                          RiskLevel risk, boolean containsPersonalData) {
        checkContract(amount, risk);
        return amount < STANDARD_LIMIT
                && !urgent
                && risk == RiskLevel.LOW
                && !containsPersonalData;
    }

    private void checkContract(long amount, RiskLevel risk) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }

    private boolean requiresStandard(long amount, boolean urgent,
                                     RiskLevel risk, boolean containsPersonalData) {
        return addStandardEligibility(amount, urgent, risk, containsPersonalData);
    }
}
