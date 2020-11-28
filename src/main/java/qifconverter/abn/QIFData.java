package qifconverter.abn;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;

public class QIFData {

    /**
     * Writes out the QIF files to the given destination
     * directory using the accounts object
     * @param destDir The destination directory File object
     * @return True if all files were written; false if one or
     * more files failed
     */
    protected boolean writeDestinationFiles(File destDir, HashMap accounts)
    {
        // Go through every account, writing out the data for those with
        // at least one transaction
        Iterator accountIterator = accounts.values().iterator();
        String path = destDir.getPath();
        boolean failure = false;
        while (accountIterator.hasNext()) {
            Account currentAccount = (Account) accountIterator.next();
            if (currentAccount.trans.size() > 0)
            {
                try
                {
                    // Create this account's file
                    FileWriter writer = new FileWriter(path + File.separator + currentAccount.name + ".qif");

                    // Write out the QIF data
                    writeQIFData(writer, currentAccount, accounts);

                    // Close this file
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error while writing out the QIF file for \"" + currentAccount.name + "\"; check stderr", "Exporting QIF File", JOptionPane.WARNING_MESSAGE);
                    failure = true;
                }
            }
        }

        // Return whether or not all files were written successfully
        return !failure;
    }

    /**
     * Writes out the given Account and all of its transactions
     * to the given Writer object. Split transactions are
     * handled accordingly depending on whether or not their
     * account GUIDs exist in the accounts object.
     * @param writer The Writer to write QIF data to
     * @param account The Account to write to the Writer
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
    protected void writeQIFData(Writer writer, Account account, HashMap accounts) throws IOException
    {
        // Write out the QIF header
        String type;
        switch (account.type) {
            default :
            case Account.TYPE_BANK :
                type = "Bank";
                break;
            case Account.TYPE_CREDIT :
                type = "CCard";
                break;
            case Account.TYPE_MUTUAL :
                type = "Invst";
                break;
        }
        writer.write("!Type:" + type + "\n");

        // Write out every transaction
        Iterator transIterator = account.trans.iterator();
        Object splits[] = new Object[2];
        int i, acctSplit = 0;
        String payee, memo, amount;
        StringBuffer category = new StringBuffer();
        boolean splitTransaction;
        while (transIterator.hasNext()) {
            Transaction currentTransaction = (Transaction) transIterator.next();
            writer.write("D" + gnucashDateToQIFDate(currentTransaction.datePosted) + "\n");
            if (currentTransaction.ref != null)
                writer.write("N" + currentTransaction.ref + "\n");

            // The following code converted "my" way of using GnuCash into QIF.
            // YMMV, so please feel free to modify this code to suit your data
            // conversion needs :-)

            // Payee = text after "at" or "from", or desc if not exist
            // Memo = header of Payee if split as per above
            // Category = acct name

            // Split out the "at" or "from", if it's there
            int split = currentTransaction.description.indexOf(" at ");
            int descLength = currentTransaction.description.length();
            if (split > 0 && descLength > split + 4) {
                payee = currentTransaction.description.substring(split + 4);
                memo = currentTransaction.description.substring(0, split);
            } else if ((split = currentTransaction.description.indexOf(" from ")) > 0 && descLength > split + 6) {
                payee = currentTransaction.description.substring(split + 6);
                memo = currentTransaction.description.substring(0, split);
            } else if ((split = currentTransaction.description.indexOf(" via ")) > 0 && descLength > split + 5) {
                payee = currentTransaction.description.substring(split + 5);
                memo = currentTransaction.description.substring(0, split);
            } else {
                payee = currentTransaction.description;
                memo = "";
            }
            writer.write("P" + payee + "\n");
            writer.write("M" + memo + "\n");

            // Find out what the target account is through the splits
            splits = currentTransaction.splits.toArray(splits);
            if (currentTransaction.splits.size() > 2)
                splitTransaction = true;
            else
                splitTransaction = false;
            category.setLength(0);
            for (i = 0; i < splits.length && splits[i] != null; i++) {
                if (((Split) splits[i]).accountGuid.equalsIgnoreCase(account.guid)) {
                    acctSplit = i;
                } else {
                    if (splitTransaction) {
                        Account target = (Account) accounts.get(((Split) splits[i]).accountGuid);
                        if (target != null) {
                            if (target.type == Account.TYPE_DOUBLEENTRY)
                                category.append("S" + target.name + "\n");
                            else
                                category.append("S[" + target.name + "]\n");
                        } else
                            category.append("SUnknown\n");
                        category.append("$" + (0 - ((Split) splits[i]).amount) + "\n");
                    } else {
                        Account target = (Account) accounts.get(((Split) splits[i]).accountGuid);
                        if (target != null) {
                            if (target.type == Account.TYPE_DOUBLEENTRY)
                                category.append("L" + target.name + "\n");
                            else
                                category.append("L[" + target.name + "]\n");
                        } else
                            category.append("LUnknown\n");
                    }
                }
            }

            // Write out the amount, our cleared status, and the categories/splits
            if (splits[acctSplit] != null) {
                writer.write("T" + ((Split) splits[acctSplit]).amount + "\n");
                if (((Split) splits[acctSplit]).reconciliationStatus == 'c')
                    writer.write("CX\n");
            }
            writer.write(category.toString());

            // Write out the end of this transaction record
            writer.write("^\n");
        }
    }

    /**
     * Converts a GnuCash date into a QIF date
     * @param gnucashDate The GnuCash date to convert
     * @return The QIF-friendly date (4-digit year)
     */
    protected static String gnucashDateToQIFDate(String gnucashDate) {
        if (gnucashDate == null || gnucashDate.length() < 10)
            return "";
        return gnucashDate.substring(5, 7) + "/" + gnucashDate.substring(8, 10) + "/" + gnucashDate.substring(0, 4);
    }
}
