package qifconverter.abn;

import java.util.ArrayList;
import java.util.List;

public class Transaction
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
