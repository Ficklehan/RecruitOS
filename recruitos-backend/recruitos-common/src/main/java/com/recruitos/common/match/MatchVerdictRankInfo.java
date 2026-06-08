package com.recruitos.common.match;

public class MatchVerdictRankInfo {

    private int rank;
    private int total;
    private int percentile;

    public MatchVerdictRankInfo() {
    }

    public MatchVerdictRankInfo(int rank, int total, int percentile) {
        this.rank = rank;
        this.total = total;
        this.percentile = percentile;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPercentile() {
        return percentile;
    }

    public void setPercentile(int percentile) {
        this.percentile = percentile;
    }
}
