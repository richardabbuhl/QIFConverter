package qifconverter.abn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TabMain {

    private final static String USER_DIR = System.getProperty("user.dir");
    private final static String QUEUE_DIR = "queue";
    private final static String TAB_FILE_NAME = "TXT230602201456";

    public static void main(String[] args) throws IOException {
        TabMain tabMain = new TabMain();
        TabProcessor tabProcessor = new TabProcessor();
        List<TabRecord> data = tabProcessor.getTabDataFancy(USER_DIR + File.separator + QUEUE_DIR + File.separator + TAB_FILE_NAME + ".TAB");

        FileWriter fileWriter = new FileWriter(USER_DIR + File.separator + QUEUE_DIR + File.separator + TAB_FILE_NAME + ".TXT");
        try (fileWriter; PrintWriter printWriter = new PrintWriter(fileWriter)) {
            tabProcessor.printAllRecord(printWriter, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
