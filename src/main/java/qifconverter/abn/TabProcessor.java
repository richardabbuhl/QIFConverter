package qifconverter.abn;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.fixed.FixedWidthFields;
import com.univocity.parsers.fixed.FixedWidthParser;
import com.univocity.parsers.fixed.FixedWidthParserSettings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TabProcessor {

    private static final DateTimeFormatter QUICKEN_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final String S = "  ";

    public List<String[]> getTabDataFixedWidth(String path) {
        try (Reader inputReader = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) {
            FixedWidthFields fieldLengths = new FixedWidthFields(10, 4, 9, 8, 7, 9, 7, 8, 65, 33);
            FixedWidthParserSettings settings = new FixedWidthParserSettings(fieldLengths);
            settings.setSkipTrailingCharsUntilNewline(true);

            FixedWidthParser parser = new FixedWidthParser(settings);
            return parser.parseAll(inputReader);

        } catch (IOException e) {
            // handle exception
        }
        return null;
    }

    public List<String[]> getTabDataCSV(String path) {
        try (Reader inputReader = new InputStreamReader(new FileInputStream(new File(path)), StandardCharsets.UTF_8)) {
            CsvParserSettings settings = new CsvParserSettings();
            settings.setDelimiterDetectionEnabled(true);
            CsvParser parser = new CsvParser(settings);
            return parser.parseAll(inputReader);

        } catch (IOException e) {
            // handle exception
        }
        return null;
    }

    public List<TabRecord> getTabDataFancy(String path) {
        try (Reader inputReader = new InputStreamReader(new FileInputStream(new File(path)), StandardCharsets.UTF_8)) {
            BeanListProcessor<TabRecord> rowProcessor = new BeanListProcessor<>(TabRecord.class);
            CsvParserSettings settings = new CsvParserSettings();
            settings.setRowProcessor(rowProcessor);
            settings.setDelimiterDetectionEnabled(true);
            CsvParser parser = new CsvParser(settings);
            parser.parse(inputReader);
            return rowProcessor.getBeans();

        } catch (IOException e) {
            // handle exception
        }
        return null;
    }

    public void printAllRecord(PrintWriter printWriter, List<TabRecord> data) {
        printWriter.println("!Type:Bank");
        for (TabRecord tabRecord : data) {
            printRecord(printWriter, tabRecord);
        }
    }

    public void printRecord(PrintWriter printWriter, TabRecord tabRecord) {
        LocalDate datePosted = LocalDate.parse(tabRecord.getDate1(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        printWriter.println("D" + QUICKEN_FORMAT.format(datePosted));
        printWriter.println("T" + tabRecord.getActualAmount().replace(',', '.'));
        String fixedName = fixName(extractName(printWriter, tabRecord.getDescription()));
        printWriter.println("P" + fixedName);
        addCategory(printWriter, fixedName);
        printWriter.println("^");
    }

    private void addCategory(PrintWriter printWriter, String fixedName) {
        if (fixedName.contains("Sodexo")) {
            printWriter.println("LLunch Exp");
        }
    }

    private String fixName(String extractName) {
        if (extractName.contains("Sodexo")) {
            return "Sodexo";
        } else if (extractName.contains("NETFLIX")) {
            return "Netflix";
        }
        return extractName;
    }

    private String extractName(PrintWriter printWriter, String strLineText) {
        if (strLineText.contains("BEA")) {
            String substring = strLineText.substring(33);
            if (substring.contains(",")) {
                int endIndex = substring.contains(",") ? substring.indexOf(",") : substring.length();
                return substring.substring(0, endIndex);
            } else if (substring.contains(S)){
                int endIndex = substring.contains(S) ? substring.indexOf(S) : substring.length();
                return substring.substring(0, endIndex);
            }
            return substring;

        } else if (strLineText.startsWith("SEPA")) {
            int beginIndex = strLineText.indexOf("Naam:") + 5;
            String substring = strLineText.substring(beginIndex);
            int endIndex = substring.contains(S) ? beginIndex + substring.indexOf(S) : strLineText.length();
            return strLineText.substring(beginIndex, endIndex);

        } else if (strLineText.contains("GEA") || strLineText.contains("STORTING")) {
            return "Geldautomat";

        } else if (strLineText.contains("ACCOUNT-BALANCED")) {
            return "Account balanced";

        } else if (strLineText.contains("ABN AMRO Bank N.V.")) {
            return "EasyPayExtra";

        } else if (strLineText.contains("OUR REF")) {
            return "Transfer";

        } else if (strLineText.contains("CHARGES")) {
            return "Wire Transfer Charge";

        } else if (strLineText.startsWith("/TRTP")) {
            String[] split = strLineText.split("/");
            return split[8];
        }

        return "PROBLEMO" + strLineText;
    }

}
