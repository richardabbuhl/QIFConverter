package qifconverter.abn;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.text.ParseException;
import java.util.List;

class TabProcessorTest {
    private String userDir = System.getProperty("user.dir");
    private String tabFileName = "TXT201126213554";
    private TabProcessor tabProcessor = new TabProcessor();

    @Test
    void getTabDataFixedWith() {
        List<String[]> data = tabProcessor.getTabDataFixedWidth(userDir + File.separator + tabFileName + ".TAB");
        for (String[] line : data) {
            for (String column : line) {
                System.out.print(column + "/");
            }
            System.out.println();
        }
    }

    @Test
    void getTabDataCSV() {
        List<String[]> data = tabProcessor.getTabDataCSV(userDir + File.separator + tabFileName + ".TAB");
        for (String[] line : data) {
            for (String column : line) {
                System.out.print(column + "/");
            }
            System.out.println();
        }
    }

    @Test
    void getTabDataFancy() {
        List<TabRecord> data = tabProcessor.getTabDataFancy(userDir + File.separator + tabFileName + ".TAB");
        for (TabRecord tabRecord : data) {
            System.out.println(tabRecord);
        }
    }

}