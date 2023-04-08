package qifconverter.abn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TabMain {

    public String userDir = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
        TabMain tabMain = new TabMain();
        TabProcessor tabProcessor = new TabProcessor();
        String tabFileName = "TXT230327200841";
        List<TabRecord> data = tabProcessor.getTabDataFancy(tabMain.userDir + File.separator + tabFileName + ".TAB");

        FileWriter fileWriter = new FileWriter(tabMain.userDir + File.separator + tabFileName + ".TXT");
        try (fileWriter; PrintWriter printWriter = new PrintWriter(fileWriter)) {
            tabProcessor.printAllRecord(printWriter, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
