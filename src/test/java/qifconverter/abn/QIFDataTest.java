package qifconverter.abn;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QIFDataTest {

    @Test
    void writeQIFData() throws IOException {
        String path = System.getProperty("user.dir");
        String currentAccount = "richard";
        FileWriter writer = new FileWriter(path + File.separator + currentAccount + ".txt");

        Account account = getAccount();
        QIFData qifData = new QIFData();
        qifData.writeQIFData(writer, account, new HashMap());
        writer.close();
    }

    private Account getAccount() {
        Account account = new Account();
        account.type = Account.TYPE_BANK;
        account.trans = getTransactions();
        return account;
    }

    private List<Transaction> getTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new Transaction();
        transaction.datePosted = "11/28/2020";
        transaction.description = "y at b";
        transaction.ref = "1234";
        transactions.add(transaction);
        return transactions;
    }
}