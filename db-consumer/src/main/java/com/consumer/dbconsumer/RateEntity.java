package com.consumer.dbconsumer;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "tbl_rates")
public class RateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String rateName;

    @Column(nullable = false)
    private Double bid;

    @Column(nullable = false)
    private Double ask;

    @Column(nullable = false)
    private Timestamp rateUpdatetime;

    @Column(nullable = false)
    private Timestamp dbUpdatetime;

    // --- GETTER & SETTER ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRateName() {
        return rateName;
    }

    public void setRateName(String rateName) {
        this.rateName = rateName;
    }

    public Double getBid() {
        return bid;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public Double getAsk() {
        return ask;
    }

    public void setAsk(Double ask) {
        this.ask = ask;
    }

    public Timestamp getRateUpdatetime() {
        return rateUpdatetime;
    }

    public void setRateUpdatetime(Timestamp rateUpdatetime) {
        this.rateUpdatetime = rateUpdatetime;
    }

    public Timestamp getDbUpdatetime() {
        return dbUpdatetime;
    }

    public void setDbUpdatetime(Timestamp dbUpdatetime) {
        this.dbUpdatetime = dbUpdatetime;
    }

    @Override
    public String toString() {
        return "RateEntity{" +
                "id=" + id +
                ", rateName='" + rateName + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", rateUpdatetime=" + rateUpdatetime +
                ", dbUpdatetime=" + dbUpdatetime +
                '}';
    }
}
