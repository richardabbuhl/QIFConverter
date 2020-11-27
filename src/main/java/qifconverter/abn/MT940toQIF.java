package qifconverter.abn;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Title: GnuCashToQIF</p>
 * <p>Description: This program converts GnuCash files into multiple
 * QIF files</p>
 * <p>License: Public Domain</p>
 * @author Steven Lawrance
 * @version 1.0
 */
public class MT940toQIF extends DefaultHandler
{
    /**
     * Run the GnuCash to QIF Data Conversion program
     * @param args The arguments from the command line. This
     * version currently doesn't do anything with them
     */
    public static void main(String[] args) {
        MT940toQIF program = new MT940toQIF();
        File sourceFile = program.getSourceFile();
        if (sourceFile == null) {
            System.exit(1);
            return;
        }
        File destDir = program.getDestinationDirectory();
        if (destDir == null) {
            System.exit(1);
            return;
        }
        if (!program.readSourceFile(sourceFile)) {
            System.exit(2);
            return;
        }
        if (!program.writeDestinationFiles(destDir)) {
            JOptionPane.showMessageDialog(null, "One or more QIF files could not be written; consult the source code or ask for help", "GnuCash->QIF Data Conversion", JOptionPane.WARNING_MESSAGE);
            System.exit(3);
            return;
        }
        JOptionPane.showMessageDialog(null, "GnuCash to QIF Data Conversion Successful :-)!", "GnuCash->QIF Data Conversion", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    /**
     * Returns the destination directory for the QIF files
     * @return The File describing the target directory, or null
     * if the user did not select a directory or if the selected
     * directory does not have write permissions.
     */
    protected File getDestinationDirectory()
    {
        // Ask the user for the destination directory
        ensureFileChooserExists();
        chooser.resetChoosableFileFilters();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Destination Directory for QIF Files");
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        // If the directory does not exist, then create it
        File dir = chooser.getSelectedFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                JOptionPane.showMessageDialog(null, "Could not create target directory; check the parent directory's permissions", "Target QIF Directory", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        // Ensure that the target directory has file-create permissions
        if (!dir.canWrite()) {
            JOptionPane.showMessageDialog(null, "Selected directory does not have Create-File/Write permissions, exiting.", "Target QIF Directory", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Return the selected directory
        return dir;
    }

    /**
     * Returns the source File object as selected by the user.
     * This method ensures that the file can be read, returning
     * null if this process cannot read it after informing
     * the user.
     * @return The File that the user selected, or null if the
     * user did not select a file
     */
    protected File getSourceFile()
    {
        // Ask the user for the source file
        ensureFileChooserExists();
        chooser.setDialogTitle("Select Source GnuCash File");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        // Create a File and ensure that it can be read
        File file = chooser.getSelectedFile();
        if (!file.canRead()) {
            JOptionPane.showMessageDialog(null, "Selected file cannot be read; check its permissions", "Source GnuCash File", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Return the file
        return file;
    }

    /** The file chooser dialog box */
    protected JFileChooser chooser = null;

    /**
     * Creates the file chooser if it doesn't already exist
     */
    protected void ensureFileChooserExists()
    {
        // If it already exists, then don't do anything
        if (chooser != null)
            return;

        // Create a file chooser with our file extensions
        chooser = new JFileChooser();
        FileFilter gnucashFileType = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory() || f.getName().endsWith(".gnucash"))
                    return true;
                else
                    return false;
            }
            public String getDescription() {
                return "GnuCash XML File";
            }
        };

        // Add the file extensions with the default set to the compressed version
        chooser.addChoosableFileFilter(gnucashFileType);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(gnucashFileType);
    }

    /**
     * Writes out the QIF files to the given destination
     * directory using the accounts object
     * @param destDir The destination directory File object
     * @return True if all files were written; false if one or
     * more files failed
     */
    protected boolean writeDestinationFiles(File destDir)
    {
        // Go through every account, writing out the data for those with
        // at least one transaction
        Iterator accountIterator = accounts.values().iterator();
        String path = destDir.getPath();
        boolean failure = false;
        while (accountIterator.hasNext()) {
            currentAccount = (Account) accountIterator.next();
            if (currentAccount.trans.size() > 0)
            {
                try
                {
                    // Create this account's file
                    FileWriter writer = new FileWriter(path + File.separator + currentAccount.name + ".qif");

                    // Write out the QIF data
                    writeQIFData(writer, currentAccount);

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
    protected void writeQIFData(Writer writer, Account account) throws IOException
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
            currentTransaction = (Transaction) transIterator.next();
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
            writer.write("T" + ((Split) splits[acctSplit]).amount + "\n");
            if (((Split) splits[acctSplit]).reconciliationStatus == 'c')
                writer.write("CX\n");
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

    /**
     * Read the GnuCash XML file into the accounts object
     * @param source The source File object
     * @return True if the file was read; false if not
     */
    protected boolean readSourceFile(File source)
    {
        try
        {
            // Run the data conversion by having the XML parser provide
            // the file state events to us
            XMLReader parser = new org.apache.xerces.parsers.SAXParser();
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            FileReader fileReader = new FileReader(source);
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            parser.parse(new InputSource(fileReader));
            fileReader.close();
            return true;
        } catch (IOException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "File read error during XML parsing; check stderr", "GnuCash->QIF Data Conversion", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SAXException e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Corrupt GnuCash file: Invalid XML; check stderr", "GnuCash->QIF Data Conversion", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "General Exception " + e.toString() + "; check stderr", "GnuCash->QIF Data Conversion", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** The accounts, using their GUID as the key */
    protected HashMap accounts = new HashMap();

    /**
     * An Account class that contains transactions
     */
    protected class Account
    {
        /** Unknown account type */
        public final static int TYPE_UNKNOWN = -1;

        /** Double-entry type of account, which does not import into QIF */
        public final static int TYPE_DOUBLEENTRY = 0;

        /** BANK acount */
        public final static int TYPE_BANK = 1;

        /** MUTUAL account */
        public final static int TYPE_MUTUAL = 2;

        /** CREDIT card account */
        public final static int TYPE_CREDIT = 3;

        /** The type of account, as specified by the TYPE_* constants */
        public int type;

        /** The GUID of this account */
        public String guid;

        /** The name of this account */
        public String name;

        /** The Transaction objects for this Account */
        public List trans;

        /**
         * Creates a blank Account object
         */
        public Account() {
            type = -1;
            trans = new ArrayList();
        }

        /**
         * Sets the account type by its name
         * @param typeName The name of the type to set this account to
         */
        public void setAccount(String typeName)
        {
            // Handle our known types and assume that all others are
            // double-entry accounts
            if (typeName.equalsIgnoreCase("bank")) {
                type = TYPE_BANK;
                return;
            } else if (typeName.equalsIgnoreCase("credit")) {
                type = TYPE_CREDIT;
                return;
            } else if (typeName.equalsIgnoreCase("mutual")) {
                type = TYPE_MUTUAL;
                return;
            } else {
                type = TYPE_DOUBLEENTRY;
                return;
            }
        }
    }

    protected class Transaction
    {
        /** The posting date in YYYY-MM-DD format */
        public String datePosted;

        /** The description of the transaction */
        public String description;

        /** The reference number, if any */
        public String ref;

        /** The splits in this transaction (usually two) */
        public List splits;

        /**
         * Creates a new empty Transaction object
         */
        public Transaction() {
            splits = new ArrayList(2);
            ref = null;
        }
    }

    protected class Split
    {
        /** The amount of the split */
        public double amount;

        /** The reconciliation state of this split */
        public char reconciliationStatus;

        /** The account GUID */
        public String accountGuid;

        /**
         * Creates a new Split object
         */
        public Split() {
        }
    }

    /** This data member will handle the incoming XML values */
    protected StringBuffer buffer = new StringBuffer();

    /** Start document */
    public void startDocument()
    {
    }

    /** Start element */
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        // Reset the buffer
        buffer.setLength(0);

        // Dispatch the type of element
        if (qName.equalsIgnoreCase("gnc:account"))
            currentAccount = new Account();
        else if (qName.equalsIgnoreCase("gnc:transaction"))
            currentTransaction = new Transaction();
        else if (qName.equalsIgnoreCase("trn:date-posted"))
            parentName = qName;
        else if (qName.equalsIgnoreCase("trn:split"))
            currentSplit = new Split();
        else if (!qName.equalsIgnoreCase("ts:date"))
            parentName = null;
    }

    /** The current account */
    protected Account currentAccount = null;

    /** The current transaction */
    protected Transaction currentTransaction = null;

    /** The current split */
    protected Split currentSplit = null;

    /** The current parent element name */
    protected String parentName = null;

    /** End element */
    public void endElement(String uri, String localName, String qName)
    {
        // Trim the sides of the value
        String value = buffer.toString().trim();

        // Dispatch the type of element
        if (qName.equalsIgnoreCase("gnc:account"))
        {
            // Put this account into the list
            accounts.put(currentAccount.guid, currentAccount);
            currentAccount = null;
        } else if (qName.equalsIgnoreCase("act:name"))
            currentAccount.name = value;
        else if (qName.equalsIgnoreCase("act:id"))
            currentAccount.guid = value;
        else if (qName.equalsIgnoreCase("act:type"))
            currentAccount.setAccount(value);
        else if (qName.equalsIgnoreCase("ts:date") && parentName != null && parentName.equalsIgnoreCase("trn:date-posted"))
            currentTransaction.datePosted = value.substring(0, 10);
        else if (qName.equalsIgnoreCase("trn:description"))
            currentTransaction.description = value;
        else if (qName.equalsIgnoreCase("trn:num"))
            currentTransaction.ref = value;
        else if (qName.equalsIgnoreCase("split:reconciled-state"))
            currentSplit.reconciliationStatus = value.charAt(0);
        else if (qName.equalsIgnoreCase("split:quantity")) {
            int pos = value.indexOf("/");
            if (pos > 0)
                currentSplit.amount = Double.parseDouble(value.substring(0, pos)) / Double.parseDouble(value.substring(pos + 1, value.length()));
            else
                currentSplit.amount = Double.parseDouble(value);
        } else if (qName.equalsIgnoreCase("split:account"))
            currentSplit.accountGuid = value;
        else if (qName.equalsIgnoreCase("trn:split"))
        {
            // Look up the referenced account and add the current transaction
            // if found and if it's not a double-entry account
            Account acct = (Account) accounts.get(currentSplit.accountGuid);
            if (acct != null && acct.type != Account.TYPE_DOUBLEENTRY)
                acct.trans.add(currentTransaction);

            // Add the current split to the transaction
            currentTransaction.splits.add(currentSplit);
            currentSplit = null;
        } else if (qName.equalsIgnoreCase("gnc:transaction"))
            currentTransaction = null;
    }

    /** Characters */
    public void characters(char ch[], int start, int length)
    {
        buffer.append(ch, start, length);
    }
}