package com.simulator.mainapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.common.RateFields;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// UniversalRateCalculator: formulas.json dosyasındaki kurallara göre türev kurları hesaplar
public class UniversalRateCalculator {

    private static final Logger logger = LogManager.getLogger(UniversalRateCalculator.class);

    private static Map<String, Map<String, String>> formulas; // direct, inverse, cross formülleri

    // Sınıf yüklenirken formulas.json dosyası okunur
    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            formulas = mapper.readValue(
                    new File(
                            "E:\\Intellij\\exchange-project\\exchange-project\\main-app\\src\\main\\resources\\rate-formulas.json"),
                    new TypeReference<Map<String, Map<String, String>>>() {
                    });
            logger.info("[UniversalRateCalculator] Loaded formulas.json successfully with {} formula groups",
                    formulas.size());
        } catch (Exception e) {
            logger.error("[UniversalRateCalculator] Failed to load formulas.json", e);
            formulas = new HashMap<>();
        }
    }

    /**
     * Verilen ham kurlar üzerinden türev kurları hesapla
     *
     * @param rates ham veriler (ör: PF1_USDTRY -> RateFields, PF2_EURUSD ->
     *              RateFields)
     * @return hesaplanan türev kurlar (ör: USDTRY, EURTRY, GBPTRY)
     */
    public static Map<String, RateFields> calculate(Map<String, RateFields> rates) {
        Map<String, RateFields> result = new HashMap<>();

        // Tüm mevcut kurları dolaş
        for (String rateName : rates.keySet()) {
            String[] parts = stripPlatform(rateName).split("(?<=\\G.{3})"); // 3 harfli para birimlerine böl
            if (parts.length < 2)
                continue;

            String base = parts[0]; // örn: USD
            String quote = parts[1]; // örn: TRY

            // --- Direct case ---
            if (formulas.containsKey("direct")) {
                double bid = getValue(rates, formulas.get("direct").get("bid"), base, quote, null);
                double ask = getValue(rates, formulas.get("direct").get("ask"), base, quote, null);
                if (bid != -1 && ask != -1) {
                    result.put(base + quote, new RateFields(bid, ask, Instant.now().toString()));
                    logger.debug("[UniversalRateCalculator] Direct {}{} = bid={} ask={}", base, quote, bid, ask);
                }
            }

            // --- Inverse case ---
            if (formulas.containsKey("inverse")) {
                double bid = getValue(rates, formulas.get("inverse").get("bid"), base, quote, null);
                double ask = getValue(rates, formulas.get("inverse").get("ask"), base, quote, null);
                if (bid != -1 && ask != -1) {
                    result.put(quote + base, new RateFields(bid, ask, Instant.now().toString()));
                    logger.debug("[UniversalRateCalculator] Inverse {}{} = bid={} ask={}", quote, base, bid, ask);
                }
            }

            // --- Cross case ---
            if (formulas.containsKey("cross")) {
                String[] anchors = { "USD" }; // Çapraz kur için ara para birimi (şimdilik sadece USD)
                for (String anchor : anchors) {
                    double bid = getValue(rates, formulas.get("cross").get("bid"), base, quote, anchor);
                    double ask = getValue(rates, formulas.get("cross").get("ask"), base, quote, anchor);
                    if (bid != -1 && ask != -1) {
                        result.put(base + quote, new RateFields(bid, ask, Instant.now().toString()));
                        logger.debug("[UniversalRateCalculator] Cross {}{} via {} = bid={} ask={}", base, quote, anchor,
                                bid, ask);
                    }
                }
            }
        }

        // Hiç hesaplama yapılamadıysa uyarı logu
        if (result.isEmpty()) {
            logger.warn("[UniversalRateCalculator] No derived rates calculated. Input size={}", rates.size());
        }

        return result;
    }

    // Formülü doldurup değer döndüren yardımcı metod
    private static double getValue(Map<String, RateFields> rates, String expr,
            String base, String quote, String anchor) {
        try {
            // Formüldeki placeholder'ları gerçek değerlerle değiştir
            String filled = expr
                    .replace("{base}", base)
                    .replace("{quote}", quote)
                    .replace("{anchor}", anchor == null ? "" : anchor);

            // Eğer bid isteniyorsa
            if (filled.endsWith("_bid")) {
                String key = filled.replace("_bid", "");
                RateFields rf = findWithInverse(rates, key);
                return rf != null ? rf.getBid() : -1;
            }

            // Eğer ask isteniyorsa
            if (filled.endsWith("_ask")) {
                String key = filled.replace("_ask", "");
                RateFields rf = findWithInverse(rates, key);
                return rf != null ? rf.getAsk() : -1;
            }

            // Eğer formülde bölme varsa
            if (filled.contains("/")) {
                String[] parts = filled.split("/");
                double left = getValue(rates, parts[0].trim(), base, quote, anchor);
                double right = getValue(rates, parts[1].trim(), base, quote, anchor);
                return (left != -1 && right != -1) ? left / right : -1;
            }

            // Eğer formülde çarpma varsa
            if (filled.contains("*")) {
                String[] parts = filled.split("\\*");
                double left = getValue(rates, parts[0].trim(), base, quote, anchor);
                double right = getValue(rates, parts[1].trim(), base, quote, anchor);
                return (left != -1 && right != -1) ? left * right : -1;
            }

            return -1;
        } catch (Exception e) {
            logger.debug("[UniversalRateCalculator] Failed to evaluate expression={} for {}/{}", expr, base, quote);
            return -1;
        }
    }

    // Veriyi bulamazsa tersini kullanmayı deneyen metod
    private static RateFields findWithInverse(Map<String, RateFields> rates, String key) {
        RateFields rf = findAverage(rates, key);
        if (rf != null && rf.getBid() > 0 && rf.getAsk() > 0) {
            return rf;
        }

        // Yoksa tersini dene
        String base = key.substring(0, 3);
        String quote = key.substring(3);
        String inverseKey = quote + base;

        RateFields inv = findAverage(rates, inverseKey);
        if (inv != null && inv.getBid() > 0 && inv.getAsk() > 0) {
            double bid = 1.0 / inv.getAsk();
            double ask = 1.0 / inv.getBid();
            logger.debug("[UniversalRateCalculator] Inverse computed {} from {}", key, inverseKey);
            return new RateFields(bid, ask, Instant.now().toString());
        }

        return null;
    }

    // Rate isminden PF1_/PF2_ prefix'ini kaldırır
    private static String stripPlatform(String rateName) {
        if (rateName.startsWith("PF1_") || rateName.startsWith("PF2_")) {
            return rateName.substring(4);
        }
        return rateName;
    }

    // PF1 ve PF2 verileri varsa ortalamasını alır
    private static RateFields findAverage(Map<String, RateFields> rates, String key) {
        RateFields pf1 = rates.get("PF1_" + key);
        RateFields pf2 = rates.get("PF2_" + key);

        if (pf1 != null && pf2 != null) {
            double bid = (pf1.getBid() + pf2.getBid()) / 2.0;
            double ask = (pf1.getAsk() + pf2.getAsk()) / 2.0;
            logger.debug("[UniversalRateCalculator] Averaged {} from PF1+PF2", key);
            return new RateFields(bid, ask, Instant.now().toString());
        }

        if (pf1 != null)
            return pf1;
        if (pf2 != null)
            return pf2;

        return null;
    }
}
