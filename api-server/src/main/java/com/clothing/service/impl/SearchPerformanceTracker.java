package com.clothing.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SearchPerformanceTracker {

    private final int reportWindowSize;
    private final List<Long> latencySamplesMs = new ArrayList<>();

    public SearchPerformanceTracker(@Value("${app.search.metrics-window-size:200}") int reportWindowSize) {
        this.reportWindowSize = Math.max(20, reportWindowSize);
    }

    public synchronized void record(long latencyMs, String source, int resultSize) {
        latencySamplesMs.add(latencyMs);
        if (latencySamplesMs.size() < reportWindowSize) {
            return;
        }

        List<Long> sorted = new ArrayList<>(latencySamplesMs);
        Collections.sort(sorted);
        long p50 = percentile(sorted, 0.50);
        long p95 = percentile(sorted, 0.95);
        long p99 = percentile(sorted, 0.99);
        long max = sorted.get(sorted.size() - 1);

        log.info(
                "search-metrics window={} p50={}ms p95={}ms p99={}ms max={}ms lastSource={} lastResultSize={}",
                sorted.size(),
                p50,
                p95,
                p99,
                max,
                source,
                resultSize
        );
        latencySamplesMs.clear();
    }

    private long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        int safeIndex = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(safeIndex);
    }
}
