package qifconverter.abn;

public class Split
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
