package ru.edu.pr03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class SelfCheck {

    private static int unitPass = 0;
    private static int unitFail = 0;
    private static int dataPass = 0;
    private static int dataFail = 0;
    private static int dataSkip = 0;

    public static void main(String[] args) {
        if (args.length > 0 && "--interactive".equals(args[0])) {
            runInteractive();
            return;
        }
        runUnitChecks();
        runDataDriven();

        System.out.println();
        System.out.println("================ ИТОГО ================");
        System.out.println("Unit-проверки: " + unitPass + "/" + (unitPass + unitFail)
                + (unitFail == 0 ? " PASS" : " FAIL"));
        System.out.println("Data-driven:   " + dataPass + "/" + (dataPass + dataFail)
                + (dataSkip > 0 ? " (пропущено: " + dataSkip + ")" : "")
                + (dataFail == 0 ? " PASS" : " FAIL"));
        System.out.println("=======================================");
    }

    private static void runUnitChecks() {
        ApprovalRouter r = new ApprovalRouter();

        check("01 personal > urgent", ApprovalRoute.SECURITY_REVIEW,
                r.route(10_000, true, RiskLevel.LOW, true));
        check("02 HIGH > urgent", ApprovalRoute.DIRECTOR_REVIEW,
                r.route(10_000, true, RiskLevel.HIGH, false));
        check("03 personal > HIGH", ApprovalRoute.SECURITY_REVIEW,
                r.route(10_000, false, RiskLevel.HIGH, true));
        check("04 amount > urgent", ApprovalRoute.FINANCE_REVIEW,
                r.route(600_000, true, RiskLevel.LOW, false));
        check("05 FAST_TRACK", ApprovalRoute.FAST_TRACK,
                r.route(10_000, true, RiskLevel.LOW, false));
        check("06 FINANCE_REVIEW (500000)", ApprovalRoute.FINANCE_REVIEW,
                r.route(500_000, false, RiskLevel.LOW, false));
        check("07 STANDARD", ApprovalRoute.STANDARD,
                r.route(10_000, false, RiskLevel.LOW, false));
        check("08 MEDIUM -> FINANCE_REVIEW", ApprovalRoute.FINANCE_REVIEW,
                r.route(10_000, false, RiskLevel.MEDIUM, false));
        check("09 SECURITY_REVIEW", ApprovalRoute.SECURITY_REVIEW,
                r.route(10_000, false, RiskLevel.LOW, true));
        check("10 499999 urgent FAST_TRACK", ApprovalRoute.FAST_TRACK,
                r.route(499_999, true, RiskLevel.LOW, false));
        check("11 500001 FINANCE_REVIEW", ApprovalRoute.FINANCE_REVIEW,
                r.route(500_001, false, RiskLevel.LOW, false));
        checkIAE("12 amount < 0 -> IAE",
                () -> r.route(-1, true, RiskLevel.LOW, false));
        checkIAE("13 risk == null -> IAE",
                () -> r.route(10_000, true, null, false));

        check("14 [A] 49999 -> STANDARD", ApprovalRoute.STANDARD,
                r.route(49_999, false, RiskLevel.LOW, false));
        check("15 [A] 50000 -> FINANCE_REVIEW", ApprovalRoute.FINANCE_REVIEW,
                r.route(50_000, false, RiskLevel.LOW, false));
        check("16 [A] urgent 10000 -> FAST_TRACK", ApprovalRoute.FAST_TRACK,
                r.route(10_000, true, RiskLevel.LOW, false));

        checkBool("17 [B] eligibility(10000,false,LOW,false)=true",
                true, r.addStandardEligibility(10_000, false, RiskLevel.LOW, false));
        checkBool("18 [B] eligibility(60000,false,LOW,false)=false (сумма)",
                false, r.addStandardEligibility(60_000, false, RiskLevel.LOW, false));
        checkBool("19 [B] eligibility(10000,true,LOW,false)=false (urgent)",
                false, r.addStandardEligibility(10_000, true, RiskLevel.LOW, false));

        check("20 HIGH + 600000 -> DIRECTOR", ApprovalRoute.DIRECTOR_REVIEW,
                r.route(600_000, false, RiskLevel.HIGH, false));
        check("21 amount=0 -> STANDARD", ApprovalRoute.STANDARD,
                r.route(0, false, RiskLevel.LOW, false));
        check("22 MEDIUM + urgent + малая -> FAST_TRACK", ApprovalRoute.FAST_TRACK,
                r.route(10_000, true, RiskLevel.MEDIUM, false));
        check("23 HIGH + personal + urgent -> SECURITY", ApprovalRoute.SECURITY_REVIEW,
                r.route(10_000, true, RiskLevel.HIGH, true));
        check("24 49999 MEDIUM -> FINANCE_REVIEW (R5 miss, R6)", ApprovalRoute.FINANCE_REVIEW,
                r.route(49_999, false, RiskLevel.MEDIUM, false));
        check("25 49999 personal -> SECURITY (R1 > R5)", ApprovalRoute.SECURITY_REVIEW,
                r.route(49_999, false, RiskLevel.LOW, true));
    }

    private static void runDataDriven() {
        Path path = Path.of("data/sample.csv");
        if (!Files.exists(path)) {
            System.out.println("\n[data] файл data/sample.csv не найден — пропуск");
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            System.out.println("\n[data] ошибка чтения: " + e.getMessage());
            return;
        }
        System.out.println("\n--- Data-driven: " + path + " ---");
        int rowNum = 0;
        for (String line : lines) {
            rowNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (rowNum == 1 || trimmed.startsWith("amount;")) continue;
            processDataLine(trimmed, rowNum);
        }
    }

    private static void processDataLine(String line, int rowNum) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            dataSkip++;
            System.out.println("SKIP data/" + rowNum + ": ожидалось 5 колонок, got " + parts.length);
            return;
        }
        long amount;
        boolean urgent;
        RiskLevel risk;
        boolean personal;
        try {
            amount = Long.parseLong(parts[0].trim());
            urgent = parseBool(parts[1].trim());
            risk = "null".equals(parts[2].trim()) ? null : RiskLevel.valueOf(parts[2].trim());
            personal = parseBool(parts[3].trim());
        } catch (RuntimeException e) {
            dataSkip++;
            System.out.println("SKIP data/" + rowNum + ": разбор — " + e.getMessage());
            return;
        }
        String expected = parts[4].trim();
        try {
            ApprovalRoute actual = new ApprovalRouter().route(amount, urgent, risk, personal);
            boolean ok = expected.equalsIgnoreCase(actual.name());
            if (ok) { dataPass++; System.out.println("PASS data/" + rowNum); }
            else    { dataFail++; System.out.println("FAIL data/" + rowNum
                        + ": ожидалось " + expected + ", получено " + actual); }
        } catch (IllegalArgumentException e) {
            if ("ERROR".equalsIgnoreCase(expected)) { dataPass++; System.out.println("PASS data/" + rowNum); }
            else { dataFail++; System.out.println("FAIL data/" + rowNum
                        + ": ожидалось " + expected + ", получено IAE: " + e.getMessage()); }
        }
    }

    private static boolean parseBool(String s) {
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        throw new IllegalArgumentException("boolean expected, got " + s);
    }

    private static void runInteractive() {
        System.out.println("Интерактивный режим. Формат: amount;urgent;risk;personal  (exit — выход)");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty() || "exit".equalsIgnoreCase(line)) break;
            String[] p = line.split(";", -1);
            if (p.length != 4) { System.out.println("Ошибка: нужно 4 значения"); continue; }
            try {
                long a = Long.parseLong(p[0].trim());
                boolean u = parseBool(p[1].trim());
                RiskLevel rk = "null".equals(p[2].trim()) ? null : RiskLevel.valueOf(p[2].trim());
                boolean pr = parseBool(p[3].trim());
                System.out.println("Маршрут: " + new ApprovalRouter().route(a, u, rk, pr));
            } catch (RuntimeException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void check(String name, ApprovalRoute expected, ApprovalRoute actual) {
        if (expected == actual) { unitPass++; System.out.println("PASS " + name); }
        else { unitFail++; System.out.println("FAIL " + name + ": ожидалось " + expected + ", получено " + actual); }
    }

    private static void checkBool(String name, boolean expected, boolean actual) {
        if (expected == actual) { unitPass++; System.out.println("PASS " + name); }
        else { unitFail++; System.out.println("FAIL " + name + ": ожидалось " + expected + ", получено " + actual); }
    }

    private static void checkIAE(String name, Runnable r) {
        try { r.run(); unitFail++; System.out.println("FAIL " + name + ": исключения не было"); }
        catch (IllegalArgumentException e) { unitPass++; System.out.println("PASS " + name); }
        catch (RuntimeException e) { unitFail++; System.out.println("FAIL " + name + ": " + e); }
    }
}
