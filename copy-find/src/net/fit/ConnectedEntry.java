package net.fit;

public class ConnectedEntry {
    private String ipAddress;
    private int iterationCount;

    ConnectedEntry(String ipAddress, int iterationCount) {
        this.ipAddress = ipAddress;
        this.iterationCount = iterationCount;
    }

    int getIterationCount() {
        return iterationCount;
    }

    String getIpAddress() {
        return ipAddress;
    }

    void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    @Override
    public boolean equals(Object a) {
        if (!(a instanceof ConnectedEntry))
            return false;

        ConnectedEntry obj = (ConnectedEntry) a;
        return obj.getIpAddress().equals(this.getIpAddress());
    }
}
