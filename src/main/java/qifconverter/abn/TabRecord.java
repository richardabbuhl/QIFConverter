package qifconverter.abn;

import com.univocity.parsers.annotations.Parsed;
import lombok.Data;

@Data
public class TabRecord {

    @Parsed(index = 0)
    private String accountNumber;

    @Parsed(index = 1)
    private String currency;

    @Parsed(index = 2)
    private String date1;

    @Parsed(index = 3)
    private String beginAmount;

    @Parsed(index = 4)
    private String endAmount;

    @Parsed(index = 5)
    private String datePosted;

    @Parsed(index = 6)
    private String actualAmount;

    @Parsed(index = 7)
    private String description;
}
