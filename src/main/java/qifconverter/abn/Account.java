package qifconverter.abn;

import java.util.ArrayList;
import java.util.List;

/**
 * An Account class that contains transactions
 */
public class Account
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
    public List<Transaction> trans;

    /**
     * Creates a blank Account object
     */
    public Account() {
        type = -1;
        trans = new ArrayList<>();
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
        } else if (typeName.equalsIgnoreCase("credit")) {
            type = TYPE_CREDIT;
        } else if (typeName.equalsIgnoreCase("mutual")) {
            type = TYPE_MUTUAL;
        } else {
            type = TYPE_DOUBLEENTRY;
        }
    }
}
