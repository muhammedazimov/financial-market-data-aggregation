package com.simulator.pf2simulator.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/rates")
public class RateController {

    private static final Logger logger = LogManager.getLogger(RateController.class);

    private static final Map<String, Double[]> BASE_RATES = Map.ofEntries(
            Map.entry("USDTRY", new Double[]{40.5465, 40.5483}),
            Map.entry("EURTRY", new Double[]{47.3141, 47.3212}),
            Map.entry("GBPTRY", new Double[]{54.2783, 54.5503}),
            Map.entry("CADTRY", new Double[]{29.4543, 29.6020}),
            Map.entry("CHFTRY", new Double[]{50.4509, 50.7037}),
            Map.entry("SARTRY", new Double[]{10.7749, 10.8289}),
            Map.entry("JPYTRY", new Double[]{0.2729, 0.2743}),
            Map.entry("AUDTRY", new Double[]{26.3853, 26.5176}),
            Map.entry("NOKTRY", new Double[]{3.9718, 3.9917}),
            Map.entry("DKKTRY", new Double[]{6.3116, 6.3433}),
            Map.entry("SEKTRY", new Double[]{4.2226, 4.2438}),
            Map.entry("RUBTRY", new Double[]{0.5064, 0.5090})
    );

    @GetMapping("/{rateName}")
    public Map<String, Object> getRate(@PathVariable String rateName) {
        logger.info("[PF2Controller] Request received for rate={}", rateName);

        if (!rateName.startsWith("PF2_")) {
            logger.warn("[PF2Controller] Invalid rate request: {} (must start with PF2_)", rateName);
            return Map.of("error", "Rate name must start with PF2_");
        }

        String code = rateName.substring(4).toUpperCase();

        // 1. Direkt mevcut mu?
        Double[] base = BASE_RATES.get(code);
        if (base != null) {
            double bid = random(base[0] - 0.05, base[0] + 0.05);
            double ask = random(base[1] - 0.05, base[1] + 0.05);
            logger.debug("[PF2Controller] Direct {} bid={} ask={}", code, bid, ask);
            return buildResponse(rateName, bid, ask);
        }

        // 2. Ters mevcut mu?
        String reversed = reverseCode(code);
        Double[] reversedBase = BASE_RATES.get(reversed);
        if (reversedBase != null) {
            double reversedBid = 1.0 / reversedBase[1];
            double reversedAsk = 1.0 / reversedBase[0];
            logger.debug("[PF2Controller] Inverse {} via {} bid={} ask={}", code, reversed, reversedBid, reversedAsk);
            return buildResponse(rateName, reversedBid, reversedAsk);
        }

        // 3. Cross-rate
        String baseCurrency = code.substring(0, 3);
        String quoteCurrency = code.substring(3, 6);

        String baseToTry = baseCurrency + "TRY";
        String quoteToTry = quoteCurrency + "TRY";
        Double[] baseToTryRate = BASE_RATES.get(baseToTry);
        Double[] quoteToTryRate = BASE_RATES.get(quoteToTry);

        if (baseToTryRate != null && quoteToTryRate != null) {
            double baseBid = random(baseToTryRate[0] - 0.05, baseToTryRate[0] + 0.05);
            double baseAsk = random(baseToTryRate[1] - 0.05, baseToTryRate[1] + 0.05);
            double quoteBid = random(quoteToTryRate[0] - 0.05, quoteToTryRate[0] + 0.05);
            double quoteAsk = random(quoteToTryRate[1] - 0.05, quoteToTryRate[1] + 0.05);

            double crossBid = baseBid / quoteAsk;
            double crossAsk = baseAsk / quoteBid;
            logger.debug("[PF2Controller] Cross {} = bid={} ask={}", code, crossBid, crossAsk);
            return buildResponse(rateName, round(crossBid), round(crossAsk));
        }

        logger.error("[PF2Controller] Unsupported rate request: {}", code);
        return Map.of("error", "Unsupported rate: " + code);
    }

    private Map<String, Object> buildResponse(String name, double bid, double ask) {
        return Map.of(
                "rateName", name,
                "bid", round(bid),
                "ask", round(ask),
                "timestamp", LocalDateTime.now().toString()
        );
    }

    private String reverseCode(String code) {
        if (code.length() != 6) return "";
        return code.substring(3) + code.substring(0, 3);
    }

    private double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private double round(double val) {
        return Math.round(val * 100000.0) / 100000.0;
    }
}
