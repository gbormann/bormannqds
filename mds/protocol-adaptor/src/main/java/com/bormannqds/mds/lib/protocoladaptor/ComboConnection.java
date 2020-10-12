package com.bormannqds.mds.lib.protocoladaptor;

/**
 * Created by bormanng on 24/06/15.
 */
public class ComboConnection {
//public:
    public ComboConnection(final String quotesAddress, final String tradesAddress) {
        this.quotesAddress = quotesAddress;
        this.tradesAddress = tradesAddress;
    }

    public String getTradesAddress() {
        return tradesAddress;
    }

    public String getQuotesAddress() {
        return quotesAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComboConnection that = (ComboConnection) o;
        return (quotesAddress != null ? quotesAddress.equals(that.quotesAddress) : that.quotesAddress == null)
                ;// if QA are the same, so should TA be, so: && (tradesAddress != null ? tradesAddress.equals(that.tradesAddress) : that.tradesAddress == null);
    }

    @Override
    public int hashCode() {
        int result = quotesAddress != null ? quotesAddress.hashCode() : 0;
        // if QA are the same, so should TA be, so: result = 31 * result + (tradesAddress != null ? tradesAddress.hashCode() : 0);
        return result;
    }

    //private:
    private final String quotesAddress;
    private final String tradesAddress;
}
